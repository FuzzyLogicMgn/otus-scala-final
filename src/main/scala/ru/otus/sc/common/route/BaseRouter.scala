package ru.otus.sc.common.route

import akka.http.scaladsl.server.Route
import ru.otus.sc.command.json.AppTapir._

trait BaseRouter {
  def route: Route
  def getEndpoints: Seq[Endpoint[_, _, _, _]]
  val baseEndpoint: Endpoint[Unit, Unit, Unit, Any] = endpoint.in("api" / "v1")
}
