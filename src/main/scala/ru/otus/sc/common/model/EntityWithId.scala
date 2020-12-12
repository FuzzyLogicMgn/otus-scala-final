package ru.otus.sc.common.model

import java.util.UUID

trait EntityWithId[T] {
  def id: Option[UUID]
  def copyWithId(id: UUID): T
}
