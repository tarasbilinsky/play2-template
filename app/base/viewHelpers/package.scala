package base

import java.util.Date

import base.models.{Lookup, ModelBase}
import base.models.enums.AlignType

import scala.language.implicitConversions
import scala.reflect.ClassTag

package object viewHelpers {
  object ClassAdditions {
    implicit def classAdditions(x: Class[_]): EnhancedClassOf = new EnhancedClassOf(x)

    class EnhancedClassOf(x: Class[_]) {
      def isOfType[T](implicit cls: ClassTag[T]):Boolean = cls.runtimeClass.isAssignableFrom(x)
      def isBoolean = isOfType[Boolean]|| isOfType[java.lang.Boolean]
      def isModelBase = isOfType[ModelBase]
      def isLookup = isOfType[Lookup]
      def isLong = isOfType[Long] || isOfType[java.lang.Long]
      def isDate = isOfType[Date]
      def isInt = isOfType[java.lang.Integer]
      def isNumber = isOfType[java.lang.Double] || isOfType[java.lang.Float]
    }
  }
  import ClassAdditions._
  def formatField(model: ModelBase, field: java.lang.reflect.Field):(String,AlignType) = {
    val fieldName = field.getName
    def getAs[T]:T = model.get(fieldName).asInstanceOf[T]
    field.getType match {
      case x if x.isBoolean => (if(getAs[Boolean]) "Yes" else "No",AlignType.Center)
      case x if x.isDate => (???,AlignType.Right)
        //TODO
    }
  }

}
