package base.models;

import base.models.annotations.Cached;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Cached
public class UserRoleBase extends Lookup {
}
