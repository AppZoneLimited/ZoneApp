package database;

import android.content.Context;

import com.orm.androrm.BooleanField;
import com.orm.androrm.CharField;
import com.orm.androrm.ForeignKeyField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

/**
 * Created by emacodos on 2/18/2015.
 */
public class EntityFlows extends Model {

    protected CharField flows;
    protected BooleanField sync;
    protected ForeignKeyField<Function> functionID;
    protected ForeignKeyField<Link> linkID;

    public EntityFlows(){
        super();

        flows = new CharField();
        sync = new BooleanField();
        functionID = new ForeignKeyField<Function>(Function.class);
        linkID = new ForeignKeyField<Link>(Link.class);
    }

    public static final QuerySet<EntityFlows> objects(Context context) {
        return objects(context, EntityFlows.class);
    }

    public String getEntityFlow() {
        return flows.get();
    }

    public void setEntityFlow(String content) {
        flows.set(content);
    }

    public boolean isSync(){
        return sync.get();
    }

    public void setSync(boolean state){
        sync.set(state);
    }

    public Function getFunction(Context context) {
        return functionID.get(context);
    }

    public void setFunction(Function function) {
        functionID.set(function);
    }

    public Link getLink(Context context) {
        return linkID.get(context);
    }

    public void setLink(Link link) {
        linkID.set(link);
    }

}
