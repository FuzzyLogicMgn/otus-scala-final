package ru.otus.sc.query.dao.impl.slick

import java.time.LocalDateTime
import java.util.UUID

import ru.otus.sc.common.model
import ru.otus.sc.common.model.Currency.Currency
import ru.otus.sc.common.model.Transaction
import ru.otus.sc.query.dao.TransactionDao
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class TransactionDaoSlickImpl(db: Database)(implicit ec: ExecutionContext) extends TransactionDao {

  import TransactionDaoSlickImpl._

  override def create(ent: Transaction): Future[Transaction] = {
    val newTran = TransactionRow.fromAccount(ent)
    val act = for {
      tranId <- transactions.returning(transactions.map(_.id)) += newTran
    } yield ent.copy(id = Some(tranId))
    db.run(act.transactionally)
  }

  override def update(ent: Transaction): Future[Option[Transaction]] = {
    ent.id match {
      case Some(tranId) =>
        val updateAct = transactions
          .filter(cl => cl.id === tranId)
          .map(u => (u.amount, u.currency))
          .update((ent.amount.value, ent.amount.currency))
        val act = for {
          upd <- transactions.filter(cl => cl.id === tranId).forUpdate.result.headOption
          _ <- upd match {
            case Some(_) => updateAct
            case None    => DBIO.successful(())
          }
        } yield upd.map(_.toTransaction)
        db.run(act.transactionally)
      case None => Future.successful(None)
    }
  }

  override def read(entityId: UUID): Future[Option[Transaction]] = {
    val act = for {
      user <- transactions.filter(cl => cl.id === entityId).result.headOption
    } yield user.map(_.toTransaction)
    db.run(act)
  }

  override def delete(entityId: UUID): Future[Option[Transaction]] = {
    val act = for {
      del <- transactions.filter(_.id === entityId).forUpdate.result.headOption
      _ <- del match {
        case Some(_) => transactions.filter(cl => cl.id === entityId).delete
        case None    => DBIO.successful(())
      }
    } yield del.map(_.toTransaction)
    db.run(act.transactionally)
  }

  override def findAll(): Future[Seq[Transaction]] = {
    val act = for {
      all <- transactions.result
    } yield all.map(_.toTransaction)
    db.run(act)
  }

}

object TransactionDaoSlickImpl {

  case class TransactionRow(
      id: Option[UUID],
      accountId: UUID,
      balance: Double,
      currency: Currency
  ) {
    def toTransaction: Transaction = model.Transaction(id, accountId, model.Amount(balance, currency))
  }

  object TransactionRow
      extends ((Option[UUID], UUID, Double, Currency) => TransactionRow) {
    def fromAccount(tran: Transaction): TransactionRow =
      TransactionRow(tran.id, tran.accountId, tran.amount.value, tran.amount.currency)
  }

  class Transactions(tag: Tag) extends Table[TransactionRow](tag, "TRAN") {
    val id        = column[UUID]("ID", O.PrimaryKey, O.AutoInc)
    val accountId = column[UUID]("ACCOUNT_ID")
    val amount    = column[Double]("AMOUNT")
    val currency  = column[Currency]("CURRENCY")
    val date      = column[LocalDateTime]("DATE")

    def accountFK = foreignKey("account_fk", accountId, AccountDaoSlickImpl.accounts)(_.id)

    override def * = (id.?, accountId, amount, currency).mapTo[TransactionRow]
  }

  val transactions = TableQuery[Transactions]
}
