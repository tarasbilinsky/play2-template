package formView

import base.models.enums.AlignType
import base.viewHelpers.{BoundField, Field, FormFieldType, FormView}
import models.ModelPlaceholders._
import models.{User, UserRole}
import models.test.{Color, Test1}
import net.oltiv.scalaebean.Shortcuts._
import org.scalatest.FlatSpec

class FormViewTest extends FlatSpec{
  base.utils.test.setUpTestORM()
  val u = new User

  def userField(n: String):BoundField = Field(u,classOf[User].getField(n))


  "Form View" should "work" in {
    val x = new FormView(Seq(Field("A")),"test")
    assert(x!=null)

    val f1 = Field("name")
    val f2 = Field(new User(), classOf[User].getDeclaredField("name"))
    val f3 = Field(new User(), classOf[User].getDeclaredField("primaryRole"))

    assert(f1==f2)
    assert(f1!=f3)
  }

  "Field Input Type" should "work" in {
    val f = userField(props(user,user.name))
    assert(f.getFieldType==FormFieldType.TextInput)

    val f2 = userField(props(user,user.primaryRole))
    assert(f2.getFieldType==FormFieldType.SelectBox)

    val f3 = userField(props(user,user.active))
    assert(f3.getFieldType==FormFieldType.RadioButtons)
  }

  "Field options" should "work for lookups" in {
    val f2 = userField(props(user,user.primaryRole))
    val r = new UserRole
    r.active = true
    r.sysName = "test"
    r.title = "test"
    r.save()
    assert(f2.getOptions.size>0)
  }

  "Format number with no meta" should "work" in {
    val t = new Test1

    List( (2.14556,"2.15"), (-2.14556,"-2.15"), (3.00,"3") ).foreach { case (n, s) =>
      t.a = n
      val f = Field(t, classOf[Test1].getDeclaredField(props(t, t.a)))
      assert(f.getValue == s)

      t.b = n.asInstanceOf[Float]
      val f2 = Field(t, classOf[Test1].getDeclaredField(props(t, t.b)))
      assert(f2.getValue == s)

      val f3 = Field(t, classOf[Test1].getDeclaredField(props(t, t.c)))
      assert(f3.getValue=="")

    }
  }

  "Format money" should "work" in {
    val t = new Test1

    t.money = 34.55

    val f = Field(t,props(t,t.money))

    assert(f.getValue=="$34.55")

    t.money2 = 3.00

    val f2 = Field(t,props(t,t.money2))
    assert(f2.getValue == "3.00")
  }


  "Field options" should "work for enums" in {
    val t = new Test1
    val ft = Field(t,classOf[Test1].getDeclaredField(props(t,t.color)))
    val options = ft.getOptions
    assert(options.size==3)
    assert(options(2).title=="Blue!!!")
  }

  "Field format boolean value" should "work" in {
    u.active = true
    val f = userField(props(user,user.active))
    assert(f.getValue == "Yes")
    assert(f.getAlign == AlignType.Center)
    u.active = false
    val f2 = userField(props(user,user.active))
    assert(f2.getValue == "No")

  }

}
