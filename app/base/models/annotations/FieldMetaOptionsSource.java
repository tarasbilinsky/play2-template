package base.models.annotations;

import base.models.ModelBase;
import base.models.enums.SearchType;
import base.viewHelpers.FormFieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldMetaOptionsSource {
    Class<? extends ModelBase> model() default ModelBase.class;
    String titleColumn() default "name";
    String orderColumn() default "id";
    boolean orderDescending() default false;
    boolean activeOnly() default false;
    String rawFilter() default "";

    /**
     * Raw sql with two columns named "id" and "name"
     * @return
     */
    String rawSql() default "";
}
