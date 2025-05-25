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
import com.rockthejvm.reviewboard.domain.data.CompanyFilter


object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript  = "sql/companies.sql"

  private val rtjvm = Company(-1L, "Rock The JVM", "rockthejvm.com", "rock-the-jvm")

  private def genString(): String =
    scala.util.Random.alphanumeric.take(8).mkString

  private def genCompany(): Company =
    Company(
      id=1L,
      name=genString(),
      slug=genString(),
      url=genString(),
      location = Some(genString()),
      country = Some(genString()),
      industry = Some(genString()),
      tags = (1 to 3).map(_ => genString()).toList
    )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Test Company Repository Spec")(

      test("create a company") {

        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
        } yield company

        program.assert {
          case Company( _, "Rock The JVM", "rockthejvm.com", "rock-the-jvm",
                        _, _, _, _, _)    => true

          case _                          => false

        }
      },

      test("duplication should raise an error") {

        val program = for {
          repo  <- ZIO.service[CompanyRepository]
          _     <- repo.create(rtjvm)
          error <- repo.create(rtjvm).flip
        } yield error

        program.assert(_.isInstanceOf[SQLException])
      },

      test("get by ID & Slug") {

        val program = for {
          repo           <- ZIO.service[CompanyRepository]
          companyCreated <- repo.create(rtjvm)              // Company
          companyById    <- repo.getById(companyCreated.id) // Option[Company]
          companyBySlug  <- repo.getById(companyCreated.id) // Option[Company]
        } yield (companyCreated, companyById, companyBySlug)

        program.assert {
          case (companyCreated, companyById, companyBySlug) =>
            // nb: companyById is an Option[Company] !!!!
            companyById.contains(companyCreated)
            &&
            companyBySlug.contains(companyCreated)
        }
      },

      test("updated record") {

        val program = for {
          repo       <- ZIO.service[CompanyRepository]
          created    <- repo.create(rtjvm)
          updated    <- repo.update(created.id, _.copy(url="blog.rockthejvm.com"))
          byId       <- repo.getById(created.id)
        } yield (updated, byId)

        program.assert {
          case (updated, byId) => byId.contains(updated)
        }
      },

      test("delete record") {

        val program = for {
          repo       <- ZIO.service[CompanyRepository]
          created    <- repo.create(rtjvm)
          updated    <- repo.delete(created.id)
          byId       <- repo.getById(created.id)
        } yield (byId)

        program.assert(_.isEmpty) // i.e None
      },

      test("get all companies") {

        val program = for {
          repo       <- ZIO.service[CompanyRepository]
          created    <- ZIO.collectAll((0 to 10).map(_ => repo.create(genCompany())))
          fetched   <- repo.getAll
        } yield (created, fetched)

        program.assert{ case (created, fetched) => 
          created.toSet == fetched.toSet }
      },

      test("search by tag") {

        val program = for {
          repo       <- ZIO.service[CompanyRepository]
          created    <- repo.create(genCompany())
          fetched   <- repo.search(CompanyFilter(tags=created.tags.headOption.toList))
        } yield (created, fetched)

        program.assert{ case (created, fetched) => 
          fetched.nonEmpty &&
          fetched.tail.isEmpty &&
          fetched.head == created
        }
      },

      ).provide(
        CompanyRepositoryLive.layer,
        dataSourceLayer,
        Repository.dbLayer,
        Scope.default
        )



}
