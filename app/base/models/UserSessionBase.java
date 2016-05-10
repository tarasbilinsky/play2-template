package base.models;

import base.controllers.EnvironmentAll;
import com.avaje.ebean.Ebean;
import models.UserSession;
import play.api.cache.CacheApi;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.util.Date;
import java.util.function.Function;

@MappedSuperclass
public abstract class UserSessionBase<T extends UserBase> extends ModelBase {
    public final long  start;
    private long  last;
    public void setLast(long last){this.last=last;}
    private boolean closed;
    public abstract T getUser();
    public abstract void setUser(T user);
    public void close(){
        closed = true; save();
    }
    public UserSessionBase(T user){
        start = System.currentTimeMillis();
        last = start;
        closed = false;
        this.setUser(user);
        this.save();
    }

    private static final char SEPARATOR_CHAR = 'X';

    public String getIdString(){
        return this.id.toString() + SEPARATOR_CHAR + this.getUser().id.toString();
    };

    public static <S extends UserSessionBase> S restore(
            Class<S> sessionClass,
            Class<? extends UserBase> userClass,
            String id) throws Exception {
        String[] ids = id.split("\\Q" + SEPARATOR_CHAR + "\\E");
        Long sessionId = Long.parseLong(ids[0]);
        Long userId = Long.parseLong(ids[1]);
        S session = Ebean.createQuery(sessionClass).select("*").fetch("user").fetch("user.primaryRole").fetch("user.roles").fetch("user.permissions").where().idEq(sessionId).eq("closed",0).findUnique();
        if (session == null) {
            session = sessionClass.getDeclaredConstructor(userClass).newInstance(Ebean.createQuery(userClass).select("*").fetch("primaryRole").fetch("roles").fetch("permissions").where().idEq(userId).findUnique());
        } else {
            session.setLast(System.currentTimeMillis());
            session.save();
        }
        return session;
    }

}
