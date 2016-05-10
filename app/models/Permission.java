package models;

import base.models.PermissionBase;
import base.models.annotations.Cached;

import javax.persistence.Entity;


@Entity
@Cached
public class Permission extends PermissionBase {
}
