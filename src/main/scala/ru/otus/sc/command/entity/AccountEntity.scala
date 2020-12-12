package ru.otus.sc.command.entity

import java.time.Instant
import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import ru.otus.sc.command.model.AccountPostTransactionResponse
import ru.otus.sc.common.model
import ru.otus.sc.common.model.{Account, Amount}

import scala.concurrent.duration._

object AccountEntity {

  final case class State(
                          clientId: Option[UUID],
                          amount: Amount = model.Amount(0),
                          organization: String = "SBR",
                          createTime: Option[Instant],
                          deleted: Boolean = false
  ) extends CborSerializable {

    def isCreated: Boolean =
      createTime.isDefined

    def create(account: Account, createTime: Instant): State =
      copy(
        Some(account.clientId),
        account.amount,
        account.organization,
        createTime = Some(createTime)
      )

    def updateBalance(tranAmount: Amount): State =
      copy(amount = amount.copy(value = amount.value + tranAmount.value))

    def delete(): State = copy(deleted = true)

    def toAccount(accId: String): Account =
      model.Account(Some(UUID.fromString(accId)), clientId.get, amount, organization)
  }
  private object State {
    val empty: State = State(clientId = None, createTime = None)
  }

  sealed trait Command                                                               extends CborSerializable
  final case class Create(account: Account, replyTo: ActorRef[StatusReply[Account]]) extends Command
  final case class Read(replyTo: ActorRef[Option[Account]])                          extends Command
  final case class UpdateBalance(diff: Amount, replyTo: ActorRef[AccountPostTransactionResponse])
      extends Command
  final case class Delete(replyTo: ActorRef[StatusReply[Account]]) extends Command

  sealed trait Event extends CborSerializable {
    def accountId: String
  }

  final case class AccountCreated(accountId: String, account: Account, createTime: Instant)
      extends Event
  final case class Transaction(accountId: String, tranAmount: Amount, createTime: Instant)
      extends Event
  final case class AccountDeleted(accountId: String) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AccountEntity")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      AccountEntity(entityContext.entityId, Set("acc"))
    }.withRole("write-model"))
  }

  def apply(accountId: String, eventProcessorTags: Set[String]): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        PersistenceId(EntityKey.name, accountId),
        State.empty,
        (state, command) =>
          if (state.isCreated && !state.deleted) onAccountExists(accountId, state, command)
          else onAccountNotExists(accountId, state, command),
        (state, event) => handleEvent(state, event)
      )
      .withTagger(_ => eventProcessorTags)
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }

  private def onAccountExists(
      accountId: String,
      state: State,
      command: Command
  ): ReplyEffect[Event, State] =
    command match {
      case cmd: Create =>
        Effect.reply(cmd.replyTo)(StatusReply.Error("Can't create, account already exists"))
      case Read(replyTo) =>
        Effect.reply(replyTo)(Some(state.toAccount(accountId)))
      case UpdateBalance(tranAmount: Amount, replyTo) =>
        if (tranAmount.value < 0 && (state.amount.value < tranAmount.value.abs)) {
          Effect.reply(replyTo)(AccountPostTransactionResponse.RejectNotEnoughFunds(UUID.fromString(accountId), state.amount))
        } else {
          Effect
            .persist(Transaction(accountId, tranAmount, Instant.now()))
            .thenReply(replyTo)(updatedAccount =>
              AccountPostTransactionResponse.Success(updatedAccount.toAccount(accountId))
            )
        }
      case Delete(replyTo) =>
        Effect
          .persist(AccountDeleted(accountId))
          .thenReply(replyTo)(deletedAccount =>
            StatusReply.Success(deletedAccount.toAccount(accountId))
          )
    }

  private def onAccountNotExists(
      accountId: String,
      state: State,
      command: Command
  ): ReplyEffect[Event, State] =
    command match {
      case Create(account, replyTo) =>
        Effect
          .persist(AccountCreated(accountId, account, Instant.now()))
          .thenReply(replyTo)(createdAccount =>
            StatusReply.Success(createdAccount.toAccount(accountId))
          )
      case Read(replyTo) =>
        Effect.reply(replyTo)(None)
      case UpdateBalance(_, replyTo) =>
        Effect.reply(replyTo)(AccountPostTransactionResponse.RejectNotFoundAccount(UUID.fromString(accountId)))
      case Delete(replyTo) => Effect.reply(replyTo)(StatusReply.Error("Account not found"))
    }

  private def handleEvent(state: State, event: Event) = {
    event match {
      case AccountCreated(_, account, createTime) => state.create(account, createTime)
      case Transaction(_, tranAmount, _)          => state.updateBalance(tranAmount)
      case AccountDeleted(_)                      => state.delete()
    }
  }
}
