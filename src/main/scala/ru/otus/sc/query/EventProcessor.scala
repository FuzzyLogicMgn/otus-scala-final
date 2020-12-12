package ru.otus.sc.query

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import ru.otus.sc.accounting.grpc.{EventReply, EventStreamService, OffsetRequest}
import ru.otus.sc.common.model._
import ru.otus.sc.query.dao.{AccountDao, ClientDao, OffsetDao, TransactionDao}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class EventProcessor(
    offsetDao: OffsetDao,
    clientDao: ClientDao,
    accountDao: AccountDao,
    tranDao: TransactionDao
)(implicit system: ActorSystem, executionContext: ExecutionContext) {

  private val offsetStoreKey      = "AccountingEventOffset"
  private val processEventTimeout = 5.second
  private val logger              = LoggerFactory.getLogger(classOf[EventProcessor])

  def processEvents(client: EventStreamService): Unit = {
    offsetDao.readOffset(offsetStoreKey) map {
      case Some(offset) =>
        logger.info(s"Start read events from offset $offset")
        offset
      case None =>
        logger.info(s"Offset with key= $offsetStoreKey not found, start from 0")
        0L
    } map { offset =>
      val responseStream = client.readAccountingEventsStream(OffsetRequest(offset))
      val done: Future[Done] =
        responseStream.runForeach(reply => {
          val events = reply.events
          logger.info(s"Got streaming reply: $events")
          val res: Future[_] = processEvent(events) map (_ =>
            offsetDao.storeOffset(offsetStoreKey, reply.offset)
          ) recover (ex => logger.error(s"Error on process event $events", ex))
          Await.ready(res, processEventTimeout)
        })

      done.onComplete {
        case Success(_) =>
          println("streamingReply done")
        case Failure(e) =>
          println(s"Error streamingReply: $e")
      }
    }
  }

  private def processEvent(events: EventReply.Events) = {
    if (events.isClientCreated) {
      val c = events.clientCreated.get
      clientDao.create(Client(Some(UUID.fromString(c.clientId)), c.name))
    } else if (events.isClientUpdated) {
      val c = events.clientUpdated.get
      clientDao.update(Client(Some(UUID.fromString(c.clientId)), c.name))
    } else if (events.isClientDeleted) {
      val c = events.clientDeleted.get
      clientDao.delete(UUID.fromString(c.clientId))
    } else if (events.isAccountCreated) {
      val acc = events.accountCreated.get
      accountDao.create(
        Account(
          Some(UUID.fromString(acc.accountId)),
          UUID.fromString(acc.clientId),
          organization = acc.organization
        )
      )
    } else if (events.isTransaction) {
      val acc       = events.transaction.get
      val accountId = UUID.fromString(acc.accountId)
      val tran = Transaction(
        None,
        accountId,
        Amount(acc.value, Currency.currencyFromSecId(acc.currency))
      )
      tranDao.create(tran).map(_ => accountDao.updateBalance(accountId, tran.amount))
    } else if (events.isAccountDeleted) {
      val acc = events.accountDeleted.get
      accountDao.delete(UUID.fromString(acc.accountId))
    } else {
      Future.successful()
    }
  }

}
