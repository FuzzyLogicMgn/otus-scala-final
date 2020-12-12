package ru.otus.sc.command.service.impl

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery, Sequence}
import akka.stream.scaladsl.Source
import ru.otus.sc.accounting.grpc
import ru.otus.sc.accounting.grpc._
import ru.otus.sc.command.entity.{AccountEntity, ClientEntity}

class EventStreamServiceImpl(implicit system: ActorSystem[_]) extends EventStreamService {

  override def readAccountingEventsStream(in: OffsetRequest): Source[EventReply, NotUsed] = {
    println(s"Start read accounting events from offset ${in.offset}")
    val queries =
      PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
    val src: Source[EventEnvelope, NotUsed] =
      queries.eventsByTag(tag = "acc", offset = Sequence(in.offset))
    src.map(eventEnvelope =>
      EventReply(eventEnvelope.event match {
        case ClientEntity.ClientCreated(clientId, name, _) =>
          EventReply.Events.ClientCreated(grpc.ClientCreated(clientId, name))
        case ClientEntity.ClientUpdated(clientId, client) =>
          EventReply.Events.ClientUpdated(ClientUpdated(clientId, client.name))
        case ClientEntity.ClientDeleted(clientId) =>
          EventReply.Events.ClientDeleted(ClientDeleted(clientId))
        case AccountEntity.AccountCreated(accountId, account, _) =>
          EventReply.Events.AccountCreated(
            grpc.AccountCreated(
              accountId,
              account.clientId.toString,
              account.amount.value,
              account.amount.currency.secid,
              account.organization
            )
          )
        case AccountEntity.Transaction(accountId, amount, _) =>
          EventReply.Events.Transaction(
            grpc.Transaction(accountId, amount.value, amount.currency.secid)
          )
        case AccountEntity.AccountDeleted(accountId) =>
          EventReply.Events.AccountDeleted(grpc.AccountDeleted(accountId))
      })
    )
  }

}
