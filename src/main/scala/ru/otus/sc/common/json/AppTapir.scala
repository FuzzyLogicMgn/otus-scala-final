package ru.otus.sc.common.json

import play.api.libs.json.{Json, OFormat}
import sttp.tapir.docs.openapi.TapirOpenAPIDocs
import sttp.tapir.json.play.TapirJsonPlay
import sttp.tapir.openapi.circe.yaml.TapirOpenAPICirceYaml
import sttp.tapir.server.akkahttp.TapirAkkaHttpServer
import sttp.tapir.{Tapir, TapirAliases}

object AppTapir
    extends Tapir
    with TapirAkkaHttpServer
    with TapirJsonPlay
    with TapirAliases
    with TapirOpenAPIDocs
    with TapirOpenAPICirceYaml {

  sealed trait ErrorInfo
  case class NotFound(what: String)          extends ErrorInfo
  case class Forbidden(cause: String)        extends ErrorInfo
  case class Unknown(code: Int, msg: String) extends ErrorInfo

  implicit lazy val notFoundFormat: OFormat[NotFound]   = Json.format
  implicit lazy val forbiddenFormat: OFormat[Forbidden] = Json.format
  implicit lazy val unknownFormat: OFormat[Unknown]     = Json.format
}
