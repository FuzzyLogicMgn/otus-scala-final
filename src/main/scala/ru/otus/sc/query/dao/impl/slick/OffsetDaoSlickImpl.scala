package ru.otus.sc.query.dao.impl.slick

import ru.otus.sc.query.dao.OffsetDao
import slick.jdbc.H2Profile.api._

import scala.concurrent.{ExecutionContext, Future}

class OffsetDaoSlickImpl(db: Database)(implicit ec: ExecutionContext) extends OffsetDao {

  import OffsetDaoSlickImpl._

  override def storeOffset(name: String, offset: Long): Future[Long] = {
    val act = for {
      _ <- sqlu"MERGE INTO AKKA_OFFSET_STORE (NAME, CURRENT_OFFSET) KEY (NAME) VALUES ($name, $offset)"
    } yield offset
    db.run(act.transactionally) recover( ex => {
      ex.printStackTrace()
      0L
    } )
  }

  override def readOffset(name: String): Future[Option[Long]] = {
    val act = for {
      user <- offsets.filter(off => off.name === name).result.headOption
    } yield user.map(_.offset)
    db.run(act)
  }

  object OffsetDaoSlickImpl {

    case class OffsetRow(name: String, offset: Long)

    object OffsetRow extends ((String, Long) => OffsetRow)

    class Offsets(tag: Tag) extends Table[OffsetRow](tag, "AKKA_OFFSET_STORE") {
      val name   = column[String]("NAME", O.PrimaryKey)
      val offset = column[Long]("CURRENT_OFFSET")

      override def * = (name, offset).mapTo[OffsetRow]
    }

    val offsets = TableQuery[Offsets]
  }
}
