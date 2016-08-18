package base.viewHelpers;

import base.models.ModelBase;

import javax.persistence.Lob;
import java.lang.reflect.Field;

public enum FormFieldType {
    NotDefined,TextInput,TextArea,HtmlEdit,HtmlView,TextView,SelectBox,RadioButtons,Checkboxes,Hidden,TextViewWithHiddenId,PasswordInput;
    public static FormFieldType defaultForField(Field f){
        Class<?> cls = f.getType();
        if(cls.equals(boolean.class) || cls.isAssignableFrom(Boolean.class)) return RadioButtons;
        if(cls.isAssignableFrom(ModelBase.class)) return SelectBox;
        if(f.getAnnotation(Lob.class)!=null) return TextArea;
        return TextInput;
    }
}
