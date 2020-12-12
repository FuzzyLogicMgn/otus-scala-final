package ru.otus.sc.query

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import ru.otus.sc.accounting.grpc.{EventStreamService, EventStreamServiceClient}
import ru.otus.sc.query.dao.impl.slick.{AccountDaoSlickImpl, ClientDaoSlickImpl, OffsetDaoSlickImpl, TransactionDaoSlickImpl}
import ru.otus.sc.query.dao.{AccountDao, TransactionDao}
import ru.otus.sc.query.db.Migrations
import ru.otus.sc.query.route.{AccountRouter, AppRouter, DocRouter}
import ru.otus.sc.query.service.impl.AccountServiceImpl
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
import scala.util.{Failure, Success, Using}

object QueryMain {

  def createRouter(accountDao: AccountDao, tranDao: TransactionDao)(implicit ec: ExecutionContext): AppRouter = {
    val accRouter = new AccountRouter(new AccountServiceImpl(accountDao, tranDao))
    val docRouter = new DocRouter(accRouter)
    new AppRouter(accRouter, docRouter)
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] =
      ActorSystem[Nothing](Behaviors.empty, "AccountingQuery")
    implicit val ec: ExecutionContext = system.executionContext

    val clientSettings             = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8081).withTls(false)
    val client: EventStreamService = EventStreamServiceClient(clientSettings)
    val configDbKey                = "h2mem1"
    Using.resource(Database.forConfig(configDbKey)) { db =>
      import com.typesafe.config.ConfigFactory
      val appConfig = ConfigFactory.load
      new Migrations(appConfig.getConfig(configDbKey)).applyMigrationsSync()

      val offsetDao  = new OffsetDaoSlickImpl(db)
      val clientDao  = new ClientDaoSlickImpl(db)
      val accountDao = new AccountDaoSlickImpl(db)
      val tranDao    = new TransactionDaoSlickImpl(db)

      val httpPort = system.settings.config.getInt("accounting.http.port")
      val binding = Http().newServerAt("localhost", httpPort).bind(createRouter(accountDao, tranDao).route)
      binding
        .map(_.addToCoordinatedShutdown(3.seconds))
        .onComplete {
          case Success(binding) =>
            val address = binding.localAddress
            system.log.info(
              "Accounting online at http://{}:{}/",
              address.getHostString,
              address.getPort
            )

          case Failure(ex) =>
            system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
            system.terminate()
        }

      binding.foreach(b => println(s"Start listen on ${b.localAddress}"))

      val processor  = new EventProcessor(offsetDao, clientDao, accountDao, tranDao)
      processor.processEvents(client)

      StdIn.readLine()

      binding.map(_.unbind()).onComplete(_ => system.terminate())
    }
  }

}
