package ru.otus.sc.query.route

import java.util.UUID

import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import ru.otus.sc.common.json.AppTapir
import ru.otus.sc.common.json.AppTapir.{Endpoint, anyJsonBody, stringBody, _}
import ru.otus.sc.common.json.EntityJsonProtocol._
import ru.otus.sc.common.model.Account
import ru.otus.sc.common.route.BaseRouter
import ru.otus.sc.query.model.{AccountFindRequest, AccountFindResponse, AccountReadRequest, AccountReadResponse}
import ru.otus.sc.query.service.AccountService

import scala.concurrent.{ExecutionContext, Future}

class AccountRouter(accountService: AccountService)(implicit ec: ExecutionContext)
    extends BaseRouter {

  override def route: Route = concat(getAccountRoute, findAccountRoute)

  private val getAccountEndpoint: Endpoint[UUID, String, Account, Any] = baseEndpoint
    .in("account")
    .get
    .in(path[UUID]("accountId"))
    .description("Account identifier (UUID)")
    .errorOut(stringBody)
    .out(anyJsonBody[Account])
    .description("Прочитать данные по счету по UUID")


  private val findAccountEndpoint: Endpoint[(UUID, Option[Int], Option[Int]), String, Seq[Account], Any] = baseEndpoint
    .in("account")
    .get
    .in(query[UUID]("clientId"))
    .in(query[Option[Int]]("minBalance"))
    .in(query[Option[Int]]("maxBalance"))
    .errorOut(stringBody)
    .out(anyJsonBody[Seq[Account]])
    .description("Вернуть список счетов данного клиента с балансом больше minBalance, но меньше maxBalance")


  private def getAccount(accountId: UUID): Future[Either[String, Account]] =
    accountService.read(AccountReadRequest(accountId)) map {
      case AccountReadResponse.Success(account) => Right(account)
      case AccountReadResponse.NotFound(_)      => Left(s"Account with ID=$accountId not found")
    }

  private def findAccount(queryParams: (UUID, Option[Int], Option[Int])): Future[Either[String, Seq[Account]]] = {
    accountService.find(AccountFindRequest.ByClientAndFilter(queryParams._1, acc => {
      var condition = true
      queryParams._2.foreach(minValue => condition = minValue <= acc.amount.value)
      if (condition) {
        queryParams._3.foreach(maxValue => condition = acc.amount.value <= maxValue)
      }
      condition
    })).map {
      case AccountFindResponse.Success(accounts) => Right(accounts)
      case AccountFindResponse.NotFound(_) => Right(Seq[Account]())
    }
  }


  private val getAccountRoute    = getAccountEndpoint.toRoute(getAccount)
  private val findAccountRoute    = findAccountEndpoint.toRoute(findAccount)

  override def getEndpoints: Seq[AppTapir.Endpoint[_, _, _, _]] =
    Seq(getAccountEndpoint, findAccountEndpoint)
}
