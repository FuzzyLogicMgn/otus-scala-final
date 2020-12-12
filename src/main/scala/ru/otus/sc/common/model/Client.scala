package ru.otus.sc.common.model

import java.util.UUID

case class Client(id: Option[UUID], name: String) extends EntityWithId[Client] {
  override def copyWithId(id: UUID): Client = copy(Some(id))
}
