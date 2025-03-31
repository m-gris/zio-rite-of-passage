package com.rockthejvm.reviewboard.repositories

import zio.*
import zio.test.*
import javax.sql.DataSource
import java.sql.SQLException
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer

import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.domain.data.Company

trait RepositorySpec extends ZIOSpecDefault {

  val initScript: String

  // TEST CONTAINERS
  // spawn a postgres on Docker just for the test
  def createContainer() = {
    val container : PostgreSQLContainer[Nothing] =
        PostgreSQLContainer("postgres")
          .withInitScript(initScript)
    container.start()
    container
  }

  // create DataSource to connect to the Postgres
  def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {

    // a STATEFUL datastructure used TO CONNECT to the container
    val dataSource = new PGSimpleDataSource() // a plain JDBC type that comes pre-bundled with postgres java lib

    dataSource.setUrl(container.getJdbcUrl())
    dataSource.setUser(container.getUsername())
    dataSource.setPassword (container.getPassword())
    dataSource
  }

  // use the datasource to CREATE THE QUILL INSTANCE (as a ZLayer)
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(
                        // the ACQUIRING EFFECT
                        ZIO.attempt(createContainer())
                        )(
                        // the RELEASING EFFECT
                        container => ZIO.attempt(container.stop()).ignoreLogged // empties / redirects the error from the error channel to the console
                        )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }

}
