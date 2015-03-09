package json;

import java.util.ArrayList;

/**
 * Created by emacodos on 3/4/2015.
 */
public class EventData {

    private String row;
    private ArrayList<EntityData> rowData;

    public EventData(){
        rowData = new ArrayList<EntityData>();
    }

    public String getRowId() {
        return row;
    }

    public void setRowId(String rowId) {
        this.row = rowId;
    }

    public ArrayList<EntityData> getData() {
        return rowData;
    }

    public void setData(ArrayList<EntityData> data) {
        this.rowData = data;
    }
}
