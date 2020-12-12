package ru.otus.sc.command

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.typed.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.management.scaladsl.AkkaManagement
import akka.persistence.journal.PersistencePluginProxyExtension
import com.typesafe.config.{Config, ConfigFactory}
import ru.otus.sc.accounting.grpc.EventStreamServiceHandler
import ru.otus.sc.command.entity.{AccountEntity, ClientEntity, ExchangeServiceEntity}
import ru.otus.sc.command.route.{AccountRouter, ClientRouter, DocRouter}
import ru.otus.sc.command.service.impl.{AccountServiceImpl, ClientServiceImpl, EventStreamServiceImpl}
import ru.otus.sc.common.route.AppRouter

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}


//TODO: настроить сериализацию LoadSnapshot
//В примере создания пользоваля убрать ID
//REST неверные типы данных
//Transaction
//Сохранение Offset
//Структура кода
//Http сервер для query части
//Тесты
//Вынести конфигурацию в файл
//Уровень логирования
//Переименовать Transaction в proto
// sbt -J-Dconfig.resource=read-node/application.conf "runMain GreeterClient"


object CommandMain {

  def createRouter()(implicit ec: ExecutionContext, system: ActorSystem[_]): AppRouter = {

    val service = new ClientServiceImpl()
    val clientRouter = new ClientRouter(service)

    val accRouter = new AccountRouter(new AccountServiceImpl())

    val docRouter = new DocRouter(clientRouter, accRouter)

    val eventStream: PartialFunction[HttpRequest, Future[HttpResponse]] = EventStreamServiceHandler.partial(new EventStreamServiceImpl())

    new AppRouter(clientRouter, accRouter, docRouter, eventStream)
  }

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val httpPort = ("80" + portString.takeRight(2)).toInt
        val managementPort = ("85" + portString.takeRight(2)).toInt
        startNode(port, httpPort, managementPort)

      case None =>
        startNode(2081, 8081, 8581)
    }
  }

  def startNode(port: Int, httpPort: Int, managementPort: Int): Unit = {
    val system: ActorSystem[Nothing] =
      ActorSystem[Nothing](Guardian(), "Accounting", config(port, httpPort, managementPort))
  }

  def config(port: Int, httpPort: Int, managementPort: Int): Config = {
    if (port == 2081) {
      ConfigFactory
        .parseString(
          s"""
      akka.persistence.journal.proxy.start-target-journal = on
      akka.persistence.snapshot-store.proxy.start-target-snapshot-store = on
      akka.remote.artery.canonical.port = $port
      accounting.http.port = $httpPort
      akka.management.http.hostname = "127.0.0.1"
      akka.management.http.port = $managementPort
       """)
        .withFallback(ConfigFactory.load())
    } else {
      ConfigFactory
        .parseString(
          s"""
      akka.persistence.journal.proxy.target-journal-address = "akka://Accounting@127.0.0.1:2081"
      akka.persistence.snapshot-store.proxy.target-snapshot-store-address = "akka://Accounting@127.0.0.1:2081"
      akka.remote.artery.canonical.port = $port
      accounting.http.port = $httpPort
      akka.management.http.hostname = "127.0.0.1"
      akka.management.http.port = $managementPort
       """)
        .withFallback(ConfigFactory.load())
    }
  }

  object Guardian {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { context =>
        implicit val system: ActorSystem[Nothing] = context.system
        import system.executionContext

        PersistencePluginProxyExtension(system)

//        val persistence = Persistence(system)
//        persistence.getClass.getDeclaredMethod("journalFor", classOf[String], classOf[Config]).invoke(persistence, null, ConfigFactory.empty)
//        persistence.getClass.getDeclaredMethod("snapshotStoreFor", classOf[String], classOf[Config]).invoke(persistence, null, ConfigFactory.empty)

        ClientEntity.init(context.system)
        AccountEntity.init(context.system)
        ExchangeServiceEntity.init(context.system)

        val cluster = Cluster(system)
        if (cluster.selfMember.roles("management")) {
          val akkaManagement = AkkaManagement(system)
          akkaManagement.start().map { _ =>
            // add a task to stop
            CoordinatedShutdown(system).addTask(
              CoordinatedShutdown.PhaseBeforeServiceUnbind,
              "stop-akka-http-management"
            ) { () =>
              akkaManagement.stop()
            }
            Done
          }
        }

        val httpPort = context.system.settings.config.getInt("accounting.http.port")

        val binding  = Http().newServerAt("localhost", httpPort).bind(createRouter().route)
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

        StdIn.readLine()

        binding.map(_.unbind()).onComplete(_ => system.terminate())
        Behaviors.empty
      }
  }

}
