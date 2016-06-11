package base.models.annotations;

import base.viewHelpers.FormFieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ViewMeta {
    String title() default "";
    FormFieldType formFieldType() default FormFieldType.NotDefined;
}
