package ru.otus.sc.query.model

import java.util.UUID

import ru.otus.sc.common.model.Transaction

sealed trait AccountFindTransactionRequest
object AccountFindTransactionRequest {
  case class ByAccount(accountId: UUID) extends AccountFindTransactionRequest
  case class ByAccountAndFilter(accountId: UUID, filter: Transaction => Boolean)
      extends AccountFindTransactionRequest
}
sealed trait AccountFindTransactionResponse
object AccountFindTransactionResponse {
  case class Success(transactions: Seq[Transaction]) extends AccountFindTransactionResponse
  case class NotFound(id: UUID)                      extends AccountFindTransactionResponse
}
