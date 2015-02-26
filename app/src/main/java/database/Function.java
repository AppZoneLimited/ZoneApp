package database;

import android.content.Context;

import com.orm.androrm.BooleanField;
import com.orm.androrm.CharField;
import com.orm.androrm.IntegerField;
import com.orm.androrm.Model;
import com.orm.androrm.OneToManyField;
import com.orm.androrm.QuerySet;

/**
 * Created by emacodos on 2/18/2015.
 */
public class Function extends Model {

    protected CharField ID;
    protected CharField Name;
    protected IntegerField version;
    protected BooleanField success;
    protected OneToManyField<Function, Link> link;
    protected OneToManyField<Function, ClientFlows> clientFlows;
    protected OneToManyField<Function, EntityFlows> entityFlows;

    public Function(){
        super();

        ID = new CharField();
        Name = new CharField();
        version = new IntegerField();
        success = new BooleanField();
        link = new OneToManyField<Function, Link>(Function.class, Link.class);
        clientFlows = new OneToManyField<Function, ClientFlows>(Function.class, ClientFlows.class);
        entityFlows = new OneToManyField<Function, EntityFlows>(Function.class, EntityFlows.class);
    }

    public static final QuerySet<Function> objects(Context context) {
        return objects(context, Function.class);
    }

    public String getFunctionId() {
        return ID.get();
    }

    public void setFunctionId(String functionId) {
        ID.set(functionId);
    }

    public String getFunctionName() {
        return Name.get();
    }

    public void setFunctionName(String customerName) {
        Name.set(customerName);
    }

    public int getVersion() {
        return version.get();
    }

    public void setVersion(int versionNo) {
        version.set(versionNo);
    }

    public Boolean isSuccess() {
        return success.get();
    }

    public void setSuccess(Boolean state) {
        success.set(state);
    }

    public QuerySet<Link> getLinks(Context context) {
        return link.get(context, this);
    }

    public void setLink(Link link1) {
        link.add(link1);
    }

    public QuerySet<ClientFlows> getClientFlows(Context context) {
        return clientFlows.get(context, this);
    }

    public void setClientFlows(ClientFlows flows) {
        clientFlows.add(flows);
    }

    public QuerySet<EntityFlows> getEntityFlows(Context context) {
        return entityFlows.get(context, this);
    }

    public void setEntityFlows(EntityFlows flows) {
        entityFlows.add(flows);
    }
}
