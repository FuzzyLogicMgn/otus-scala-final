package ru.otus.sc.query.model

import java.util.UUID

import ru.otus.sc.common.model.Account

sealed trait AccountFindRequest
object AccountFindRequest {
  case class ByClient(clientId: UUID) extends AccountFindRequest
  case class ByClientAndFilter(clientId: UUID, filter: Account => Boolean)
      extends AccountFindRequest
}
sealed trait AccountFindResponse
object AccountFindResponse {
  case class Success(accounts: Seq[Account]) extends AccountFindResponse
  case class NotFound(id: UUID)              extends AccountFindResponse
}
