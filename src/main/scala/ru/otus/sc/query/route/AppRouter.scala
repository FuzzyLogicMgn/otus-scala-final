package ru.otus.sc.query.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.otus.sc.common.route.BaseRouter
import sttp.tapir.Endpoint

class AppRouter(accountRouter: AccountRouter, docRouter: DocRouter)
    extends BaseRouter {
  override def route: Route = concat(accountRouter.route, docRouter.route)

  override def getEndpoints: Seq[Endpoint[_, _, _, _]] = Seq()
}
