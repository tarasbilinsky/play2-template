package base.models;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.util.Date;

@MappedSuperclass
public abstract class UserSessionBase extends ModelBase {
    public Date  start;
    public Date  last;
    public boolean closed;
    @ManyToOne
    public UserBase user;
}
