package ru.otus.sc.command.model

import ru.otus.sc.common.model.Currency.Currency
import ru.otus.sc.common.model.{Amount, ExchangeRate}

case class ExchangeRatesRequest(secid: String)
case class ExchangeRatesResponse(rate: Option[ExchangeRate])

case class ExchangeRatesConvertRequest(amount: Amount, target: Currency)
sealed trait ExchangeRatesConvertResponse
object ExchangeRatesConvertResponse {
  case class Success(amount: Amount) extends ExchangeRatesConvertResponse
  case class RateNotFound(secid: String) extends ExchangeRatesConvertResponse
}
