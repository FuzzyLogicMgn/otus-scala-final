package ru.otus.sc.query.dao

import scala.concurrent.Future

trait OffsetDao {
  def storeOffset(name: String, offset: Long): Future[Long]
  def readOffset(name: String): Future[Option[Long]]
}