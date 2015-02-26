package database;

import android.content.Context;

import com.orm.androrm.CharField;
import com.orm.androrm.ForeignKeyField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

/**
 * Created by emacodos on 2/18/2015.
 */
public class ClientFlows extends Model {

    protected CharField flows;
    protected ForeignKeyField<Function> functionID;
    protected ForeignKeyField<Link> linkID;

    public ClientFlows(){
        super();

        flows = new CharField();
        functionID = new ForeignKeyField<Function>(Function.class);
        linkID = new ForeignKeyField<Link>(Link.class);
    }

    public static final QuerySet<ClientFlows> objects(Context context) {
        return objects(context, ClientFlows.class);
    }

    public String getClientFlow() {
        return flows.get();
    }

    public void setClientFlow(String content) {
        flows.set(content);
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
