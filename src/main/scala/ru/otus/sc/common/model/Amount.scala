package ru.otus.sc.common.model

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import ru.otus.sc.common.model.Currency.Currency

case class Amount(
    value: Double,
    @JsonScalaEnumeration(classOf[CurrencyType]) currency: Currency = Currency.RUB
)
