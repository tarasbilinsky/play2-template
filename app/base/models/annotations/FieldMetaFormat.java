package base.models.annotations;

import base.models.enums.FormatType;
import base.models.enums.SearchType;
import base.viewHelpers.FormFieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldMetaFormat {
    FormatType type() default FormatType.Undefined;
    String format() default "";
    boolean hideZeroesAfterDotInNumber() default true;
}
