package base.utils

import java.lang.reflect.Method

object Enums {

  def values(x: Class[_]) = {
    val valuesMethod: Method = x.getDeclaredMethod("values")
    val values: Array[Enum[_]] = valuesMethod.invoke(null).asInstanceOf[Array[Enum[_]]]
    values
  }

}
