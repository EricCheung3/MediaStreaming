package openfire.chat.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import openfire.chat.service.UserServiceImpl;

import org.easydarwin.android.camera.R;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class FriendsAdapter extends BaseAdapter {

//	private static XMPPConnection connection = null;
	
	private Activity context;
	private List<Map<String,String>> listMap = new ArrayList<Map<String,String>>();
    private static HashMap<Integer,Boolean> isSelected = new HashMap<Integer,Boolean>();
    
	public FriendsAdapter(Activity context, List<Map<String,String>> listMap){
		this.context = context;
		this.listMap = listMap;
		//listMap = getFriendsList(username,passwd);
	}
	/*
	private List<Map<String,String>> getFriendsList(String username,String passwd){
		try {
			if (null == connection || !connection.isAuthenticated()) {
				XMPPConnection.DEBUG_ENABLED = true;

				ConnectionConfiguration config = new ConnectionConfiguration(
						UserServiceImpl.SERVER_HOST, UserServiceImpl.SERVER_PORT, UserServiceImpl.SERVER_NAME);
				config.setReconnectionAllowed(true);
				config.setSendPresence(true);
				config.setSASLAuthenticationEnabled(true);
				connection = new XMPPConnection(config);
				connection.connect();
				connection.login(username,passwd);
				
				Roster roster = connection.getRoster();
				Object[] entries = roster.getEntries().toArray();
				
				for (Object entry : entries) {
					Map<String,String> map = new HashMap<String,String>();
					
					String[] s = entry.toString().split(" ");

					if(s.length==2){
						map.put("name", s[0].substring(0, s[0].length()-1));
						map.put("username", s[1]);
					}else if(s.length==3){
						map.put("name", s[0].substring(0, s[0].length()-1));
						map.put("username", s[1]);
						map.put("group", s[2].substring(1, s[2].length()-1));
					}
					listMap.add(map);
				}
				System.out.println(listMap.toString());
				return listMap;
			}
		} catch (XMPPException xe) {
			Log.e("XMPPChatDemoActivity", xe.toString());
		}
		return null;
	}
	*/
	@Override
	public int getCount() {
		return listMap.size();
	}

	@Override
	public Object getItem(int position) {
		return listMap.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View view, ViewGroup arg2) {

		if(view == null)
			view = View.inflate(context, R.layout.frienditem, null);
		
		TextView username = (TextView) view.findViewById(R.id.friend_username);
		TextView displayname = (TextView) view.findViewById(R.id.friend_displayname);
		CheckBox check = (CheckBox) view.findViewById(R.id.check_box);
		
		
 
		username.setText(listMap.get(position).get("username"));
		//check.setChecked(checked);
		
		return view;
	}


	public HashMap<Integer,Boolean> getIsSelected(){
		return isSelected;
	}
	
	public static void setIsSelected(HashMap<Integer,Boolean> isSelected){
		FriendsAdapter.isSelected = isSelected;
	}
	
	private boolean isChecked;
	public boolean getChecked() {
		return isChecked;
	}
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
}
