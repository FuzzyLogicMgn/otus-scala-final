package ru.otus.sc.command.service.impl

import java.time.Duration

import akka.actor.typed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import ru.otus.sc.command.entity.{AccountEntity, ExchangeServiceEntity}
import ru.otus.sc.command.model._
import ru.otus.sc.command.service.AccountService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AccountServiceImpl(implicit ec: ExecutionContext, system: typed.ActorSystem[_])
    extends AccountService {
  private val sharding                = ClusterSharding(system)
  private val EXCHANGE_SERVICE_DUPLICATE_FACTOR = 2
  implicit private val timeout: Timeout =
    Timeout.create(Duration.ofSeconds(5))

  override def create(accountCreateRequest: AccountCreateRequest): Future[AccountCreateResponse] = {
    val entityRef =
      sharding.entityRefFor(AccountEntity.EntityKey, accountCreateRequest.account.id.get.toString)
    entityRef
      .ask(AccountEntity.Create(accountCreateRequest.account, _))
      .map(rs => AccountCreateResponse(rs.getValue))
  }

  override def read(accountReadRequest: AccountReadRequest): Future[AccountReadResponse] = {
    val entityRef = sharding.entityRefFor(AccountEntity.EntityKey, accountReadRequest.id.toString)
    entityRef.ask(AccountEntity.Read(_)).map {
      case Some(value) => AccountReadResponse.Success(value)
      case None        => AccountReadResponse.NotFound(accountReadRequest.id)
    }
  }

  override def postTransaction(
      request: AccountPostTransactionRequest
  ): Future[AccountPostTransactionResponse] = {
    val accRef =
      sharding.entityRefFor(AccountEntity.EntityKey, request.tran.accountId.toString)
    accRef.ask(AccountEntity.Read(_)).flatMap {
      case Some(account) => {
        val exchangeService = sharding.entityRefFor(
          ExchangeServiceEntity.EntityKey,
          Random.nextInt(EXCHANGE_SERVICE_DUPLICATE_FACTOR).toString
        )
        exchangeService
          .ask(ExchangeServiceEntity.ConvertAmount(request.tran.amount, account.amount.currency, _))
          .flatMap { amountInAccountCurrency =>
            accRef.ask(AccountEntity.UpdateBalance(amountInAccountCurrency, _))
          }
      }
      case None =>
        Future.successful(
          AccountPostTransactionResponse.RejectNotFoundAccount(request.tran.accountId)
        )
    }
  }

  override def delete(accountDeleteRequest: AccountDeleteRequest): Future[AccountDeleteResponse] = {
    val entityRef = sharding.entityRefFor(AccountEntity.EntityKey, accountDeleteRequest.id.toString)
    entityRef
      .ask(AccountEntity.Delete(_))
      .map(rs =>
        if (rs.isSuccess) {
          AccountDeleteResponse.Success(rs.getValue)
        } else {
          AccountDeleteResponse.NotFound(accountDeleteRequest.id)
        }
      )
  }
}
