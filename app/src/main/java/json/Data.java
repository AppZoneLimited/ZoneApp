package json;

import java.util.ArrayList;

/**
 * Created by emacodos on 3/4/2015.
 */
public class Data {

    private ArrayList<EntityData> data;

    public Data(){
        data = new ArrayList<EntityData>();
    }

    public ArrayList<EntityData> getData() {
        return data;
    }

    public void setData(ArrayList<EntityData> data) {
        this.data = data;
    }
}
