package database;

import android.content.Context;

import com.orm.androrm.BooleanField;
import com.orm.androrm.CharField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

/**
 * Created by emacodos on 2/18/2015.
 */
public class Entity extends Model {

    public static final String COLUMN_ID = "mId";
    public static final String COLUMN_TABLE_NAME = "tableName";

    protected CharField tableName;
    protected CharField value;
    protected BooleanField sync;

    public Entity(){
        super();

        tableName = new CharField();
        value = new CharField();
        sync = new BooleanField();
    }

    public static final QuerySet<Entity> objects(Context context) {
        return objects(context, Entity.class);
    }

    public String getEntityName() {
        return tableName.get();
    }

    public void setEntityName(String entityName) {
        tableName.set(entityName);
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String content) {
        value.set(content);
    }

    public boolean isSync(){
        return sync.get();
    }

    public void setSync(boolean state){
        sync.set(state);
    }

}
