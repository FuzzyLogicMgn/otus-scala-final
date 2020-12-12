package ru.otus.sc.query.service

import ru.otus.sc.common.service.AppService
import ru.otus.sc.query.model._

import scala.concurrent.Future

trait AccountService extends AppService {
  def read(accountReadRequest: AccountReadRequest): Future[AccountReadResponse]
  def find(accountFindRequest: AccountFindRequest): Future[AccountFindResponse]
  def findTransactions(
      accountFindTransactionRequest: AccountFindTransactionRequest
  ): Future[AccountFindTransactionResponse]

  override def getServiceName: String = "AccountService"
}
