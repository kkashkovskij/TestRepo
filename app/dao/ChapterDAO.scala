package dao

import javax.inject.Inject

import controllers.ChapterFormModel

import scala.concurrent.{ExecutionContext, Future}
import models.Chapter
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent.ExecutionContext

class ChapterDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Chapters = TableQuery[ChaptersTable]

  def all(): Future[Seq[Chapter]] = db.run(Chapters.result)

  def insert(chapter: Chapter): Future[Unit] = db.run(Chapters += chapter).map { _ => () }

  def delete(id: Int): Unit ={
    val q = Chapters.filter(_.id === id)
    val action = q.delete
    val affectedRowCound: Future[Int] = db.run(action)
    val sql = action.statements.head
  }

  def modify(id: Int, shortName: String, fullName: String, text: String): Unit = {

    if (shortName != "") db.run((for { a <- Chapters if a.id === id } yield a.shortName).update(shortName))
    if (fullName != "")db.run((for { a <- Chapters if a.id === id } yield a.fullName).update(fullName))
    if (text != "")db.run((for { a <- Chapters if a.id === id } yield a.text).update(text))
  }

  private class ChaptersTable(tag: Tag) extends Table[Chapter] (tag, "chapters"){

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def shortName = column[String]("shortname")
    def fullName = column[String]("fullname")
    def text = column[String]("text")
    def parentId = column[Option[Int]]("parentid")


    def * = (id, shortName, fullName, text, parentId) <> (Chapter.tupled, Chapter.unapply)
  }
}
