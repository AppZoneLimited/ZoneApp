package database;

import android.content.Context;

import com.orm.androrm.CharField;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

import java.util.ArrayList;

/**
 * Created by emacodos on 2/19/2015.
 */
public class Contact extends Model {

    protected CharField name;
    protected CharField number;

    public Contact() {
        super();

        name = new CharField();
        number = new CharField();
    }

    public static final QuerySet<Contact> objects(Context context) {
        return objects(context, Contact.class);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String contactName) {
        name.set(contactName);
    }

    public String getNumber() {
        return number.get();
    }

    public void setNumber(String phoneNo) {
        number.set(phoneNo);
    }

    public static ArrayList<Contact> getAllContact(Context context) {
        return (ArrayList<Contact>) Contact.objects(context).all().toList();
    }
}
