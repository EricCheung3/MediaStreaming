package easydarwin.android.videostreaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.easydarwin.android.camera.R;

import android.app.Activity;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ContactAdapter extends BaseAdapter{


	private List<ContactInfo> contactlist = new ArrayList<ContactInfo>();  
    private Activity context;  
    private static HashMap<Integer,Boolean> isSelected = new HashMap<Integer,Boolean>();
    private List<Boolean> mChecked;
    
	public ContactAdapter(List<ContactInfo> contactlist, Activity context){
		this.contactlist = contactlist;
		this.context = context;

		getAllContacts();
	}
	
	private void getAllContacts(){  
        // get all the contacts
        Cursor cur = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,  
                null,null,null, ContactsContract.Contacts.DISPLAY_NAME  
                + " COLLATE LOCALIZED ASC");  
         
        if (cur.moveToFirst()) {  
            int id = cur.getColumnIndex(ContactsContract.Contacts._ID);  
            int displayName = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);  

            while (cur.moveToNext()){  
                ContactInfo contact = new ContactInfo();   
                String contactId = cur.getString(id);  
                String disPlayName = cur.getString(displayName);  
                //contact.setContactName(disPlayName);  

                Cursor phones =context.getContentResolver().query(  
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,  
                        null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID  
                        + " = " + contactId, null, null);  

                if (phones.moveToFirst()) {  
                    do {   
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));  
                        contact.setUserNumber(phoneNumber);  
                        String Name = phones.getString(phones.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));  
                        contact.setContactName(Name);;  
                       // System.out.println("phoneNumber:"+Name+phoneNumber);
                    } while (phones.moveToNext());  
                }  
                phones.close();  
  
                //System.out.println(contact.getContactName()+contact.getUserNumber());
                if(contact.getUserNumber()!=null)
                	contactlist.add(contact);  
            }  
        }         
        cur.close();  
    }
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return contactlist.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return contactlist.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub

		if(convertView ==null)
			convertView = View.inflate(context,R.layout.contact_layout, null);  
			
		
		TextView name = (TextView)convertView.findViewById(R.id.contactName);
		CheckBox checkbox = (CheckBox)convertView.findViewById(R.id.check_box);
		
		ContactInfo contact = contactlist.get(position);  
        name.setText(contact.getContactName());  

        checkbox.setChecked(contact.getChecked());  
        
        checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {             
            @Override  
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {  
                // TODO Auto-generated method stub    
                contactlist.get(position).setChecked(isChecked);  
            }  
        });          
			
		return convertView;
	}

	public HashMap<Integer,Boolean> getIsSelected(){
		return isSelected;
	}
	
	public static void setIsSelected(HashMap<Integer,Boolean> isSelected){
		ContactAdapter.isSelected = isSelected;
	}
}
