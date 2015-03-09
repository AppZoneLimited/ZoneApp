package com.appzonegroup.zoneapp;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.orm.androrm.Filter;

import java.util.ArrayList;

import database.Entity;
import json.Data;
import json.EntityData;
import json.EventData;
import json.Instruction;
import json.Output;

/**
 * Created by emacodos on 3/7/2015.
 */

/*
* NOTES:
* For the Local event type
*
* 1. create: add new data to the data to the database ie the data passed on the create method
* 2. update: updates an existing record on the database. when data parameter on the update method
 *           is null, an error is returned. while otherwise the id from the data is used to identify
 *           the row to be updated.
 *3. retrieve: gets the records stored in the database, if data parameter is null, every row
 *             associated with the entity is retrieved. while otherwise the id from the data is used
 *             to identify the row to be retrieved.
 *4. delete: deletes the records stored in the database. if data parameter is null, every row
 *           associated with the entity is deleted. while otherwise the id from the data parameter
 *           is used to identify the row to be deleted.
*/

public class LocalEntityService {

    public static final String TAG = LocalEntityService.class.getSimpleName();

    public static final String OPERATION_CREATE = "create";
    public static final String OPERATION_UPDATE = "update";
    public static final String OPERATION_RETRIEVE = "retrieve";
    public static final String OPERATION_DELETE = "delete";

    public static final String EVENT_NAME_CREATE = "Entity Created";
    public static final String EVENT_NAME_UPDATE = "Entity Updated";
    public static final String EVENT_NAME_RETRIEVE = "Entity Retrieved";
    public static final String EVENT_NAME_DELETE = "Entity Deleted";

    public static final String EVENT_NAME_ERROR = "Entity Operation Failed";

    public static final String TYPE_SERVER = "server";
    public static final String TYPE_LOCAL = "local";

    public static final String STATUS_FAILURE = "failure";
    public static final String STATUS_SUCCESS = "success";

    private static Gson mJson;

    private static Context mContext;

    public LocalEntityService(){
    }

    public static String localEntityService(Context context, String instruction, String data){

        String type, operation, entity;

        mContext = context;
        mJson = new Gson();

        if(instruction == null || TextUtils.isEmpty(instruction)){
            return eventError("Null instruction Value");
        }
        else {
            //Parse JSON instruction to Java Object
            Instruction ins = mJson.fromJson(instruction, Instruction.class);
            type = ins.getType();
            operation = ins.getOperation();
            entity = ins.getEntity();

            //Check for the method type
            switch (type) {
                case TYPE_SERVER:
                    //Do server work
                    switch (operation) {
                        case OPERATION_CREATE:
                            //Do Create
                            return addNewRecordToServer(entity, data);

                        case OPERATION_RETRIEVE:
                            //Do Retrieve
                            return queryServerDB(entity, data);

                        case OPERATION_UPDATE:
                            //Do Update
                            return updateRecordInServer(entity, data);

                        case OPERATION_DELETE:
                            //Do Delete
                            return deleteRecordFromServer(entity, data);

                        default:
                            return eventError("Unknown Operation");

                    }
                case TYPE_LOCAL:
                    //Do local work
                    switch (operation) {
                        case OPERATION_CREATE:
                            //Do Create
                            return addNewRecord(entity, data);

                        case OPERATION_RETRIEVE:
                            //Do Retrieve
                            return queryDB(entity, data);

                        case OPERATION_UPDATE:
                            //Do Update
                            return updateRecord(entity, data);

                        case OPERATION_DELETE:
                            //Do Delete
                            return deleteRecord(entity, data);

                        default:
                            return eventError("Unknown Operation");
                    }
                default:
                    return eventError("Unknown Type");
            }
        }
    }

    private static String addNewRecordToServer(String entity, String data) {
        return null;
    }

    private static String queryServerDB(String entity, String data) {
        return null;
    }

    private static String updateRecordInServer(String entity, String data) {
        return null;
    }

    private static String deleteRecordFromServer(String entity, String data) {
        return null;
    }

    private static String addNewRecord(String entity, String data) {
        if(entity == null || TextUtils.isEmpty(entity)){
            return eventError("Null entity Value");
        }
        else if(data == null || TextUtils.isEmpty(data)){
            return eventError("Null data Value");
        }
        else {
            Entity table = new Entity();
            table.setEntityName(entity);
            table.setValue(data);
            if(table.save(mContext)){

                Data d = mJson.fromJson(data, Data.class);
                ArrayList<EntityData> entityDatas = d.getData();

                EventData eventData = new EventData();
                eventData.setRowId(table.getId() + "");
                eventData.setData(entityDatas);

                ArrayList<EventData> eventDataList = new ArrayList<>();
                eventDataList.add(eventData);

                return eventSuccess(EVENT_NAME_CREATE, eventDataList);
            }
            else {
                return eventError("SQL Error");
            }
        }
    }

    private static String queryDB(String entity, String data) {
        if(data == null || TextUtils.isEmpty(data)){
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();

            if(entities.size() > 0) {
                ArrayList<EventData> eventDataList = new ArrayList<>();
                for (int i = 0; i < entities.size(); i++) {
                    String dataStr = entities.get(i).getValue();

                    Data dataObj = mJson.fromJson(dataStr, Data.class);
                    ArrayList<EntityData> entityDatas = dataObj.getData();

                    EventData eventData = new EventData();
                    eventData.setRowId(entities.get(i).getId() + "");
                    eventData.setData(entityDatas);

                    eventDataList.add(eventData);
                }
                return eventSuccess(EVENT_NAME_RETRIEVE, eventDataList);
            }
            else {
                return eventError("No such Entity Found");
            }
        }
        else {
            int id;
            Data d = mJson.fromJson(data, Data.class);
            ArrayList<EntityData> entityDatas = d.getData();
            String mId = null;
            for (EntityData entityData: entityDatas){
                if(entityData.getName().equals(Entity.COLUMN_ID)){
                    mId = entityData.getValue();
                    break;
                }
            }
            if(mId == null || TextUtils.isEmpty(mId)){
                //Error no Id
                return eventError("ID Not Found");
            }
            else {
                try {
                    id = Integer.parseInt(mId);
                }catch (Exception e){
                    //Error fake id
                    return eventError("Invalid ID");
                }
                if(id > 0) {
                    Entity table = Entity.objects(mContext).get(id);
                    if (table != null) {
                        ArrayList<EventData> eventDataList = new ArrayList<>();
                        String dataStr = table.getValue();

                        Data dataObj = mJson.fromJson(dataStr, Data.class);
                        ArrayList<EntityData> entityDataList = dataObj.getData();

                        EventData eventData = new EventData();
                        eventData.setRowId(id + "");
                        eventData.setData(entityDataList);

                        eventDataList.add(eventData);
                        //success
                        return eventSuccess(EVENT_NAME_RETRIEVE, eventDataList);
                    }
                    else {
                        return eventError("ID Does Not Exist");
                    }
                }
                else {
                    return eventError("Invalid ID");
                }
            }
        }
    }

    private static String updateRecord(String entity, String data) {
        if(data == null || TextUtils.isEmpty(data)){
            return eventError("Null data Value");
        }
        else {
            int id;
            Data d = mJson.fromJson(data, Data.class);
            ArrayList<EntityData> entityDatas = d.getData();
            String mId = null;
            for (EntityData entityData: entityDatas){
                if(entityData.getName().equals(Entity.COLUMN_ID)){
                    mId = entityData.getValue();
                    entityDatas.remove(entityData);
                    break;
                }
            }
            if(mId == null || TextUtils.isEmpty(mId)){
                //Error no Id
                return eventError("ID Not Found");
            }
            else {
                try {
                    id = Integer.parseInt(mId);
                } catch (Exception e) {
                    //Error fake id
                    return eventError("Invalid ID");
                }
                Entity table = Entity.objects(mContext).get(id);
                if (table != null) {
                    table.setEntityName(entity);
                    table.setValue(mJson.toJson(entityDatas));
                    table.save(mContext);

                    EventData eventData = new EventData();
                    eventData.setRowId(table.getId() + "");
                    eventData.setData(entityDatas);

                    ArrayList<EventData> eventDataList = new ArrayList<>();
                    eventDataList.add(eventData);

                    return eventSuccess(EVENT_NAME_UPDATE, eventDataList);
                }
                else {
                    return eventError("Invalid ID");
                }
            }
        }
    }

    private static String deleteRecord(String entity, String data) {
        if(data == null || TextUtils.isEmpty(data)){
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();

            if(entities.size() > 0) {
                ArrayList<EventData> eventDataList = new ArrayList<>();
                for (int i = 0; i < entities.size(); i++) {
                    String dataStr = entities.get(i).getValue();

                    Data dataObj = mJson.fromJson(dataStr, Data.class);
                    ArrayList<EntityData> entityDatas = dataObj.getData();

                    EventData eventData = new EventData();
                    eventData.setRowId(entities.get(i).getId() + "");
                    eventData.setData(entityDatas);

                    eventDataList.add(eventData);

                    entities.get(i).delete(mContext);
                }

                return eventSuccess(EVENT_NAME_DELETE, eventDataList);
            }
            else {
                return eventError("No Such Entity Found");
            }
        }
        else {
            int id;
            Data d = mJson.fromJson(data, Data.class);
            ArrayList<EntityData> entityDatas = d.getData();
            String mId = null;
            for (EntityData entityData: entityDatas){
                if(entityData.getName().equals(Entity.COLUMN_ID)){
                    mId = entityData.getValue();
                    break;
                }
            }
            if(mId == null || TextUtils.isEmpty(mId)){
                //Error no Id
                return eventError("ID Not Found");
            }
            else {
                try {
                    id = Integer.parseInt(mId);
                }catch (Exception e){
                    //Error fake id
                    return eventError("Invalid ID");
                }
                Entity table = Entity.objects(mContext).get(id);
                if(table != null) {
                    if (table.delete(mContext)) {
                        //success

                        EventData eventData = new EventData();
                        eventData.setRowId(id + "");
                        eventData.setData(entityDatas);

                        ArrayList<EventData> eventDataList = new ArrayList<>();
                        eventDataList.add(eventData);

                        return eventSuccess(EVENT_NAME_DELETE, eventDataList);
                    } else {
                        //Error sql
                        return eventError("SQL Error");
                    }
                }
                else {
                    return eventError("Invalid ID");
                }
            }
        }
    }

    private static String eventError(String reason) {
        Output output = new Output();
        output.setEventName(EVENT_NAME_ERROR);
        output.setEventType(STATUS_FAILURE);
        output.setReason(reason);
        return mJson.toJson(output);
    }

    private static String eventSuccess(String eventName, ArrayList<EventData> eventDataList) {
        Output output = new Output();
        output.setEventName(eventName);
        output.setEventType(STATUS_SUCCESS);
        output.setEventData(eventDataList);
        return mJson.toJson(output);
    }
}