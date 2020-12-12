package ru.otus.sc.command.model

import java.util.UUID

import ru.otus.sc.common.model.{Account, Amount, Transaction}

case class AccountCreateRequest(account: Account)
case class AccountCreateResponse(account: Account)

case class AccountReadRequest(id: UUID)
sealed trait AccountReadResponse
object AccountReadResponse {
  case class Success(account: Account) extends AccountReadResponse
  case class NotFound(id: UUID)        extends AccountReadResponse
}

case class AccountUpdateRequest(account: Account)
sealed trait AccountUpdateResponse
object AccountUpdateResponse {
  case class Success(account: Account) extends AccountUpdateResponse
  case class NotFound(id: UUID)        extends AccountUpdateResponse
  object AccountWithoutId              extends AccountUpdateResponse
}

case class AccountDeleteRequest(id: UUID)
sealed trait AccountDeleteResponse
object AccountDeleteResponse {
  case class Success(Account: Account) extends AccountDeleteResponse
  case class NotFound(id: UUID)        extends AccountDeleteResponse
}

case class AccountPostTransactionRequest(tran: Transaction)
sealed trait AccountPostTransactionResponse
object AccountPostTransactionResponse {
  case class Success(account: Account)      extends AccountPostTransactionResponse
  case class RejectNotFoundAccount(accountId: UUID) extends AccountPostTransactionResponse
  case class RejectNotFoundRate(secId: String)      extends AccountPostTransactionResponse
  case class RejectNotEnoughFunds(accountId: UUID, balance: Amount)
      extends AccountPostTransactionResponse
}