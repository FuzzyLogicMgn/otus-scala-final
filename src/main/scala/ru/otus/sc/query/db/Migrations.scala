package ru.otus.sc.query.db

import com.typesafe.config.Config
import org.flywaydb.core.Flyway

class Migrations(config: Config) {

  def applyMigrationsSync(): Unit =
    Flyway
      .configure()
      .dataSource(config.getString("url"), config.getString("user"), null)
      .load()
      .migrate()
}
