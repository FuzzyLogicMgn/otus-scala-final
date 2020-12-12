package ru.otus.sc.command.route

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.otus.sc.common.route.BaseRouter
import sttp.tapir.Endpoint

import scala.concurrent.Future

class AppRouter(clientRouter: ClientRouter, accountRouter: AccountRouter, docRouter: DocRouter, eventStream: PartialFunction[HttpRequest, Future[HttpResponse]])
    extends BaseRouter {
  override def route: Route = concat(handle(eventStream), clientRouter.route, accountRouter.route, docRouter.route)

  override def getEndpoints: Seq[Endpoint[_, _, _, _]] = Seq()
}
