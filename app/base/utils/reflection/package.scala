package base.utils

import scala.reflect.runtime.universe._
import scala.reflect.api.TypeTags

package object reflection {
  def typeName(t: TypeTag[_]) = t.tpe.typeSymbol.name.toString

  lazy val runtimeMirror = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def constructor(t: Type) = t.decl(termNames.CONSTRUCTOR).asMethod

  def classMirror(t: Type) = runtimeMirror.reflectClass(t.typeSymbol.asClass)

  def constructorMethod(t: Type) = classMirror(t).reflectConstructor(constructor(t))
}
