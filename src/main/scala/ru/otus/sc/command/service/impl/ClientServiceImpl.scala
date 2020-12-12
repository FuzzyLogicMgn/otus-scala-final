package ru.otus.sc.command.service.impl

import java.time.Duration
import java.util.UUID

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import ru.otus.sc.command.entity.ClientEntity
import ru.otus.sc.command.model._
import ru.otus.sc.command.service.ClientService

import scala.concurrent.{ExecutionContext, Future}

class ClientServiceImpl(implicit ec: ExecutionContext, system: ActorSystem[_])
    extends ClientService {
  //TODO: вынести в общий интерфейс
  private val sharding = ClusterSharding(system)
  implicit private val timeout: Timeout =
    Timeout.create(Duration.ofSeconds(5))

  override def create(clientCreateRequest: ClientCreateRequest): Future[ClientCreateResponse] = {
      val entityRef =
        sharding.entityRefFor(ClientEntity.EntityKey, UUID.randomUUID().toString)
      entityRef
        .ask(ClientEntity.Create(clientCreateRequest.client.name, _))
        .map(rs => ClientCreateResponse(rs.getValue))
  }

  override def read(clientReadRequest: ClientReadRequest): Future[ClientReadResponse] = {
    val entityRef = sharding.entityRefFor(ClientEntity.EntityKey, clientReadRequest.id.toString)
    entityRef.ask(ClientEntity.Read(_)).map {
      case Some(value) => ClientReadResponse.Success(value)
      case None        => ClientReadResponse.NotFound(clientReadRequest.id)
    }
  }

  override def update(clientUpdateRequest: ClientUpdateRequest): Future[ClientUpdateResponse] = {
    clientUpdateRequest.client.id match {
      case Some(clientId) =>
        val client = sharding.entityRefFor(ClientEntity.EntityKey, clientId.toString)
        client
          .ask(ClientEntity.Update(clientUpdateRequest.client, _))
          .map(rs =>
            if (rs.isSuccess) {
              ClientUpdateResponse.Success(rs.getValue)
            } else {
              ClientUpdateResponse.NotFound(clientId)
            }
          )
      case None => Future.successful(ClientUpdateResponse.ClientWithoutId)
    }
  }

  override def delete(clientDeleteRequest: ClientDeleteRequest): Future[ClientDeleteResponse] = {
    val client = sharding.entityRefFor(ClientEntity.EntityKey, clientDeleteRequest.id.toString)
    client
      .ask(ClientEntity.Delete(_))
      .map(rs =>
        if (rs.isSuccess) {
          ClientDeleteResponse.Success(rs.getValue)
        } else {
          ClientDeleteResponse.NotFound(clientDeleteRequest.id)
        }
      )
  }
}
