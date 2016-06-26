import base.viewHelpers.{Field, FormView}
import org.scalatest.FlatSpec

class FormViewTest extends FlatSpec{
  "Form View" should "work" in {
    val x = new FormView(Seq(Field("A")),"test")
    assert(x!=null)
  }

}
