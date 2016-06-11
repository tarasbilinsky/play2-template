package base.viewHelpers

import base.models.ModelBase
import base.models.annotations.ViewMeta
import base.utils.Titles

import scala.collection.mutable

case class FieldValueId(val id: Object)

class FieldOption(val id: FieldValueId, val title: String)


class FormView(private val model: Option[ModelBase]=None) {

  def this(model: Option[ModelBase], ff: Seq[String]) = {
    this(model)
    this + ff
  }

  private val fields: mutable.MutableList[Field] = mutable.MutableList()

  class Field(
               val name: String,
               var title:String,
               var fieldType: FormFieldType = FormFieldType.TextInput,
               var value: String = "",
               var valueId: Option[FieldValueId] = None,
               var options: Seq[FieldOption] = Nil
             )


  object  Field{
    def apply(name: String) = new Field(name, Titles.camelCaseToTitle(name))
    def apply(modelOption: Option[ModelBase], f: String):Field = {
      modelOption.fold(Field(f)) { model =>
        val mf = model.getClass.getDeclaredField(f)
        val ma = mf.getAnnotation(classOf[ViewMeta])
        val ma1: ViewMeta = Option(ma).fold(new ViewMeta)(x => x)
        val title = if (ma1.title().isEmpty) Titles.camelCaseToTitle(f) else ma.title()
        val fieldType = if (ma1.formFieldType() == FormFieldType.NotDefined) FormFieldType.defaultForType(mf) else ma1.formFieldType()
        val valueFromModel = model.get(f)
        new Field(f, title, fieldType, Option(valueFromModel).fold("")(_.toString), Option(new FieldValueId(valueFromModel)))
      }
    }
  }


  def +(f: Field):FormView = {fields+=f; this}
  def +(f: String):FormView = {fields+=Field(model,f); this}

  def +(ff: Seq[String]):FormView = {ff.foreach(this+_);this}
  def ++(ff: Seq[Field]):FormView = {ff.foreach(this+_);this}
}
