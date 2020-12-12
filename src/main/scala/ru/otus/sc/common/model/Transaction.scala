package ru.otus.sc.common.model

import java.util.UUID

case class Transaction(
    id: Option[UUID],
    accountId: UUID,
    amount: Amount
) extends EntityWithId[Transaction] {
  override def copyWithId(id: UUID): Transaction = copy(Some(id))
}
