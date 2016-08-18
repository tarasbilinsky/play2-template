package models;

import base.models.UserBase;
import base.models.UserRoleBase;
import base.models.annotations.FieldMeta;
import base.models.annotations.FieldMetaOptionsSource;
import base.models.enums.SearchType;
import base.viewHelpers.FormFieldType;

import javax.persistence.*;
import java.util.Set;

@Entity
public class User extends UserBase {

    @FieldMeta(title = "User Name", formFieldType = FormFieldType.TextInput)
    public String name;

    public SearchType x;

    @ManyToOne
    public UserRole primaryUserRole;

    @ManyToMany
    @FieldMetaOptionsSource()
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
        return primaryUserRole;
    }


}
