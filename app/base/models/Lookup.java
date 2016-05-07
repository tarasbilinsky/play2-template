package base.models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.annotation.Transactional;
import base.models.annotations.AutoSysName;
import base.models.annotations.Cached;
import base.models.exceptions.LookupException;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.MappedSuperclass;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@MappedSuperclass
public abstract class Lookup extends ModelBase {
    public String title;
    public String sysName;
    public int orderNumber;
    public boolean active;

    private static final Map<Class<? extends Lookup>,Map<Long,Lookup>> cache = new ConcurrentHashMap<>();
    private static final Map<Class<? extends Lookup>,Map<String,Lookup>> cacheSysName = new ConcurrentHashMap<>();


    public static <T extends Lookup> T find (Class<T> clazz, String sysName){
        if(StringUtils.isBlank(sysName)) throw new LookupException("Trying to find with empty SysName");
        if(clazz==null) throw new LookupException("Trying to find with null Class");
        sysName = sysName.toLowerCase();
        if(clazz.getAnnotation(Cached.class)!=null){
            Map<String,Lookup> m = cacheSysName.get(clazz);
            if(m!=null){
                Lookup r = m.get(sysName);
                if(r==null){
                    T rr = Ebean.createQuery(clazz).where().eq("sysName",sysName).findUnique();
                    if(rr !=null){
                        m.put(rr.sysName,rr);
                        return rr;
                    } else {
                        throw new LookupException("Sysname "+sysName+" not found");
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    T res = (T)r;
                    return res;
                }
            } else {
                List<T> rr = Ebean.createQuery(clazz).select("*").order("sysName").findList();
                cacheSysName.put(clazz, new ConcurrentHashMap<>());
                T r = null;
                for(T t: rr){
                    String s = t.sysName.toLowerCase();
                    m.put(s,t);
                    if(s.equals(sysName)) r = t;
                }
                if(r == null) throw new LookupException("Sysname "+sysName+" not found");
                else return r;
            }
        } else {
            return Ebean.createQuery(clazz).where().eq("sysName",sysName).findUnique();
        }
    }

    public static <T extends Lookup> T find (Class<T> clazz, Long id){
        if(clazz==null) throw new LookupException("Trying to find with null Class");
        if(id==null || id==0L) throw new LookupException("Trying to find with empty Id");
        if(clazz.getAnnotation(Cached.class)!=null){
            Map<Long,Lookup> m = cache.get(clazz);
            if(m!=null){
                Lookup r = m.get(id);
                if(r==null){
                    T rr = Ebean.createQuery(clazz).where().idEq(id).findUnique();
                    if(rr !=null){
                        m.put(rr.id,rr);
                        return rr;
                    } else {
                        throw new LookupException("Id "+id+" not found");
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    T res = (T)r;
                    return res;
                }
            } else {
                List<T> rr = Ebean.createQuery(clazz).select("*").order("sysName").findList();
                cacheSysName.put(clazz, new ConcurrentHashMap<>());
                T r = null;
                for(T t: rr){
                    m.put(t.id,t);
                    if(t.id.equals(id)) r = t;
                }
                if(r == null) throw new LookupException("Id "+id+" not found");
                else return r;
            }
        } else {
            return Ebean.createQuery(clazz).where().idEq(id).findUnique();
        }
    }

    @Override @Transactional
    public void save(){
        if(this.getClass().getAnnotation(AutoSysName.class)!=null){
            if(StringUtils.isBlank(sysName)){
                sysName = title.toLowerCase().replaceAll("[^0-9a-zA-Z_-]","");
                int i = 0;
                while(Ebean.createQuery(this.getClass()).where().eq("sysName",sysName+(i==0?"":i)).findRowCount()>0) i++;
                if(i>0) sysName = sysName+i;
            }
            super.save();
        }
    }

}