package ru.otus.sc.command.route

import java.util.UUID

import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import ru.otus.sc.command.model._
import ru.otus.sc.command.service.ClientService
import ru.otus.sc.common.json.AppTapir._
import ru.otus.sc.common.json.EntityJsonProtocol._
import ru.otus.sc.common.model.Client
import ru.otus.sc.common.route.BaseRouter

import scala.concurrent.{ExecutionContext, Future}

class ClientRouter(clientService: ClientService)(implicit ec: ExecutionContext) extends BaseRouter {
  override def route: Route =
    concat(createClientRoute, readClientRoute, updateClientRoute, deleteClientRoute)

  private val createClientEndpoint: Endpoint[Client, String, Client, Any] = baseEndpoint
    .in("client")
    .post
    .in(anyJsonBody[Client])
    .description("JSON with client description")
    .errorOut(stringBody)
    .out(anyJsonBody[Client])
    .description("Returns created client")

  private val getClientEndpoint: Endpoint[UUID, String, Client, Any] = baseEndpoint
    .in("client")
    .get
    .in(path[UUID]("clientId"))
    .description("Client identifier (UUID)")
    .errorOut(stringBody)
    .out(anyJsonBody[Client])
    .description("Returns client")

  private val updateClientEndpoint: Endpoint[Client, String, Client, Any] = baseEndpoint
    .in("client")
    .put
    .in(anyJsonBody[Client])
    .description("Client data")
    .errorOut(stringBody)
    .out(anyJsonBody[Client])
    .description("Updated client")

  private val deleteClientEndpoint: Endpoint[UUID, String, Client, Any] = baseEndpoint
    .in("client")
    .delete
    .in(path[UUID]("clientId"))
    .description("Client identifier (UUID)")
    .errorOut(stringBody)
    .out(anyJsonBody[Client])
    .description("Deleted client")

  private def createClient(client: Client): Future[Either[String, Client]] = {
    clientService.create(ClientCreateRequest(client)) map { response =>
      Right(response.client)
    }
  }

  private def getClient(clientId: UUID): Future[Either[String, Client]] = {
    clientService.read(ClientReadRequest(clientId)) map {
      case ClientReadResponse.Success(client) => Right(client)
      case ClientReadResponse.NotFound(_)     => Left(s"Client with ID $clientId not found")
    }
  }

  private def updateClient(client: Client): Future[Either[String, Client]] = {
    clientService.update(ClientUpdateRequest(client)) map {
      case ClientUpdateResponse.Success(client)    => Right(client)
      case ClientUpdateResponse.NotFound(clientId) => Left(s"Client with ID $clientId not found")
      case ClientUpdateResponse.ClientWithoutId    => Left("Client ID was not provided")
    }
  }

  private def deleteClient(clientId: UUID): Future[Either[String, Client]] = {
    clientService.delete(ClientDeleteRequest(clientId)) map {
      case ClientDeleteResponse.Success(client)    => Right(client)
      case ClientDeleteResponse.NotFound(clientId) => Left(s"Client with ID $clientId not found")
    }
  }

  val createClientRoute: Route = createClientEndpoint.toRoute(createClient)
  val readClientRoute: Route   = getClientEndpoint.toRoute(getClient)
  val updateClientRoute: Route = updateClientEndpoint.toRoute(updateClient)
  val deleteClientRoute: Route = deleteClientEndpoint.toRoute(deleteClient)

  def getEndpoints: Seq[Endpoint[_, _, _, _]] =
    Seq(createClientEndpoint, getClientEndpoint, updateClientEndpoint, deleteClientEndpoint)
}
