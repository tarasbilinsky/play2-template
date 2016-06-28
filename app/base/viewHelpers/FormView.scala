package base.viewHelpers

import scala.language.implicitConversions
import java.util
import javax.persistence.{Lob, ManyToMany}

import base.models.{Lookup, ModelBase}
import base.models.annotations.{FieldMeta, FieldMetaFormat, FieldMetaOptionsSource}
import base.utils
import base.utils.Titles
import play.api.libs.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsUndefined, JsValue}
import net.oltiv.scalaebean.Shortcuts._

import scala.collection.mutable
import FormFieldType._
import base.models.enums.{AlignType, FormatType}
import com.avaje.ebean.{Ebean, Expr, Expression, Model, Query}
import models.ModelPlaceholders._
import ClassAdditions._

case class FieldValueId(val id: Any){require(id!=null)}

class FieldOption(val id: FieldValueId, val title: String){require(title!=null)}

case class Field( val name: String,
                  var title: Option[String] = None,
                  var fieldType: Option[FormFieldType] = None,
                  var value: Option[String] = None,
                  var valueId: Option[FieldValueId] = None,
                  var options: Option[Seq[FieldOption]] = None,
                  var align: Option[AlignType] = None,
                  var formatType: Option[FormatType] = None,
                  var format: Option[String] = None,
                  var hint: Option[String] = None,
                  val extra: Option[Map[String,String]] = None
                ){
  require(name!=null)
  override def hashCode(): Int = name.hashCode
  override def equals(other: Any): Boolean = other match {
    case o: Field => o.name == this.name
    case _ => false
  }
  protected def evalIfNone[A](p: =>Option[A], f: =>A):A = {
    p match {
      case Some(pp) => pp
      case None => val pp = f; pp
    }

  }

  def getTitle:String = evalIfNone(title,{val newTitle = Titles.camelCaseToTitle(name); title = Some(newTitle); newTitle})
  def getFieldType:FormFieldType = fieldType.getOrElse(FormFieldType.TextInput)
  def getValue:String = value.getOrElse("")
  def getValueId:FieldValueId = valueId.getOrElse(FieldValueId(""))
  def getOptions:Seq[FieldOption] = options.getOrElse(Nil)

  def getAlign:AlignType = align.getOrElse(AlignType.Left)

  def getFormatType:FormatType = formatType.getOrElse(FormatType.Text)
  def getFormat:String = format.getOrElse("")

  def isOptionSelected(o: FieldOption):Boolean = o.id==getValueId

  def getHint:String = hint.getOrElse("")
  def getExtra:Map[String,String] = extra.getOrElse(Map.empty)
}

class BoundField(name: String, val model: ModelBase, val modelField: java.lang.reflect.Field) extends Field(name) {
  require(model!=null && name!=null && modelField!=null)

  private val modelFieldType = modelField.getType
  private lazy val fieldMeta = Option(modelField.getAnnotation(classOf[FieldMeta]))
  private lazy val fieldMetaOptionsSource = Option(modelField.getAnnotation(classOf[FieldMetaOptionsSource]))
  private lazy val fieldMetaFormat = Option(modelField.getAnnotation(classOf[FieldMetaFormat]))

  override def getTitle:String = title.getOrElse(fieldMeta.fold(super.getTitle){
    m=>
      val metaTitle = m.title()
      if(metaTitle.isEmpty) super.getTitle else {
        title = Some(metaTitle)
        metaTitle
      }

  })

  override def getFieldType:FormFieldType = {
    def getFieldTypeFromType:FormFieldType = {
      modelField.getType match {
        case x if x.isModelBase => SelectBox
        case x if x.isBoolean => RadioButtons
        case x if x.isEnum => RadioButtons
        case _ if Option(modelField.getAnnotation(classOf[ManyToMany])).isDefined => Checkboxes
        case _ => Option(modelField.getAnnotation(classOf[Lob])).fold(TextInput){_=>TextArea}
      }
    }
    fieldType.getOrElse(fieldMeta.fold {
      getFieldTypeFromType
    } {
      m =>
        val t = m.formFieldType()
        val t2 = if (t == FormFieldType.NotDefined) getFieldTypeFromType else t
        fieldType = Some(t2)
        t2
    })
  }

  private def valueAndAlignLoad = {
    val (v,a) = formatField(model,modelField,getFormatType,getFormat)
    value = Some(v); align = Some(a)
    (v,a)
  }

  override def getValue:String = evalIfNone(value, valueAndAlignLoad._1)

  override def getValueId:FieldValueId = evalIfNone(valueId, {val v = FieldValueId(model.get(name)); valueId = Some(v); v})

  override def getAlign:AlignType = evalIfNone(align,valueAndAlignLoad._2)

  import FormatType._
  private def formatTypeLoad: FormatType = modelFieldType match {
    case x if x.isDate => DateTime
    case x if x.isInt => Integer
    case x if x.isNumber => Number
    case x if Option(modelField.getAnnotation(classOf[Lob])).isDefined => TextMultiline
    case _ => Text
  }

  private def formatLoad: String = getFormatType match {
    case DateTime => "MM/dd/yyyy hh:mmaa"//TODO
    case Number => "###,###,###,###.00"//TODO
    case Integer => "d"//TODO
    case _ => ""
  }

  override def getFormatType:FormatType = evalIfNone(
    formatType,
    {
      val v = fieldMetaFormat.fold
      {formatTypeLoad}
      {m => val t = m.`type`; if(t == FormatType.Undefined) formatTypeLoad else t};formatType = Some(v);v}
  )

  override def getFormat:String = evalIfNone(
    format,
    {
      val v = fieldMetaFormat.fold {formatLoad}{vv => val vf = vv.format(); if(vf.isEmpty) formatLoad else vf}
      format = Some(v)
      v
    }
  )

  override def getOptions: Seq[FieldOption] = evalIfNone(options,{
    val o: Seq[FieldOption] = modelFieldType match {
      case x if x.isLookup => {
        val q = query(x,classOf[Lookup])
          .select(props(lookup,lookup.id,lookup.title))
          .where()
          .eq(props(lookup,lookup.active),true)
          .orderBy(props(lookup,lookup.orderNumber))
        q.seq.map{mm => new FieldOption(FieldValueId(mm.id),mm.title)}
      }
      case x if x.isEnum => x.getDeclaredFields.filter(y => y.getType == x).zipWithIndex.map{ case (f,id) =>
        def defaultTitle = Titles.camelCaseToTitle(f.getName)
        val title: String = Option(f.getAnnotation(classOf[FieldMeta])).fold(defaultTitle){m =>
          val t = m.title()
          if(t.isEmpty) defaultTitle else t
        }
        new FieldOption(FieldValueId(id-1), title)
      }
      case x if x.isBoolean => {
        Seq(new FieldOption(FieldValueId(1), "Yes"), new FieldOption(FieldValueId(0), "No"))
      }
      case x if x.isLong || x.isModelBase && fieldMetaOptionsSource.isDefined => {
        fieldMetaOptionsSource.map {
          case m if !m.rawSql().isEmpty => query(fieldMetaOptionsSource.get.rawSql()).seq.map { o => new FieldOption(FieldValueId(o.getLong("id")), o.getString("name")) }
          case m if m.model() != classOf[ModelBase] =>
            val filter: Expression = if (m.activeOnly()) Expr.eq("active", true) else if (!m.rawFilter().isEmpty) Expr.raw(m.rawFilter()) else Expr.raw("1=1")
            query(m.model(), classOf[ModelBase]).select(s"${m.titleColumn},id").having(filter).orderBy(m.orderColumn() + (if (m.orderDescending()) " desc" else "asc"))
              .seq.map { o => new FieldOption(FieldValueId(o.id), o.get(m.titleColumn()).toString) }
        }.get
      }

      case _ => Nil

    }
    options = Some(o)
    o
  })

  override def getHint: String = hint.getOrElse(fieldMeta.fold(super.getHint){_.hint()})

  override def getExtra: Map[String, String] = super.getExtra //TODO-LATER maybe some extra from meta

}


object  Field{
  private [viewHelpers] def apply(modelOption: Option[ModelBase], f: String):Field = {
    modelOption.fold{
      Field(f)
    }
    { model =>
      val mf = model.getClass.getDeclaredField(f)
      Field(model,mf)
    }
  }
  //private [viewHelpers]
  def apply(model: ModelBase, mf: java.lang.reflect.Field):BoundField = {
    val f = mf.getName
    new BoundField(f, model, mf)
  }
}


class FormView [+T >: ModelBase] private [this] (val name: Option[String], model: Option[T] = None, fields: mutable.LinkedHashMap[String,Field] = mutable.LinkedHashMap()) {

  def this(model: T, name: String, propertiesList: String) = {
    this(utils.emptyStringToNone(name),Some(model))
    for(p <- propertiesList.split(","); if !p.isEmpty) this + Field(p)
  }

  def this(model: T,ff: Seq[Field], name: String) = {
    this(model,name,"")
    this ++ ff
  }

  def this (ff: Seq[Field], name: String) = {
    this(Some(name))
    this ++ ff
  }

  def loadFromParams(implicit request: play.api.mvc.Request[_]):T = ???
  def get(fieldName: String):Option[Field] = fields.get(fieldName)

  private [this] def +(f: Field):FormView[T] = {fields.put(f.name,f); this}
  private [this] def +(f: String):FormView[T] = {fields.put(f,Field(model.asInstanceOf[Option[ModelBase]],f)); this}

  private [this] def +(ff: Seq[String]):FormView[T] = {ff.foreach(this+_);this}
  private [this] def ++(ff: Seq[Field]):FormView[T] = {ff.foreach(this+_);this}

}

object FormView{

  def loadFromParams[T](cls: Class[T], name: String = "")(implicit request: play.api.mvc.Request[_]):T = ???

  private def getParams(request: play.api.mvc.Request[_]):Map[String, Array[String]] =  {
    val a = ((request.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
      case body: play.api.mvc.AnyContent if body.asJson.isDefined => fromJson(js = body.asJson.get).mapValues(Seq(_))
      case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
      case body: play.api.mvc.MultipartFormData[_] => body.asFormUrlEncoded
      case body: play.api.libs.json.JsValue => fromJson(js = body).mapValues(Seq(_))
      case _ => Map.empty[String, Seq[String]]
    }) ++ request.queryString).mapValues(s=>s.toArray)

    //val b = collection.mutable.Map(a.toSeq : _*)

    a
  }

  private def fromJson(prefix: String = "", js: JsValue): Map[String, String] = js match {
    case JsObject(fields) => {
      fields.map { case (key, value) => fromJson(Option(prefix).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + key, value) }.foldLeft(Map.empty[String, String])(_ ++ _)
    }
    case JsArray(values) => {
      values.zipWithIndex.map { case (value, i) => fromJson(prefix + "[" + i + "]", value) }.foldLeft(Map.empty[String, String])(_ ++ _)
    }
    case JsNull => Map.empty
    case JsUndefined() => Map.empty
    case JsBoolean(value) => Map(prefix -> value.toString)
    case JsNumber(value) => Map(prefix -> value.toString)
    case JsString(value) => Map(prefix -> value.toString)
  }
}
