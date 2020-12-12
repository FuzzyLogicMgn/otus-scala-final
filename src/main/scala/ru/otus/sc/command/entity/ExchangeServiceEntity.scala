package ru.otus.sc.command.entity

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.slf4j.LoggerFactory
import ru.otus.sc.common.model.Currency.Currency
import ru.otus.sc.common.model.{Amount, Currency, ExchangeRate}

import scala.xml.XML

object ExchangeServiceEntity {
  private val logger = LoggerFactory.getLogger(ExchangeServiceEntity.getClass)
  private val uri =
    "https://iss.moex.com/iss/statistics/engines/futures/markets/indicativerates/securities"
  private val BILLING_CURRENCY          = Currency.RUB
  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ExchangeService")
  private lazy val exchangeRatesBySecId: Map[String, ExchangeRate] = {
    import java.net.URI
    import java.net.http.HttpResponse.BodyHandlers
    import java.net.http.{HttpClient, HttpRequest}

    // Запрашиваем XML с курсами валют
    logger.info("Request currency exchange rates...")
    val client   = HttpClient.newHttpClient
    val request  = HttpRequest.newBuilder.uri(URI.create(uri)).build
    val response = client.send(request, BodyHandlers.ofString)
    logger.info(response.body)

    // Разбираем XML в отображение <имя_пары_валют> - <значение_курса>
    val doc = XML.loadString(response.body)
    val id2Rate: Seq[(String, ExchangeRate)] = for {
      item <- doc \\ "row" if item \@ "secid" != ""
    } yield {
      val secid = item \@ "secid"
      val rate  = item \@ "rate"
      //App.log(s"Rate: $secid: $rate")
      secid -> ExchangeRate(secid, rate.toDouble)
    }
    id2Rate.toMap
  }

  private lazy val billingCurrencyRate: Map[Currency, ExchangeRate] = {
    val currencyPairRegex = """([A-Z]{3})/([A-Z]{3})""".r
    exchangeRatesBySecId
      .flatMap(entry =>
        entry._1 match {
          case currencyPairRegex(first, BILLING_CURRENCY.secid) =>
            first match {
              case Currency(cur) => Seq(cur -> entry._2)
              case _             => Seq()
            }
          case _ => Seq()
        }
      )
  }

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      ExchangeServiceEntity(entityContext.entityId)
    }.withRole("write-model"))
  }

  def getBillingRates(currency: Currency): ExchangeRate =
    currency match {
      case BILLING_CURRENCY => ExchangeRate("BILLING_CURRENCY", 1)
      case other            => billingCurrencyRate(other)
    }

  trait Command extends CborSerializable
  final case class ConvertAmount(
      requestAmount: Amount,
      accountCurrency: Currency,
      replyTo: ActorRef[Amount]
  ) extends Command

  def apply(entityId: String): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case ConvertAmount(requestAmount, accountCurrency, replyTo) =>
        if (requestAmount.currency == accountCurrency) {
          replyTo ! requestAmount
        } else {
          val toBillingCurrencyRate            = getBillingRates(requestAmount.currency).rate
          val fromBillingToAccountCurrencyRate = 1 / getBillingRates(accountCurrency).rate
          replyTo ! Amount(
            requestAmount.value * toBillingCurrencyRate * fromBillingToAccountCurrencyRate,
            accountCurrency
          )
        }
        Behaviors.same
    }

}
