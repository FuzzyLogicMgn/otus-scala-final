package ru.otus.sc.query.model

import java.util.UUID

import ru.otus.sc.common.model.Account

case class AccountReadRequest(id: UUID)
sealed trait AccountReadResponse
object AccountReadResponse {
  case class Success(account: Account) extends AccountReadResponse
  case class NotFound(id: UUID)        extends AccountReadResponse
}