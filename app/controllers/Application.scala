package controllers

import javax.inject.Inject

import dao.{ArticleDAO, ChapterDAO}
import models.{Article, Chapter, Entity}
import play.api.data.Form
import play.api.data.Forms.{mapping, number, text}
import play.api.i18n.{I18nSupport, Messages}
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
      val messages: Messages = request.messages
      val message: String = messages("info.error")
    articleDao.all().zip(chapterDao.all()).map { case (articles, chapters) => {


        pathList.clear()
        pathMap.clear()
        numbersList.clear()
        treeRoots.clear()
        getFromdb()
        setTreeNodes(null, 1)
        getAllPath()


      Ok(views.html.index(articleForm, chapterForm, pathList, pathMap))} }
  }

  def info(s: String) = Action{implicit request =>
    val messages: Messages = request.messages
    val fullName: String = pathMap.get(s).orNull.getFullName
    val text: String = pathMap.getOrElse(s, null).getText
    val children: mutable.MutableList[String] = pathMap.getOrElse(s, null).getChildrenList
    Redirect(routes.Application.info(s))
        Ok(views.html.info(s,fullName,text, children, modifyForm))
  }

  def delete(s: String) = Action{

        if (pathMap.get(s).orNull.getType == "chapter") chapterDao.delete(pathMap.get(s).orNull.getId)
        else if (pathMap.get(s).orNull.getType == "article") articleDao.delete(pathMap.get(s).orNull.getId)

    Redirect(routes.Application.index)
  }

  def modify(s: String) = Action{implicit request =>

    val mod: ModifyForm = modifyForm.bindFromRequest.get
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



  var chapters: Seq[Chapter] = Seq[Chapter]()
  var articles: Seq[Article] = Seq[Article]()
  var treeRoots: mutable.MutableList[TreeNode] = new mutable.MutableList[TreeNode]()
  var numbersList: mutable.MutableList[String] = new mutable.MutableList[String]()
  var pathList: mutable.MutableList[String] = new mutable.MutableList[String]()
  var pathMap: mutable.HashMap[String, Entity] = new mutable.HashMap[String, Entity]()
  var flag: Boolean = false

  def getFromdb(): Unit = {

      val chaptersF: Future[Seq[Chapter]] = chapterDao.all()
      val articlesF: Future[Seq[Article]] = articleDao.all()
      chapters = Await.result(chaptersF, 1.second).sortBy(_.id)
      articles = Await.result(articlesF, 1.second).sortBy(_.id)

    }



  def setTreeNodes (treeNode: TreeNode, count: Int): Unit ={
    var bufferNode: TreeNode = null
    var i: Int = 1

    for(c <- chapters) {
      if (treeNode == null && c.parentId.isEmpty) {
        bufferNode = new TreeNode(c, null, i, new mutable.MutableList[TreeNode], new mutable.MutableList[Article])
        setArticles(bufferNode)
        treeRoots.+=:(bufferNode)
        setTreeNodes(bufferNode, 1)
        i+=1
      } else if (treeNode!=null && (treeNode.getData().id == c.parentId.getOrElse(0))){
        bufferNode = new TreeNode(c, treeNode, i, new mutable.MutableList[TreeNode], new mutable.MutableList[Article])
        treeNode.addChild(bufferNode)
        setArticles(bufferNode)
        setTreeNodes(bufferNode, 1)
        i+=1
      }
    }
  }

  def setArticles(treeNode: TreeNode): Unit = {
    for (a <- articles){
      if(a.chapterId == treeNode.getData().id) {
        treeNode.addArticle(a)
      }
    }
  }

  def getPathList(n: TreeNode, path: String, number: String): Unit = {


    var str: String = ""
    var num: String = ""
    if (number == "") num = n.getChapterNumber().toString + "."
    else num = number + n.getChapterNumber().toString + "."
    str = path + "/" + num + n.getData().shortName

    for(c <- n.getChildren()){
      getPathList(c, str, num)
    }

    for(a <- n.getArticles()){
      pathList.+=:(str + "/" + a.shortName)
      pathMap.put(str + "/" + a.shortName, a)
      numbersList.+=:("art:")
    }
    pathList.+=:(str)
    pathMap.put(str, n)
    numbersList.+=:(num)
  }

  def getAllPath(): Unit = {
    for (root <- treeRoots){
      getPathList(root, "", "")
    }
  }


}

case class ChapterFormModel(shortName: String, fullName: String, text: String, parentId: Int)

object ChapterFormModel{}

case class ArticleFormModel(shortName: String, fullName: String, text: String, chapterId: Int)

object ArticleFormModel{}

case class ModifyForm(shortName: String, fullName: String, text: String)

object ModifyForm

case class TreeNode (data: Chapter, parent: TreeNode, chapterNumber: Int, children: mutable.MutableList[TreeNode], articleList: mutable.MutableList[Article]) extends Entity{
  def addChild(treeNode: TreeNode): Unit ={
    children.+=:(treeNode)
  }
  def addArticle(article: Article): Unit ={
    articleList.+=:(article)
  }
  def getData(): Chapter= {
    data
  }
  def getChildren(): mutable.MutableList[TreeNode] = {
    this.children
  }

  def getChapterNumber(): Int = {
    this.chapterNumber
  }

  def getArticles(): mutable.MutableList[Article] = {
    this.articleList
  }

  def getFullName(): String = {
    data.getFullName
  }

  override def getChildrenList: mutable.MutableList[String] = {
    val list: mutable.MutableList[String] = new mutable.MutableList[String]()
    for (c <- children){
      list.+=:(c.toString)
    }

    for (a <- articleList){
      list.+=:(a.toString)
    }

    list

  }
  override def toString: String = data.toString

  override def getText: String = data.text

  def getId: Int = data.getId

  def getType: String = data.getType
}

