package ru.otus.sc.common.model

import com.fasterxml.jackson.core.`type`.TypeReference
import play.api.libs.json.{JsPath, Reads, Writes}

object Currency extends Enumeration {

  protected case class Val(secid: String) extends super.Val

  import scala.language.implicitConversions
  implicit def valueToCurrencyVal(x: Value): Val = x.asInstanceOf[Val]

  type Currency = Val

  val USD: Currency = Val("USD")
  val EUR: Currency = Val("EUR")
  val RUB: Currency = Val("RUB")
  val CAD: Currency = Val("CAD")
  val CHF: Currency = Val("CHF")
  val CNY: Currency = Val("CNY")
  val INR: Currency = Val("INR")
  val JPY: Currency = Val("JPY")
  val TRY: Currency = Val("TRY")
  val UAH: Currency = Val("UAH")

  def unapply(arg: String): Option[Val] =
    arg match {
      case USD.secid => Some(USD)
      case EUR.secid => Some(EUR)
      case RUB.secid => Some(RUB)
      case CAD.secid => Some(CAD)
      case CHF.secid => Some(CHF)
      case CNY.secid => Some(CNY)
      case INR.secid => Some(INR)
      case JPY.secid => Some(JPY)
      case TRY.secid => Some(TRY)
      case UAH.secid => Some(UAH)
      case _         => None
    }

  implicit val EnumNoFieldsReads: Reads[Currency] =
    (JsPath \ "secid").read[String].map { x: String => Currency.unapply(x).get }

  implicit val EnumNoFieldsWrites: Writes[Currency] =
    (JsPath \ "secid").write[String].contramap { x: Currency => x.secid }

  def currencyToSecId(role: Currency): String = role.secid

  def currencyFromSecId(secid: String): Currency =
    Currency.unapply(secid) match {
      case Some(value) => value
      case None        => throw new RuntimeException(s"Unsupported currency ID: $secid")
    }

  import slick.jdbc.H2Profile.api._

  implicit val currencyType: BaseColumnType[Currency] =
    MappedColumnType.base[Currency, String](currencyToSecId, currencyFromSecId)
}

class CurrencyType extends TypeReference[Currency.type]
