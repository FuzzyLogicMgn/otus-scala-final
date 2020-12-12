package ru.otus.sc.command.entity

import java.time.Instant
import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import ru.otus.sc.common.model.Client

import scala.concurrent.duration._

object ClientEntity {

  final case class State(name: String, createTime: Option[Instant], deleted: Boolean = false)
      extends CborSerializable {

    def isCreated: Boolean =
      createTime.isDefined

    def create(name: String, createTime: Instant): State =
      copy(name = name, createTime = Some(createTime))

    def update(client: Client): State = copy(name = client.name)

    def delete(): State = copy(deleted = true)

    def toClient(clientId: String): Client = Client(Some(UUID.fromString(clientId)), name)
  }
  object State {
    val empty = State(name = "", createTime = None)
  }

  sealed trait Command extends CborSerializable

  final case class Create(name: String, replyTo: ActorRef[StatusReply[Client]]) extends Command

  final case class Update(client: Client, replyTo: ActorRef[StatusReply[Client]]) extends Command

  final case class Read(replyTo: ActorRef[Option[Client]]) extends Command

  final case class Delete(replyTo: ActorRef[StatusReply[Client]]) extends Command

  sealed trait Event extends CborSerializable {
    def clientId: String
  }
  final case class ClientCreated(clientId: String, name: String, createTime: Instant) extends Event

  final case class ClientUpdated(clientId: String, client: Client) extends Event

  final case class ClientDeleted(clientId: String) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ClientEntity")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      ClientEntity(entityContext.entityId, Set("acc"))
    }.withRole("write-model"))
  }

  def apply(clientId: String, eventProcessorTags: Set[String]): Behavior[Command] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        PersistenceId(EntityKey.name, clientId),
        State.empty,
        (state, command) =>
          if (state.isCreated && !state.deleted) onClientExists(clientId, state, command)
          else onClientNotExists(clientId, state, command),
        (state, event) => handleEvent(state, event)
      )
      .withTagger(_ => eventProcessorTags)
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }

  private def onClientExists(
      clientId: String,
      state: State,
      command: Command
  ): ReplyEffect[Event, State] =
    command match {
      case Create(_, replyTo) =>
        Effect.reply(replyTo)(StatusReply.Error("Can't create, client already exists"))
      case Read(replyTo) =>
        Effect.reply(replyTo)(Some(state.toClient(clientId)))
      case Update(client, replyTo) =>
        Effect
          .persist(ClientUpdated(clientId, client))
          .thenReply(replyTo)(client => StatusReply.Success(client.toClient(clientId)))
      case Delete(replyTo) =>
        Effect
          .persist(ClientDeleted(clientId))
          .thenReply(replyTo)(deleteClient => StatusReply.Success(deleteClient.toClient(clientId)))
    }

  private def onClientNotExists(
      clientId: String,
      state: State,
      command: Command
  ): ReplyEffect[Event, State] =
    command match {
      case Create(name, replyTo) =>
        Effect
          .persist(ClientCreated(clientId, name, Instant.now()))
          .thenReply(replyTo)(createdClient =>
            StatusReply.Success(createdClient.toClient(clientId))
          )
      case Read(replyTo) =>
        Effect.reply(replyTo)(None)
      case Update(_, replyTo) =>
        Effect.reply(replyTo)(StatusReply.Error("Can't update, client not exists"))
      case Delete(replyTo) =>
        Effect.reply(replyTo)(StatusReply.Error("Can't delete, client not exists"))
    }

  private def handleEvent(state: State, event: Event) = {
    event match {
      case ClientCreated(_, name, createTime) => state.create(name, createTime)
      case ClientUpdated(_, client)           => state.update(client)
      case ClientDeleted(_)                   => state.delete()
    }
  }
}
