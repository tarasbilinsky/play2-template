package base.models;

import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import java.util.HashSet;
import java.util.Set;


@MappedSuperclass
public abstract class UserBase extends ModelBase {
    public String name;

    public String password;

    public Set<? extends UserRoleBase> getRoles(){return new HashSet<>();}

    public Set<? extends PermissionBase> getPermissions(){return new HashSet<>();}

    abstract public UserRoleBase getPrimaryRole();

}
