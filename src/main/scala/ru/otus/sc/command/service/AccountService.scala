package ru.otus.sc.command.service

import ru.otus.sc.command.model._
import ru.otus.sc.common.service.AppService

import scala.concurrent.Future

/**
  * Сервис для операций со счетами.
  * Помимо CRUD операций поддержана возможность производить транзакции по счёту
  */
trait AccountService extends AppService {
  def create(accountCreateRequest: AccountCreateRequest): Future[AccountCreateResponse]
  def delete(accountDeleteRequest: AccountDeleteRequest): Future[AccountDeleteResponse]
  def read(accountReadRequest: AccountReadRequest): Future[AccountReadResponse]
  def postTransaction(
      accountPostTransactionRequest: AccountPostTransactionRequest
  ): Future[AccountPostTransactionResponse]

  override def getServiceName: String = "AccountService"
}
