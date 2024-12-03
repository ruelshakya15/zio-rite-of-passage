package com.rockthejvm.reviewboard.repositories

import zio.*
import zio.test.*

import org.testcontainers.containers.PostgreSQLContainer // TEST CONTAINER
import org.postgresql.ds.PGSimpleDataSource

import javax.sql.DataSource // DATA SOURCE USED TO BUILD QUILL INSTANCE

trait RepositorySpec {

  val initScript: String

  // *************** TEST CONTAINERS(Feature of JVM)***************
  // spawn a Postgres instance on Docker just for the test
  private def createContainer() = {
    val container: PostgreSQLContainer[
      Nothing
    ] = // if you look at defn <SELF> used which is like F-bounded polymorphism so we need to pass Nothing
      PostgreSQLContainer("postgres").withInitScript(initScript)

    container.start()
    container
  }

  // create a DataSource to connect to the Postgres
  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = { // "Datasource" is of type javax.sql.DataSource
    val dataSource = new PGSimpleDataSource()
    dataSource.setUrl(
      container.getJdbcUrl()
    ) // PLAIN JAVA way to connect to DB/PostgreSQL container
    dataSource.setUser(container.getUsername())
    dataSource.setPassword(container.getPassword())
    dataSource
  }

  // use the DataSource (as a ZLayer) to build the Quill instance (as a ZLayer)
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }

}
