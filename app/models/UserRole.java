package models;


import base.models.UserRoleBase;
import base.models.annotations.Cached;
import javax.persistence.Entity;


@Entity
@Cached
public class UserRole extends UserRoleBase {
}
