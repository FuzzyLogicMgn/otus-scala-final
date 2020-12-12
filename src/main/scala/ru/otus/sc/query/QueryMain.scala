package ru.otus.sc.query

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import ru.otus.sc.accounting.grpc.{EventStreamService, EventStreamServiceClient}
import ru.otus.sc.query.dao.impl.slick.{AccountDaoSlickImpl, ClientDaoSlickImpl, OffsetDaoSlickImpl, TransactionDaoSlickImpl}
import ru.otus.sc.query.db.Migrations
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.Using

object QueryMain {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("AccountingReadModel")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val clientSettings             = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8081).withTls(false)
    val client: EventStreamService = EventStreamServiceClient(clientSettings)
    val configDbKey = "h2mem1"
    Using.resource(Database.forConfig(configDbKey)) { db =>
      import com.typesafe.config.ConfigFactory
      val appConfig = ConfigFactory.load
      new Migrations(appConfig.getConfig(configDbKey)).applyMigrationsSync()
      val offsetDao  = new OffsetDaoSlickImpl(db)
      val clientDao  = new ClientDaoSlickImpl(db)
      val accountDao = new AccountDaoSlickImpl(db)
      val tranDao    = new TransactionDaoSlickImpl(db)
      val processor = new EventProcessor(offsetDao, clientDao, accountDao, tranDao)

//      val binding = Http().newServerAt("localhost", 8080).bind(createRouter(db).route)
//
//      binding.foreach(b => println(s"Start listen on ${b.localAddress}"))
      processor.processEvents(client)
      StdIn.readLine()

//      binding.map(_.unbind()).onComplete(_ => system.terminate())
    }
  }

}
