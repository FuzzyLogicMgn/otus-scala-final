package ru.otus.sc.query.route

import akka.http.scaladsl.server.Route
import ru.otus.sc.common.json.AppTapir
import ru.otus.sc.common.json.AppTapir._
import ru.otus.sc.common.route.BaseRouter
import sttp.tapir.swagger.akkahttp.SwaggerAkka

class DocRouter(accountRouter: AccountRouter) extends BaseRouter {
  private val docsAsYaml: String = accountRouter.getEndpoints
    .toOpenAPI("Account Query Service", "1.0")
    .toYaml
  override def route: Route = new SwaggerAkka(docsAsYaml).routes

  override def getEndpoints: Seq[AppTapir.Endpoint[_, _, _, _]] = Seq()
}
