package base.controllers

import base.models.ModelBase
import play.api.libs.json._

import scala.collection.JavaConversions._


object ParamsScala {

  def readFormFromRequest(m: ModelBase)(implicit request: play.api.mvc.Request[_]) = {
    //TODO make a FormView
  }

  private def getParams()(implicit request: play.api.mvc.Request[_]):java.util.Map[String, Array[String]] =  {
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

  private   def fromJson(prefix: String = "", js: JsValue): Map[String, String] = js match {
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
