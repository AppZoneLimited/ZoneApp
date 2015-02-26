package database;

import android.content.Context;

import com.orm.androrm.BooleanField;
import com.orm.androrm.CharField;
import com.orm.androrm.ForeignKeyField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

/**
 * Created by emacodos on 2/19/2015.
 */
public class Link extends Model {

    protected CharField url;
    protected CharField type;
    protected BooleanField success;
    protected ForeignKeyField<Function> functionID;

    public Link(){
        super();

        url = new CharField();
        type = new CharField();
        success = new BooleanField();
        functionID = new ForeignKeyField<Function>(Function.class);
    }

    public static final QuerySet<Link> objects(Context context) {
        return objects(context, Link.class);
    }

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String api) {
        url.set(api);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String flowType) {
        type.set(flowType);
    }

    public Boolean isSuccess() {
        return success.get();
    }

    public void setSuccess(Boolean state) {
        success.set(state);
    }

    public Function getFunction(Context context) {
        return functionID.get(context);
    }

    public void setFunction(Function function) {
        functionID.set(function);
    }
}
