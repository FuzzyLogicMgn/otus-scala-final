package ru.otus.sc.query.dao

import java.util.UUID

import ru.otus.sc.common.model.EntityWithId

import scala.concurrent.Future

trait EntityDao[T <: EntityWithId[T]] {
  def create(ent: T): Future[T]
  def update(ent: T): Future[Option[T]]
  def read(entityId: UUID): Future[Option[T]]
  def delete(entityId: UUID): Future[Option[T]]
  def findAll(): Future[Seq[T]]
}


