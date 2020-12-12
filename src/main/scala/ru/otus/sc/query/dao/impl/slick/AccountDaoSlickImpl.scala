package ru.otus.sc.query.dao.impl.slick

import java.util.UUID

import ru.otus.sc.common.model
import ru.otus.sc.common.model.Currency.Currency
import ru.otus.sc.common.model.{Account, Amount}
import ru.otus.sc.query.dao.AccountDao
import slick.jdbc.H2Profile.api._

import scala.concurrent.{ExecutionContext, Future}

class AccountDaoSlickImpl(db: Database)(implicit ec: ExecutionContext) extends AccountDao {

  import AccountDaoSlickImpl._

  override def create(ent: Account): Future[Account] = {
    val newAccount = AccountRow.fromAccount(ent)
    val act = for {
      _ <- accounts += newAccount
    } yield ent
    db.run(act.transactionally)
  }

  override def update(ent: Account): Future[Option[Account]] = {
    ent.id match {
      case Some(accountId) =>
        val updateAct = accounts
          .filter(cl => cl.id === accountId)
          .map(u => (u.org_code, u.balance, u.currency))
          .update((ent.organization, ent.amount.value, ent.amount.currency))
        val act = for {
          upd <- accounts.filter(cl => cl.id === accountId).forUpdate.result.headOption
          _ <- upd match {
            case Some(_) => updateAct
            case None    => DBIO.successful(())
          }
        } yield upd.map(_.toAccount)
        db.run(act.transactionally)
      case None => Future.successful(None)
    }
  }

  override def updateBalance(accountId : UUID, diff: Amount): Future[Option[Account]] = {
    val updateAct = sqlu"update ACCOUNT set balance = balance + ${diff.value} where id=${accountId.toString}"
    val act = for {
      upd <- accounts.filter(cl => cl.id === accountId).forUpdate.result.headOption
      _ <- upd match {
        case Some(_) => updateAct
        case None    => DBIO.successful(())
      }
    } yield upd.map(_.toAccount)
    db.run(act.transactionally)
  }

  override def read(entityId: UUID): Future[Option[Account]] = {
    val act = for {
      user <- accounts.filter(cl => cl.id === entityId).result.headOption
    } yield user.map(_.toAccount)
    db.run(act)
  }

  override def delete(entityId: UUID): Future[Option[Account]] = {
    val act = for {
      del <- accounts.filter(_.id === entityId).forUpdate.result.headOption
      _ <- del match {
        case Some(_) => accounts.filter(cl => cl.id === entityId).delete
        case None    => DBIO.successful(())
      }
    } yield del.map(_.toAccount)
    db.run(act.transactionally)
  }

  override def findAll(): Future[Seq[Account]] = {
    val act = for {
      all <- accounts.result
    } yield all.map(_.toAccount)
    db.run(act)
  }

}

object AccountDaoSlickImpl {

  case class AccountRow(
      id: Option[UUID],
      clientId: UUID,
      org: String,
      balance: Double,
      currency: Currency
  ) {
    def toAccount: Account = model.Account(id, clientId, model.Amount(balance, currency))
  }

  object AccountRow extends ((Option[UUID], UUID, String, Double, Currency) => AccountRow) {
    def fromAccount(acc: Account): AccountRow =
      AccountRow(acc.id, acc.clientId, acc.organization, acc.amount.value, acc.amount.currency)
  }

  class Accounts(tag: Tag) extends Table[AccountRow](tag, "ACCOUNT") {
    val id       = column[UUID]("ID", O.PrimaryKey)
    val clientId = column[UUID]("CLIENT_ID")
    val org_code = column[String]("ORG_CODE")
    val balance  = column[Double]("BALANCE")
    val currency = column[Currency]("CURRENCY")
    def clientFK = foreignKey("client_fk", clientId, ClientDaoSlickImpl.clients)(_.id)

    override def * = (id.?, clientId, org_code, balance, currency).mapTo[AccountRow]
  }

  val accounts = TableQuery[Accounts]
}
