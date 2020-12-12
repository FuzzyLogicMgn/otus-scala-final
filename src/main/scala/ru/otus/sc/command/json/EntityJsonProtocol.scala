package ru.otus.sc.command.json

import play.api.libs.json.{Json, OFormat}
import ru.otus.sc.common.model.{Account, Amount, Client, Transaction}

trait EntityJsonProtocol {
  implicit lazy val amountFormat: OFormat[Amount]                = Json.format
  implicit lazy val clientFormat: OFormat[Client]                = Json.format
  implicit lazy val accFormat: OFormat[Account]                  = Json.format
  implicit lazy val tranFormat: OFormat[Transaction]             = Json.format
}

object EntityJsonProtocol extends EntityJsonProtocol
