package base

import java.util.Date

import base.models.{Lookup, ModelBase}
import base.models.enums.AlignType
import scala.language.implicitConversions

package object viewHelpers {
  object ClassAdditions {
    implicit def classAdditions(x: Class[_]): EnhancedClassOf = new EnhancedClassOf(x)

    class EnhancedClassOf(x: Class[_]) {
      def isBoolean = classOf[Boolean].isAssignableFrom(x) || classOf[java.lang.Boolean].isAssignableFrom(x)
      def isModelBase = classOf[ModelBase].isAssignableFrom(x)
      def isLookup = classOf[Lookup].isAssignableFrom(x)
      def isLong = classOf[Long].isAssignableFrom(x) || classOf[java.lang.Long].isAssignableFrom(x)
      def isDate = classOf[Date].isAssignableFrom(x)
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
