package models;

import base.models.PermissionBase;
import base.models.UserBase;
import base.models.UserRoleBase;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class User extends UserBase {
    public String name;
    public UserRole primaryRole;
    @ManyToMany
    public Set<UserRole> roles;
    @ManyToMany
    public Set<Permission> permissions;

    @Override
    public Set<UserRole> getRoles() {
        return roles;
    }

    @Override
    public Set<Permission> getPermissions(){return permissions;}

    @Override
    public UserRoleBase getPrimaryRole() {
        return null;//TODO
    }
}
