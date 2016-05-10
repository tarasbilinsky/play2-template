package models;

import base.models.UserBase;
import base.models.UserSessionBase;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class UserSession extends UserSessionBase<User>{

    @ManyToOne
    public User user;

    public UserSession(User user){super(user);}

    @Override
    public User getUser(){return user;}

    @Override
    public void setUser(User user){this.user = user;}


    public static UserSession restore(String id) throws Exception{
        return restore(UserSession.class,User.class,id);
    }

}
