package ru.otus.sc.command.model

import java.util.UUID

import ru.otus.sc.common.model.Client

case class ClientCreateRequest(client: Client)
case class ClientCreateResponse(client: Client)

case class ClientUpdateRequest(client: Client)
sealed trait ClientUpdateResponse
object ClientUpdateResponse {
  case class Success(client: Client) extends ClientUpdateResponse
  case class NotFound(id: UUID) extends ClientUpdateResponse
  object ClientWithoutId extends ClientUpdateResponse
}

case class ClientReadRequest(id: UUID)
sealed trait ClientReadResponse
object ClientReadResponse {
  case class Success(client: Client) extends ClientReadResponse
  case class NotFound(id: UUID)      extends ClientReadResponse
}

case class ClientDeleteRequest(id: UUID)
sealed trait ClientDeleteResponse
object ClientDeleteResponse {
  case class Success(client: Client) extends ClientDeleteResponse
  case class NotFound(id: UUID)      extends ClientDeleteResponse
}
