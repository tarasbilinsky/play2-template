package base.models.enums;

import base.models.*;

public enum UserRolesEnum {
    Admin,User,Guest;
    public UserRoleBase get(){
        return Lookup.find(UserRoleBase.class,this.name());
    }
}
