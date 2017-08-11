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



class Application @Inject() (
                              articleDao: ArticleDAO,
                              chapterDao: ChapterDAO,
                              controllerComponents: ControllerComponents
                            )(implicit executionContext: ExecutionContext) extends AbstractController(controllerComponents) with I18nSupport{

//

  def index = Action.async {implicit request =>
     // val messages: Messages = request.messages
//      val message: String = messages("info.error")
      articleDao.all().zip(chapterDao.all()).map { case (articles, chapters) => {
      getFromdb()
      val treeChapter = TreeChapter(null)
      treeChapter.setChildren(chapters)
//TODO все названия методов, классов, параметров должны быть смысловыми, чтобы прочитав можно было понять про что идет речь, что это за параметр, что будет делать метод, про что этот класс и т.д.
      Ok(views.html.index(articleForm, chapterForm, treeChapter, articles, chapters)) }
  }
  }

  def info(s: String) = Action{implicit request =>

    val fullName: String = pathMap.get(s).orNull.getFullName
    val text: String = pathMap.getOrElse(s, null).getText
    val children: mutable.MutableList[String] = pathMap.getOrElse(s, null).getChildrenList // TODO тут фигня какя-то нужно смотреть
    Ok(views.html.info(s,fullName,text, children, modifyForm))
  }

  //TODO здесь нужно всего лишь Id передавать и удалять по Id, можно сделать два метода deleteChapter deleteArticle

  def deleteArticle(id: Int) = Action{implicit request =>
    articleDao.delete(id)
    Redirect(routes.Application.index)
  }

  def deleteChapter(id: Int) = Action{implicit request =>
    chapterDao.delete(id)
    Redirect(routes.Application.index)
  }

  def edit(s: String) = Action{ implicit request =>

    val mod: ModifyForm = modifyForm.bindFromRequest.get

    //TODO нужно нормально ресолвить форму с паттернматчингом если нужно на конкретный case class, без нулов, без instanceOf
    val instance = pathMap.getOrElse(s, null)
    if (instance.getType == "article")
      articleDao.modify(instance.getId, mod.shortName, mod.fullName, mod.text)
    else if (pathMap.get(s).getOrElse(null).getType == "chapter")
      chapterDao.modify(instance.getId, mod.shortName, mod.fullName, mod.text)

    Redirect(routes.Application.index)
  }

  val chapterForm :Form[ChapterFormModel] = Form(
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

  //TODO Лишняя форма
  val modifyForm: Form[ModifyForm] = Form(
    mapping(
      "shortName" -> text,
      "fullName" -> text,
      "text" -> text,
    )(ModifyForm.apply)(ModifyForm.unapply)
  )


  private def trimToOption(str: String): Option[String] = {
    val trimed =  str.trim
    if(trimed.isEmpty) None else Some(trimed)
  }

  def insertChapter = Action.async{ implicit request =>
    val chapter: ChapterFormModel = chapterForm.bindFromRequest.get
    chapterDao.insert(
      Chapter(1, chapter.shortName,
        chapter.fullName,
        chapter.text,
      if(chapter.parentId == -1) None else Some(chapter.parentId))).map(_ => Redirect(routes.Application.index))
  }

  def insertArticle =
    Action.async{ implicit request =>
    val article: ArticleFormModel = articleForm.bindFromRequest.get
    articleDao.insert(Article(1, article.shortName, article.fullName, article.text,
      article.chapterId)).map(_ => Redirect(routes.Application.index))
  }

  //TODO избавиться от var
  var chapters: Seq[Chapter] = Seq[Chapter]()
  var articles: Seq[Article] = Seq[Article]()
  val numbersList: mutable.MutableList[String] = new mutable.MutableList[String]()
  val pathList: mutable.MutableList[String] = new mutable.MutableList[String]()
  val pathMap: mutable.HashMap[String, TextItem] = new mutable.HashMap[String, TextItem]()


  def getFromdb(): Unit = {

      val chaptersF: Future[Seq[Chapter]] = chapterDao.all()
      val articlesF: Future[Seq[Article]] = articleDao.all()
    //TODO убрать эвейты
      chapters = Await.result(chaptersF, 1.second).sortBy(_.id)
      articles = Await.result(articlesF, 1.second).sortBy(_.id)

    }

}
//TODO такие методы сеттеры с возвращаемым значением unit нужно избегать

case class ChapterFormModel(shortName: String, fullName: String, text: String, parentId: Int)

case class ArticleFormModel(shortName: String, fullName: String, text: String, chapterId: Int)

case class ModifyForm(shortName: String, fullName: String, text: String)
//TODO переделать структуру дерева
case class TreeChapter(chapter: Chapter){

  private var children: Seq[TreeChapter] = Seq.empty
  private var childrenWithIndex: Seq[(TreeChapter, Int)] = Seq.empty

  def setChildren(chapters: Seq[Chapter]): Unit = {
    if (chapter != null)
    children = chapterToTreeChapter(chapters.filter(_.parentId.getOrElse(0) == chapter.id))
    else children = chapterToTreeChapter(chapters.filter(_.parentId.getOrElse(None) == None))
    childrenToChildrenWithIndex()
    for (c <- children){
      c.setChildren(chapters)
    }
  }

  def childrenToChildrenWithIndex(): Unit = {
    for (c <- children){
      childrenWithIndex = children.zipWithIndex
    }
  }

  def getChildrenWithIndex(): Seq[(TreeChapter, Int)] = childrenWithIndex

  def getChapter(): Chapter = chapter

  def getChildren():

  def chapterToTreeChapter(chapters: Seq[Chapter]): Seq[TreeChapter] = {

    for(c <- chapters){
      children :+= TreeChapter(c)
    }
    children
  }

}

