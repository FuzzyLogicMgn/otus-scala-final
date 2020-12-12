package ru.otus.sc.query.dao

import java.util.UUID

import ru.otus.sc.common.model.{Account, Amount}

import scala.concurrent.Future

trait AccountDao extends EntityDao[Account] {
  def updateBalance(accountId : UUID, diff: Amount): Future[Option[Account]]
}


