package base.viewHelpers

import java.lang.reflect.{Method, Modifier}

import scala.language.implicitConversions
import java.util
import javax.persistence.{Lob, ManyToMany}

import base.models.{Lookup, ModelBase}
import base.models.annotations.{FieldMeta, FieldMetaFormat, FieldMetaOptionsSource}
import base.utils
import base.utils.{Enums, Titles}
import play.api.libs.json._
import net.oltiv.scalaebean.Shortcuts._

import scala.collection.mutable
import FormFieldType._
import base.models.enums.{AlignType, FormatType}
import com.avaje.ebean.{common, _}
import models.ModelPlaceholders._
import ClassAdditions._
import com.avaje.ebean.bean.BeanCollection.ModifyListenMode
import com.avaje.ebean.common.BeanSet
import models.{ModelPlaceholders => PH}

import scala.collection.JavaConversions._
import scala.util.Try

case class FieldValue(val value: Any){
  require(value!=null)
  def id:String = {
    value match {
      case value:ModelBase => value.id.toString
      case value:Enum[_] => value.ordinal.toString
      case value:Boolean => if(value) "1" else "0"
      case _ => value.toString
    }
  }
}

class FieldOption(val v: FieldValue, val title: String){
  require(title!=null && v!=null)
  def isSelectedFor(value: Any):Boolean = {
    value match {
      case value: BeanSet[_] => value.exists(_==v.value)
      case _ => value==v.value
    }
  }
}

case class Field(val name: String,
                 var title: Option[String] = None,
                 var fieldType: Option[FormFieldType] = None,
                 var valueView: Option[String] = None,
                 var value: Option[FieldValue] = None,
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
  def getValueView:String = valueView.getOrElse("")
  def getValue:FieldValue = value.getOrElse(FieldValue(""))
  def getOptions:Seq[FieldOption] = options.getOrElse(Nil)

  def getAlign:AlignType = align.getOrElse(AlignType.Left)

  def getFormatType:FormatType = formatType.getOrElse(FormatType.Text)
  def getFormat:String = format.getOrElse("")

  def getHint:String = hint.getOrElse("")
  def getExtra:Map[String,String] = extra.getOrElse(Map.empty)
}

class BoundField(name: String, val model: ModelBase, val modelField: java.lang.reflect.Field) extends Field(name) {
  require(model!=null && name!=null && modelField!=null)

  private val modelFieldType = modelField.getType
  private lazy val fieldMeta = Option(modelField.getAnnotation(classOf[FieldMeta]))
  private lazy val fieldMetaOptionsSource = Option(modelField.getAnnotation(classOf[FieldMetaOptionsSource]))
  private lazy val fieldMetaFormat = Option(modelField.getAnnotation(classOf[FieldMetaFormat]))
  private lazy val fieldLob = Option(modelField.getAnnotation(classOf[Lob]))
  private lazy val fieldManyToMany = Option(modelField.getAnnotation(classOf[ManyToMany]))

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
        case _ if Option(modelField.getAnnotation(classOf[javax.persistence.Id])).isDefined => Hidden
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
    valueView = Some(v); align = Some(a)
    (v,a)
  }

  override def getValueView:String = evalIfNone(valueView, valueAndAlignLoad._1)

  override def getValue:FieldValue = evalIfNone(value, {val v = FieldValue(model.get(name)); value = Some(v); v})

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
    case DateTime => "MM/dd/yyyy hh:mmaa"
    case Number => "###,###,###,###.##"
    case Money =>  "¤###,###,###,###.##"
    case Integer => ""
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
    def fromLookup[T](m: Class[T]) = {
      val q = query(m,classOf[Lookup])
        .select(props(lookup,lookup.id,lookup.title))
        .where()
        .eq(props(lookup,lookup.active),true)
        .orderBy(props(lookup,lookup.orderNumber))
      q.seq.map{mm => new FieldOption(FieldValue(mm),mm.title)}
    }
    def fromMeta[T](m: Class[T]) = {
      fieldMetaOptionsSource.map {
        case m if !m.rawSql().isEmpty => query(fieldMetaOptionsSource.get.rawSql()).seq.map { o => new FieldOption(FieldValue(o.getLong("id")), o.getString("name")) }
        case m if m.model() != classOf[ModelBase] =>
          val filter: Expression = if (m.activeOnly()) Expr.eq("active", true) else if (!m.rawFilter().isEmpty) Expr.raw(m.rawFilter()) else Expr.raw("1=1")
          query(m.model(), classOf[ModelBase]).select(s"${m.titleColumn},id").having(filter).orderBy(m.orderColumn() + (if (m.orderDescending()) " desc" else "asc"))
            .seq.map { o => new FieldOption(FieldValue(o), o.get(m.titleColumn()).toString) }
      }.getOrElse(Nil)
    }
    val o: Seq[FieldOption] = modelFieldType match {
      case x if x.isLookup && fieldMetaOptionsSource.isEmpty => fromLookup(x)
      case x if x.isEnum =>
        Enums.values(x).map{ e:Enum[_] =>
          val enumName = e.name()
          val title: String = Option(x.getDeclaredField(enumName).getAnnotation(classOf[FieldMeta]))
            .fold(Titles.camelCaseToTitle(enumName))(_.title())
          new FieldOption(FieldValue(e), title)
        }
      case x if x.isBoolean => {
        Seq(new FieldOption(FieldValue(true), "Yes"), new FieldOption(FieldValue(false), "No"))
      }
      case x if (x.isLong || x.isModelBase) && fieldMetaOptionsSource.isDefined => fromMeta(x)
      case x if fieldManyToMany.isDefined => {
        val p = "^.*<(.*)>$".r
        val lookupManyToMany = modelField.getGenericType.toString match {
          case p(cls) => {
            val clazz = Class.forName(cls)
            clazz match {
              case x if x.isLookup => fromLookup(x)
              case x if x.isModelBase && fieldMetaOptionsSource.isDefined => fromMeta(x)
              case _ => Nil
            }
          }
          case _ => Nil
        }
        lookupManyToMany
      }
      case _ => Nil

    }
    options = Some(o)
    o
  })

  override def getHint: String = hint.getOrElse(fieldMeta.fold(super.getHint){_.hint()})

  override def getExtra: Map[String, String] = super.getExtra //TODO-LATER maybe some extra from meta

}


object  Field {
  private[viewHelpers] def apply(modelOption: Option[ModelBase], f: String): Field = {
    modelOption.fold {
      Field(f)
    } { model =>
      val mf = model.getClass.getField(f)
      Field(model, mf)
    }
  }

  def apply(model: ModelBase, mf: java.lang.reflect.Field): BoundField = {
    val f = mf.getName
    new BoundField(f, model, mf)
  }

  def apply(model: ModelBase, f: String): BoundField = apply(model, model.getClass.getDeclaredField(f))
}

class FormView [+T <: ModelBase] private [this] (val name: Option[String], model: Option[T] = None, val fields: mutable.LinkedHashMap[String,Field] = mutable.LinkedHashMap()) {

  val fieldNamePrefix = name.fold("")(_+"_")

  def this(model: T, name: String, propertiesList: String) = {
    this(utils.emptyStringToNone(name),Some(model))
    for(p <- propertiesList.split(","); if !p.isEmpty) this + Field(Some(model),p)
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

  def getAll() = fields.values

  private [this] def +(f: Field):FormView[T] = {fields.put(f.name,f); this}
  private [this] def +(f: String):FormView[T] = {fields.put(f,Field(model.asInstanceOf[Option[ModelBase]],f)); this}

  private [this] def +(ff: Seq[String]):FormView[T] = {ff.foreach(this+_);this}
  private [this] def ++(ff: Seq[Field]):FormView[T] = {ff.foreach(this+_);this}

}

object FormView{

  def loadFromParams[T<:ModelBase](cls: Class[T], name: String = "")(implicit request: play.api.mvc.Request[_]):T = {
    val params = getParams(request)
    val namePrefix = if(name=="") "" else s"${name}_"
    val fields = cls.getFields.filter(f => !Modifier.isStatic(f.getModifiers))
    val idField:java.lang.reflect.Field=>Option[Any] = f=>Option(f.getAnnotation(classOf[javax.persistence.Id]))

    val id = fields.find(idField(_).isDefined)

    val idValue:Option[Long] = id.flatMap{idField =>
      val n = namePrefix+idField.getName
      val p = params.get(n)
      p.flatMap{x:Array[String] =>
        if(x.isEmpty) None else
        Try(x(0).toLong).toOption
      }
    }

    val m: T = idValue.fold(cls.newInstance()){id =>
      Ebean.createQuery(cls).where().idEq(id).findUnique()
    }
    require(m!=null)

    val unsavedManyToMany: mutable.MutableList[String] = new mutable.MutableList[String]

    fields
      .filter(idField(_).isEmpty)
      .foreach{f =>
        val name = f.getName
        def set(x: Any) = m.set(name,x)
        val value:Option[Array[String]] = params.get(namePrefix+name)
        value.foreach{vv =>
          if(vv.size>0) {
            val v = vv(0)
            f.getType match {
              case x if x.isLong => Try(v.toLong).foreach(vL => set(vL))
              case x if x.isEnum => Try(v.toInt).foreach(vInt => set(Enums.values(x)(vInt)))
              case x if x.isModelBase => Try(v.toLong).foreach(
                vL => query(x.asInstanceOf[Class[ModelBase]],PH.modelBase,PH.modelBase.id==vL,PH.modelBase.id).one.foreach(set(_))
              )
              case x if x.isString => set(v)
              case x => {
                val setFld = "^java.util.(Set|List)\\<(.*)\\>$".r
                f.getGenericType.getTypeName match {
                  case setFld(_,genericClass) =>
                    val cls = Class.forName(genericClass)
                    cls match{
                      case x if x.isModelBase =>
                        val bs = Option(m.get(name).asInstanceOf[BeanSet[ModelBase]]).getOrElse(new BeanSet[ModelBase]())
                        bs.setModifyListening(ModifyListenMode.ALL)
                        val mm: Seq[ModelBase] = vv.filter(!_.isEmpty).map{ v =>
                          Ebean.getReference(x,v).asInstanceOf[ModelBase]
                        }.filter(mm=>Option(mm).isDefined).toSeq
                        val removed = bs.retainAll(mm)
                        val added = bs.addAll(mm)
                    }
                }
              }
            }
          }
        }
      }
    m
  }

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
