package models.test;

import base.models.ModelBase;
import base.models.annotations.FieldMetaFormat;
import base.models.enums.FormatType;

import javax.persistence.Entity;


@Entity
public class Test1 extends ModelBase {
 public Color color;
    public double a;
    public float b;
    public Float c;
    @FieldMetaFormat(type = FormatType.Money)
    public Double money;

    @FieldMetaFormat(type = FormatType.Money, format = "######.00")
    public Double money2;
}
