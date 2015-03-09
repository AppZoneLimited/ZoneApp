package json;

/**
 * Created by emacodos on 3/4/2015.
 */
public class Instruction {

    private String type;
    private String operation;
    private String entity;

    public Instruction(){

    }

    public String getType() {
        return type;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntity() {
        return entity;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }
}
