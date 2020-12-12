package ru.otus.sc.query

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import org.slf4j.LoggerFactory
import ru.otus.sc.accounting.grpc.{EventReply, EventStreamService, EventStreamServiceClient, OffsetRequest}
import ru.otus.sc.common.model._
import ru.otus.sc.query.dao._
import ru.otus.sc.query.dao.impl.slick.{AccountDaoSlickImpl, ClientDaoSlickImpl, TransactionDaoSlickImpl}
import ru.otus.sc.query.db.Migrations
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn
import scala.util.{Failure, Success, Using}

object QueryMain {

  private val logger = LoggerFactory.getLogger(QueryMain.getClass)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("AccountingReadModel")
    implicit val ec     = system.dispatcher

    val clientSettings             = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8081).withTls(false)
    val client: EventStreamService = EventStreamServiceClient(clientSettings)

    def processEvents(
        clientDao: ClientDao,
        accountDao: AccountDao,
        tranDao: TransactionDao
    ): Unit = {
      val responseStream = client.readAccountingEventsStream(OffsetRequest(0))
      val done: Future[Done] =
        responseStream.runForeach(reply => {
          val events = reply.events
          logger.info(s"Got streaming reply: $events")
          val res: Future[_] = processEvent(clientDao, accountDao, tranDao, events) recover { ex =>
            logger.error(s"Error on process event $events", ex)
          }
          Await.ready(res, 5.second)
        })

      done.onComplete {
        case Success(_) =>
          println("streamingReply done")
        case Failure(e) =>
          println(s"Error streamingReply: $e")
      }
    }

    val configDbKey = "h2mem1"
    Using.resource(Database.forConfig(configDbKey)) { db =>
      import com.typesafe.config.ConfigFactory
      val appConfig = ConfigFactory.load
      new Migrations(appConfig.getConfig(configDbKey)).applyMigrationsSync()
      val clientDao  = new ClientDaoSlickImpl(db)
      val accountDao = new AccountDaoSlickImpl(db)
      val tranDao    = new TransactionDaoSlickImpl(db)

//      val binding = Http().newServerAt("localhost", 8080).bind(createRouter(db).route)
//
//      binding.foreach(b => println(s"Start listen on ${b.localAddress}"))
      processEvents(clientDao, accountDao, tranDao)
      StdIn.readLine()

//      binding.map(_.unbind()).onComplete(_ => system.terminate())
    }
  }

  private def processEvent(
      clientDao: ClientDao,
      accountDao: AccountDao,
      tranDao: TransactionDao,
      events: EventReply.Events
  )(implicit executionContext: ExecutionContext) = {
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
