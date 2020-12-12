package ru.otus.sc.query.service.impl

import java.util.UUID

import ru.otus.sc.common.model.{Account, Transaction}
import ru.otus.sc.query.dao.{AccountDao, TransactionDao}
import ru.otus.sc.query.model._
import ru.otus.sc.query.service.AccountService

import scala.concurrent.{ExecutionContext, Future}

class AccountServiceImpl(
    accountDao: AccountDao,
    transactionDao: TransactionDao
)(implicit ec: ExecutionContext)
    extends AccountService {

  override def read(accountReadRequest: AccountReadRequest): Future[AccountReadResponse] =
    accountDao.read(accountReadRequest.id) map {
      case Some(value) => AccountReadResponse.Success(value)
      case None        => AccountReadResponse.NotFound(accountReadRequest.id)
    }

  override def find(accountFindRequest: AccountFindRequest): Future[AccountFindResponse] = {
    def find(
        accounts: Seq[Account],
        clientId: UUID,
        filterFcn: Option[Account => Boolean]
    ): AccountFindResponse = {
      val clientAccounts = accounts.filter(acc => acc.clientId == clientId)
      filterFcn.map(fcn => clientAccounts.filter(fcn)).getOrElse(clientAccounts) match {
        case Seq() => AccountFindResponse.NotFound(clientId)
        case items => AccountFindResponse.Success(items)
      }
    }

    accountDao.findAll() map { accounts =>
      accountFindRequest match {
        case AccountFindRequest.ByClient(clientId) => find(accounts, clientId, None)
        case AccountFindRequest.ByClientAndFilter(clientId, filter) =>
          find(accounts, clientId, Some(filter))
      }
    }
  }

  override def findTransactions(
      accountFindTransactionRequest: AccountFindTransactionRequest
  ): Future[AccountFindTransactionResponse] = {
    def find(
        accounts: Seq[Transaction],
        accountId: UUID,
        filterFcn: Option[Transaction => Boolean]
    ): AccountFindTransactionResponse = {
      val clientAccounts = accounts.filter(tr => tr.accountId == accountId)
      filterFcn.map(fcn => clientAccounts.filter(fcn)).getOrElse(clientAccounts) match {
        case Seq() => AccountFindTransactionResponse.NotFound(accountId)
        case items => AccountFindTransactionResponse.Success(items)
      }
    }

    transactionDao.findAll() map { transactions =>
      accountFindTransactionRequest match {
        case AccountFindTransactionRequest.ByAccount(accountId) =>
          find(transactions, accountId, None)
        case AccountFindTransactionRequest.ByAccountAndFilter(accountId, filter) =>
          find(transactions, accountId, Some(filter))
      }
    }
  }
}
