package models.test;

import base.models.annotations.FieldMeta;

public enum Color { Red, Green, @FieldMeta(title = "Blue!!!") Blue
}
