package controllers

import javax.inject.Inject

import dao.{ArticleDAO, ChapterDAO}
import models.{Article, Chapter, TextItem}
import play.api.data.Form
import play.api.data.Forms.{mapping, number, text}
import play.api.i18n.{I18nSupport}
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}


class Application @Inject()(
                             articleDao: ArticleDAO,
                             chapterDao: ChapterDAO,
                             controllerComponents: ControllerComponents
                           )(implicit executionContext: ExecutionContext) extends AbstractController(controllerComponents) with I18nSupport {


  def index = Action.async { implicit request =>

    articleDao.all().zip(chapterDao.all()).map { case (articles, chapters) => {
      val treeChapter = TreeChapter(null)
      treeChapter.setChildren(chapters)
      Ok(views.html.index(articleForm, chapterForm, treeChapter, articles, chapters))
    }
    }
  }

  def chapterInfo(chapterId: Int) = Action { implicit request =>


    var result: TreeChapter = TreeChapter(null)
    var path: String = ""

    def getChapterById(treeChapter: TreeChapter): Unit = {

      if ((treeChapter.getChapter() != null) && (treeChapter.getChapter().id == chapterId)) result = treeChapter
      else {
        for (c <- treeChapter.getChildren()) {
          getChapterById(c)
        }
      }
    }

    def getChapterPath(chapter: Chapter): Unit = {

      for (c <- chapters) {
        if (chapter.parentId.getOrElse(0) == c.id) {
          path = c.shortName + "/" + path
          getChapterPath(c)
        }
      }
    }

    val treeChapter = TreeChapter(null)
    treeChapter.setChildren(chapters)
    getChapterById(treeChapter)
    path = result.getChapter().shortName
    getChapterPath(result.getChapter())

    Ok(views.html.chapterInfo(result, chapters, articles, path, chapterForm.fill(ChapterFormModel(result.getChapter().shortName,
      result.getChapter().fullName,
      result.getChapter().text,
      result.getChapter().parentId.getOrElse(-1)))))
  }

  def articleInfo(articleId: Int) = Action { implicit request =>

    var article = articles.apply(0)

    for (a <- articles) {
      if (a.id == articleId) article = a
    }

    var path = article.shortName

    for (c <- chapters)
      if (c.id == article.chapterId) getArticlePath(c)

    def getArticlePath(chapter: Chapter): Unit = {

      for (c <- chapters) {
        if (chapter.parentId.getOrElse(0) == c.id) {
          path = c.shortName + "/" + path
          getArticlePath(c)
        }
      }
    }

    Ok(views.html.articleInfo(article, path, articleForm.fill(ArticleFormModel(article.shortName,
      article.fullName,
      article.text,
      article.chapterId))))
  }


  def deleteArticle(id: Int) = Action { implicit request =>
    articleDao.delete(id)
    Redirect(routes.Application.index)
  }

  def deleteChapter(id: Int) = Action { implicit request =>
    chapterDao.delete(id)
    Redirect(routes.Application.index)
  }

  def editChapter(id: Int) = Action { implicit request =>

    val edit: ChapterFormModel = chapterForm.bindFromRequest.get
    chapterDao.edit(Chapter(id, edit.shortName, edit.fullName, edit.text, if (edit.parentId == -1) None else Some(edit.parentId)))
    Redirect(routes.Application.index)
  }

  def editArticle(id: Int) = Action { implicit request =>

    val edit: ArticleFormModel = articleForm.bindFromRequest.get
    articleDao.edit(Article(id, edit.shortName, edit.fullName, edit.text, edit.chapterId))
    Redirect(routes.Application.index)
  }

  val chapterForm: Form[ChapterFormModel] = Form(
    mapping(
      "shortName" -> text,
      "fullName" -> text,
      "text" -> text,
      "parentId" -> number
    )(ChapterFormModel.apply)(ChapterFormModel.unapply)
  )

  val articleForm: Form[ArticleFormModel] = Form(
    mapping(
      "shortName" -> text,
      "fullName" -> text,
      "text" -> text,
      "chapterId" -> number
    )(ArticleFormModel.apply)(ArticleFormModel.unapply)
  )


  private def trimToOption(str: String): Option[String] = {
    val trimed = str.trim
    if (trimed.isEmpty) None else Some(trimed)
  }

  def insertChapter = Action.async { implicit request =>
    val chapter: ChapterFormModel = chapterForm.bindFromRequest.get
    chapterDao.insert(
      Chapter(1, chapter.shortName,
        chapter.fullName,
        chapter.text,
        if (chapter.parentId == -1) None else Some(chapter.parentId))).map(_ => Redirect(routes.Application.index))
  }

  def insertArticle =
    Action.async { implicit request =>
      val article: ArticleFormModel = articleForm.bindFromRequest.get
      articleDao.insert(Article(1, article.shortName, article.fullName, article.text,
        article.chapterId)).map(_ => Redirect(routes.Application.index))
    }

  val chapters: Seq[Chapter] = getChaptersFromdb()
  val articles: Seq[Article] = getArticlesFromdb()

  //TODO убрать эвейты
  def getArticlesFromdb(): Seq[Article] = {
    val articlesF: Future[Seq[Article]] = articleDao.all()
    Await.result(articlesF, 1.second).sortBy(_.id)
  }

  def getChaptersFromdb(): Seq[Chapter] = {
    val chaptersF: Future[Seq[Chapter]] = chapterDao.all()
    Await.result(chaptersF, 1.second).sortBy(_.id)
  }
}


case class ChapterFormModel(shortName: String, fullName: String, text: String, parentId: Int)

case class ArticleFormModel(shortName: String, fullName: String, text: String, chapterId: Int)

case class ModifyForm(shortName: String, fullName: String, text: String)

case class TreeChapter(chapter: Chapter) {

  private var children: Seq[TreeChapter] = Seq.empty
  private var childrenWithIndex: Seq[(TreeChapter, Int)] = Seq.empty

  def setChildren(chapters: Seq[Chapter]): Unit = {
    if (chapter != null)
      children = chapterToTreeChapter(chapters.filter(_.parentId.getOrElse(0) == chapter.id))
    else children = chapterToTreeChapter(chapters.filter(_.parentId.getOrElse(None) == None))
    childrenToChildrenWithIndex()
    for (c <- children) {
      c.setChildren(chapters)
    }
  }

  def childrenToChildrenWithIndex(): Unit = {
    for (c <- children) {
      childrenWithIndex = children.zipWithIndex
    }
  }

  def getChildrenWithIndex(): Seq[(TreeChapter, Int)] = childrenWithIndex

  def getChapter(): Chapter = chapter

  def getChildren(): Seq[TreeChapter] = children

  def chapterToTreeChapter(chapters: Seq[Chapter]): Seq[TreeChapter] = {

    for (c <- chapters) {
      children :+= TreeChapter(c)
    }
    children
  }

}

