package ru.otus.sc.common.model

import java.util.UUID

import ru.otus.sc.common.model

case class Account(
                    id: Option[UUID],
                    clientId: UUID,
                    amount: Amount = model.Amount(0),
                    organization: String = "SBR"
) extends EntityWithId[Account] {
  override def copyWithId(id: UUID): Account = copy(Some(id))
}
