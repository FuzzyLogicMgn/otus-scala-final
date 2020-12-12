package ru.otus.sc.common.service

/**
  * Родитель для всех сервисов приложения.
  */
trait AppService {
  def getServiceName: String
}
