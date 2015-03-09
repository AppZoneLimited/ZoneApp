package json;

import java.util.ArrayList;

/**
 * Created by emacodos on 3/4/2015.
 */
public class Output {

    private String eventType;
    private String eventName;
    private ArrayList<EventData> eventData;
    private String reason;

    public Output(){
        eventData = new ArrayList<EventData>();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public ArrayList<EventData> getEventData() {
        return eventData;
    }

    public void setEventData(ArrayList<EventData> eventData) {
        this.eventData = eventData;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
