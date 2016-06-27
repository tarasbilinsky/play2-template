package base

import java.util.Date

import base.models.{Lookup, ModelBase}
import base.models.enums.AlignType
import scala.language.implicitConversions

package object viewHelpers {
  object ClassAdditions {
    implicit def classAdditions(x: Class[_]): EnhancedClassOf = new EnhancedClassOf(x)

    class EnhancedClassOf(x: Class[_]) {
      def isOfType(cls:Class[_]):Boolean = cls.isAssignableFrom(x)
      def isBoolean = isOfType(classOf[Boolean]) || isOfType(classOf[java.lang.Boolean])
      def isModelBase = isOfType(classOf[ModelBase])
      def isLookup = isOfType(classOf[Lookup])
      def isLong = isOfType(classOf[Long]) || isOfType(classOf[java.lang.Long])
      def isDate = isOfType(classOf[Date])
      def isInt = isOfType(classOf[java.lang.Integer])
      def isNumber = isOfType(classOf[java.lang.Double]) || isOfType(classOf[java.lang.Float])
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
