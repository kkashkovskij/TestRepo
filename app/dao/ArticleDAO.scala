package dao

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import models.Article
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import play.api.db.slick.HasDatabaseConfigProvider

import scala.concurrent.ExecutionContext

class ArticleDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Articles = TableQuery[ArticlesTable]

  def all(): Future[Seq[Article]] = db.run(Articles.result)

  def insert(article: Article): Future[Unit] = db.run(Articles += article).map { _ => () }

  def delete(id: Int): Unit ={
    val q = Articles.filter(_.id === id)
    val action = q.delete
    val affectedRowCound: Future[Int] = db.run(action)
    val sql = action.statements.head
  }

  def modify(id: Int, shortName: String, fullName: String, text: String): Unit = {

    if (shortName != "") db.run((for { a <- Articles if a.id === id } yield a.shortName).update(shortName))
    if (fullName != "")db.run((for { a <- Articles if a.id === id } yield a.fullName).update(fullName))
    if (text != "")db.run((for { a <- Articles if a.id === id } yield a.text).update(text))

  }


  private class ArticlesTable(tag: Tag) extends Table[Article] (tag, "articles"){

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def shortName = column[String]("shortname")
    def fullName = column[String]("fullname")
    def text = column[String]("text")
    def chapterId = column[Int]("chapterid")

    def * = (id, shortName, fullName, text, chapterId) <> (Article.tupled, Article.unapply)

  }



}