package models;

import base.models.UserBase;
import base.models.UserRoleBase;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Set;

@Entity
public class User extends UserBase {
    public String name;
    public UserRole primaryRole;
    @ManyToMany
    public Set<UserRole> roles;
    @ManyToMany
    public Set<UserPermission> permissions;

    @Override
    public Set<UserRole> getRoles() {
        return roles;
    }

    @Override
    public Set<UserPermission> getPermissions(){return permissions;}

    @Override
    public UserRoleBase getPrimaryRole() {
        return roles==null || roles.isEmpty()?null:roles.iterator().next();
    }
}
