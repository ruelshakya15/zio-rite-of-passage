package com.rockthejvm.reviewboard.services

import zio.*

import org.flywaydb.core.Flyway

import com.rockthejvm.reviewboard.config.FlywayConfig
import com.rockthejvm.reviewboard.config.Configs

trait FlywayService {
  // db schema maintained
  // migration(change to db) => versioned

  def runClean: Task[Unit]
  def runBaseline: Task[Unit]
  def runMigrations: Task[Unit] // look at all changes we specified
  def runRepairs: Task[Unit]
}

class FlywayServicelive private (flyway: Flyway) extends FlywayService {
    override def runClean: Task[Unit] = 
        // hover read doc for more but, dedicated thread pool so main zio pool not interfered with
        ZIO.attemptBlocking(flyway.clean())

    override def runBaseline: Task[Unit] = 
        ZIO.attemptBlocking(flyway.baseline())

    override def runMigrations: Task[Unit] = 
        ZIO.attemptBlocking(flyway.migrate())

    override def runRepairs: Task[Unit] = 
        ZIO.attemptBlocking(flyway.repair())
}

object FlywayServicelive {
    val layer = ZLayer{
        for {
            config <- ZIO.service[FlywayConfig]
            flyway <- ZIO.attempt(
                Flyway
                .configure()
                .dataSource(config.url, config.user, config.password)
                .load()
            )
        } yield new FlywayServicelive(flyway)
    }

    val configuredLayer = 
        Configs.makeLayer[FlywayConfig]("rockthejvm.db.dataSource") >>> layer
}
