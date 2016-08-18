package base

import java.text.{DecimalFormat, NumberFormat, SimpleDateFormat}
import java.util.{Date, TimeZone}

import base.models.annotations.FieldMetaFormat
import base.models.{Lookup, ModelBase}
import base.models.enums.{AlignType, FormatType}
import play.api.Play
import play.data.format.Formats.DateFormatter

import scala.language.implicitConversions
import scala.reflect.ClassTag

package object viewHelpers {
  object ClassAdditions {
    implicit def classAdditions(x: Class[_]): EnhancedClassOf = new EnhancedClassOf(x)

    class EnhancedClassOf(x: Class[_]) {
      @inline def isOfType[T](implicit cls: ClassTag[T]):Boolean = cls.runtimeClass.isAssignableFrom(x)
      def isBoolean = isOfType[Boolean] || isOfType[java.lang.Boolean] || x == java.lang.Boolean.TYPE
      def isModelBase = isOfType[ModelBase]
      def isLookup = isOfType[Lookup]
      def isLong = isOfType[Long] || isOfType[java.lang.Long] || x == java.lang.Long.TYPE
      def isDate = isOfType[Date]
      def isInt = isOfType[java.lang.Integer] || x == java.lang.Integer.TYPE
      def isNumber = isOfType[java.lang.Double] || isOfType[java.lang.Float] || x == java.lang.Double.TYPE || x == java.lang.Float.TYPE
      def isString = isOfType[String]
    }
  }
  import ClassAdditions._

  val defaultTimeZone: TimeZone = TimeZone.getTimeZone("EST") //TODO dependency injection TimeZone.getTimeZone(Play.configuration.getString("timezone").get)

  def formatField(model: ModelBase, field: java.lang.reflect.Field, formatType: FormatType, format: String):(String,AlignType) = {
    val fieldName = field.getName
    def getAs[T]:T = model.get(fieldName).asInstanceOf[T]
    def getAsAny[T](cls: Class[T]):T = model.get(fieldName).asInstanceOf[T]
    def protectNull(a: Any, fmt: Any=>String):String = Option(a).fold("")(fmt(_))
    field.getType match {
      case x if x.isBoolean => (if(getAs[Boolean]) "Yes" else "No",AlignType.Center)
      case x if x.isDate => {
        val f = new SimpleDateFormat(format)
        f.setTimeZone(defaultTimeZone)
        val v = protectNull(getAs[Date],f.format)
        (v,AlignType.Right)
      }
      case x if x.isNumber => {
        val f = new DecimalFormat(format)
        def formatInner(x: Any):String = {
          val v1 = f.format(x)
          v1
        }
        val v = protectNull(getAsAny(x),formatInner)
        (v,AlignType.Right)
      }

      case x if x.isInt || x.isLong => ( protectNull(getAs[Any],_.toString),AlignType.Right)

      case _ => (protectNull(getAs[Any],_.toString),AlignType.Left)
    }
  }

}
