package base.controllers

import base.models.ModelBase
import base.utils
import net.oltiv.scalaebean.EbeanImplicits._
import net.oltiv.scalaebean.EbeanShortcuts._
import play.api.mvc.Action
//import net.oltiv.playBase.views.html.crud.edit



import com.avaje.ebean.{Expression, Query}

import scala.language.reflectiveCalls
import scala.reflect.runtime.universe._


abstract class ControllerCrud[T<:ModelBase](
                                             m: T,
                                             columns: String = "",
                                             val indexRoute: play.api.mvc.Call,
                                             nameColumn: String = "name"
                                             )(implicit env: EnvironmentAll, tag: WeakTypeTag[T])
  extends ControllerBase
{
  lazy val qe: Expression = com.avaje.ebean.Expr.ne("id",null)
  protected val title = utils.Titles.camelCaseToTitle(tag.tpe.typeSymbol.asClass.name.toString)
  protected def queryById(id: Long):Query[T]  = query(mClass,m,m.id==id)
  protected val tableColumns = columns
  protected val formColumns = columns
  protected def postSave(m: T):(Boolean,String) = (true,"")
  protected def preSave(m: T):(Boolean,String) = (true,"")
  protected def preDelete(m: T):(Boolean,String) = (true,"")

  protected val listHasEditLink = true
  protected val listHasDeleteLink = true
  protected val listHasAddNewLink = true
  protected val listMoreAdditionalColumns:List[(String,String,Option[String])] = Nil


  def index = Action { implicit request =>
    //val t = new TableView(Ebean.createQuery(mClass).where(qe))
    //t.addColumns(tableColumns)
    //Ok(net.oltiv.playBase.views.html.crud.table(title,t,listHasDeleteLink,listHasAddNewLink,listHasEditLink,listMoreAdditionalColumns))
    Ok
  }

  /*
  val editView: {def apply(title: String, f: FormView, id: Long, isNew: Boolean, name: => String)(implicit env: EnvironmentAll, request: RequestWithAttributes[AnyContent]):play.twirl.api.HtmlFormat.Appendable} = net.oltiv.playBase.views.html.crud.edit
  def edit(id: Long) = StackAction { implicit request =>
    val m = queryByIdInner(id)
    val f = new FormView(m).addFormFields(formColumns)
    Ok(editView(title,f,id,id==ModelBase.NewModelId,m.get(nameColumn).toString))
  }

  def add = edit(ModelBase.NewModelId)
  */

  def save(id: Long) = Action { implicit request =>
    val m = queryByIdInner(id)
    //readFromRequest(m)
    val (s,msg) = preSave(m)
    if(!s){
      Redirect(indexRoute).flashing("error" -> s"$title id# $id ${m.get(nameColumn)} cannot be updated. $msg")
    } else {
      m.save
      val (s,msg) = postSave(m)
      Redirect(indexRoute).flashing("message" -> s"$title id# $id ${m.get(nameColumn)} has been updated", "error"->msg)
    }
  }

  def delete(id: Long) = Action {implicit request =>
    val m = queryByIdInner(id)
    preDelete(m)
    m.delete
    Redirect(indexRoute).flashing("message" -> s"$title id# $id ${m.get(nameColumn)} has been deleted")
  }



  private def queryByIdInner(id: Long):T = if(id==ModelBase.NewModelId) ModelBase.newInstance(m.getClass).asInstanceOf[T] else queryById(id).one.get

  private val mClass: Class[T] = tag.mirror.runtimeClass(tag.tpe).asInstanceOf[Class[T]]

}
