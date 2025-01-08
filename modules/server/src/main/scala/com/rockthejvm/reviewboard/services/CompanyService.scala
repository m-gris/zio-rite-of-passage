package com.rockthejvm.reviewboard.services

import scala.collection.mutable

import zio.*

import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.repositories.CompanyRepository

// BUSINESS LOGIC
// sits between:
//    - the HTTP LAYER
//    - and the DATABASE LAYER (aka 'repository')
// (the CompanyController will depend on this service)

// responsible for managing the data that the http server receives
trait CompanyService {
  def create(req: CompanyCreationRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}


/*
    the SERVICE layer acts as an INTERFACE between the Controller and the Repository
                      Controller <= Service => Repository

    For now... below it looks like we're "just" delegating to the repo...
    but as the business becomes more complex, this separation will prove usefull.

    Separation between HTTP (service) , BUSINESS (service) , DATABASE (repo)

 */
class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {

  override def create(req: CompanyCreationRequest): Task[Company] =
    repo.create(req.toCompany(-1L))

  override def getAll: Task[List[Company]] = repo.getAll

  override def getById(id: Long): Task[Option[Company]] = repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] = repo.getBySlug(slug)
}

object CompanyServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repo)
  }
}

class CompanyServiceDummy extends CompanyService {

  // in-memory DB for now
  val db: mutable.Map[Long, Company] = mutable.Map()

  override def create(req: CompanyCreationRequest): Task[Company] = ZIO.succeed {
      val newId = db.keys.maxOption.getOrElse(0L) + 1
      val newCompany = req.toCompany(newId)
      db += (newId -> newCompany)
      newCompany
    }

  override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

  override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(db.values.find(_.slug == slug))

}


