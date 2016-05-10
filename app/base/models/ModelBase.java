package base.models;

import base.models.annotations.TrackHistory;
import base.models.exceptions.ModelException;
import base.models.exceptions.TrackHistoryException;
import com.avaje.ebean.Model;
import com.avaje.ebean.bean.EntityBean;

import javax.persistence.MappedSuperclass;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

@MappedSuperclass
public abstract class ModelBase extends Model{

    private static class Property{
        Property(int i, String n){number=i;name=n;}
        int number;
        String name;
    }

    public ModelBase(){}

    @Override
    public int hashCode () {
        if ( id==null ) return 0;
        return (int)(long)id;
    }

    @Override
    public boolean equals(Object o){
        if ( o==null || id==null ) return false;
        if(!ModelBase.class.isAssignableFrom(o.getClass())) return false;
        ModelBase oM = (ModelBase) o;
        if(oM.id==null || this.id==null) return false;
        return
            (
                this.getClass().isAssignableFrom(o.getClass()) ||
                o.getClass().isAssignableFrom(this.getClass())
            )
            &&
            this.id.equals(oM.id);
    }


    public static final Long NewModelId = 0L;

    @javax.persistence.Id
    @javax.persistence.GeneratedValue
    public Long id;


    public Object get (int propertyNumber) {
        return ((EntityBean)this)._ebean_getFieldIntercept(propertyNumber);
    }

    protected int getPropertyIndex (String propertyName) {
        String[] mp=getPropertyNames();
        int pIndex = -1;
        for(int j=0;j<mp.length;j++){
            if(mp[j].equals(propertyName)){
                pIndex = j; break;
            }
        }
        if(pIndex==-1){
            throw new ModelException("Property "+propertyName+" not found in "+getClass().getName());
        }
        return pIndex;
    }

    public Object get (String propertyName) {
        EntityBean model = ((EntityBean)this);
        return model._ebean_getFieldIntercept(getPropertyIndex(propertyName));
    }

    public Object getRecurse (String propertyName) {
        ModelBase m = this;
        String[] pp = propertyName.split("\\.");
        return recurseProperties(m,pp,propertyName).get(pp[pp.length-1]);
    }

    public void set(int propertyIndex, Object value){
        EntityBean model = ((EntityBean)this);
        model._ebean_setFieldIntercept(propertyIndex,value);
    }

    public void set(String propertyName, Object value){
        set(getPropertyIndex(propertyName),value);
    }

    private ModelBase recurseProperties(ModelBase m, String[] pp, String propertyName){
        Object res;
        for(int i = 0; i<pp.length-1; i++){
            String p = pp[i];
            res = m.get(p);
            if(ModelBase.class.isAssignableFrom(res.getClass())){
                m = (ModelBase)res;
            } else {
                throw new ModelException("Property (recurse) "+propertyName+"not found in "+getClass().getName()+". Property "+p+" is not ModelBase but "+m.getClass());
            }
        }
        return m;
    }

    public void setRecurse (String propertyName, Object value) {
        ModelBase m = this;
        String[] pp = propertyName.split("\\.");
        recurseProperties(m,pp,propertyName).set(pp[pp.length-1],value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(){
        super.save();
        TrackHistory a = this.getClass().getAnnotation(TrackHistory.class);
        if(a!=null){
            ModelBase h;
            if(a.model().equals(void.class)){
                String ms = a.pkg()+".History"+this.getClass().getSimpleName();
                try {
                    Class<?> clazz = Class.forName(ms);
                    if(!ModelBase.class.isAssignableFrom(clazz)){
                        throw new TrackHistoryException("History model has to be a Model, history class name"+ms);
                    } else {
                        h = ModelBase.newInstance((Class<? extends ModelBase>)clazz);
                    }
                } catch (ClassNotFoundException e) {
                    throw new TrackHistoryException("Missing history model "+ms);
                }
            } else {
                h = newInstance((Class<ModelBase>)a.model());
            }
            copy(this,h);
            h.set("_ModelId",this.id);
            h.set("_Date", Calendar.getInstance().getTime());
            /// h.set("h_user",???) --- Solution: use transient lastModifiedByUser, and same field in history model no transient
            h.save();
        }
    }

    public List<Property> compareTo(ModelBase that) throws Exception{
        if(that==null || !that.getClass().equals(this.getClass())) throw new ModelException("Cannot compare different types");
        String[] mp=getPropertyNames();
        List<Property> res = new LinkedList<>();
        for(int j=0;j<mp.length;j++){
            Object pThis = this.get(j);
            Object pThat = that.get(j);
            if(pThis==null && pThat==null) continue; else{
                if(pThis!=null && pThat!=null && pThis.equals(pThat)) continue; else
                res.add(new Property(j,mp[j]));
            }
        }
        return res;
    }

    public static <T extends ModelBase> T newInstance(Class<T> clazz){
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ModelException("Cannot instantiate "+clazz.getCanonicalName());
        }
    }

    public String[] getPropertyNames(){
        EntityBean model = ((EntityBean)this);
        return model._ebean_getPropertyNames();
    }

    public boolean isEmpty(){
        ModelBase empty = newInstance(this.getClass());
        String[] mp=getPropertyNames();
        for(int j=0;j<mp.length;j++){
            if(mp[j].equals("id")) continue;
            Object pThis = this.get(j);
            Object pThat = empty.get(j);
            if(pThis==null && pThat==null) continue;
                if(pThis!=null && pThat!=null && pThis.equals(pThat)) continue; else return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ModelBase> T copy(T source){
        Class<T> clazz = (Class<T>)source.getClass();
        T destination = newInstance(clazz);
        String[] mp = source.getPropertyNames();
        for(int j=0;j<mp.length;j++) {
            if(mp[j].equals("id")) continue;
            //?? if(clazz.getDeclaredField(mp[j]).getAnnotation(Formula.class)) continue;
            destination.set(j,source.get(j));
        }
        return destination;
    }

    public static <T extends ModelBase, D extends ModelBase> void copy(T source, D destination){
        if(destination==null) return;
        String[] mp = source.getPropertyNames();
        for(int j=0;j<mp.length;j++) {
            if(mp[j].equals("id")) continue;
            try {
                destination.set(mp[j],source.get(j));
            } catch (ModelException e){
                //ignore
            }
        }
    }

}
