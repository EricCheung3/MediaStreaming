package openfire.chat.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import openfire.chat.adapter.FriendsAdapter;
import openfire.chat.service.UserServiceImpl;

import org.easydarwin.android.camera.R;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.IBBProviders;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class ChatMainActivity extends Activity {

	private XMPPConnection connection;
	private ArrayList<String> messages = new ArrayList<String>();
	private Handler mHandler = new Handler();
	private ListView listview;
	private EditText textMessage;
	private Button btn_Send;
	private Button btn_Cancel;
	private ListView friendlistView;
	private PopupWindow popFriends;
	private PopupWindow popStreamingLink;
	private FriendsAdapter friendsAdapter;
	private List<Map<String, String>> listMap;
	private ArrayList<String> selectedListMap = new ArrayList<String>();

	private String streaminglink = "http://129.128.184.46:8554/live.sdp";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_chat);
		// set provider manager
		configureProviderManager(ProviderManager.getInstance());
		
		String username = getIntent().getStringExtra("username");
		String password = getIntent().getStringExtra("password");

		textMessage = (EditText) this.findViewById(R.id.edit_say_something);
		listview = (ListView) this.findViewById(R.id.listMessages);
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//				R.layout.listitem, messages);
//		listview.setAdapter(adapter);
		MessageAdapter();
		
		Button send = (Button) this.findViewById(R.id.btn_send_message);
		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String to = "admin@myria";
				String text = textMessage.getText().toString();
				Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);				
				if (connection != null) {
					connection.sendPacket(msg);
					messages.add(connection.getUser().split("@")[0] + ":");
					messages.add(text);
//					setListAdapter();
					MessageAdapter();
					//Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
					
				}
				
			}
		});

		// popup all the friends
		popupContactList(username, password);
		connection = GetConnection(username, password);
		// Set the status to available
		Presence presence = new Presence(Presence.Type.available);
		connection.sendPacket(presence);
		// get message listener
		ReceiveMsgListenerConnection(connection);

		/**
		 * // test add user as your friends Roster roster =
		 * connection.getRoster(); if(addUsers(roster, "test1@myria", "test1")){
		 * Collection<RosterEntry> entries = roster.getEntries(); for
		 * (RosterEntry entry : entries) {
		 * Log.i("RosterEntry","------"+entry.toString()+"----"); //user4:
		 * user4@myria [myFriends] } }
		 */

		/**
		 * according to the selected friends create a multi-user chat room and
		 * invite them to join it (default the users invited join the room
		 * automatically) (or [accepted / decline ...])
		 */
		Button btnCreateRoom = (Button) findViewById(R.id.btnCreateRoom);
		btnCreateRoom.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (createMultiUserRoom(connection, "room2"/*, selectedListMap*/)) {
					Log.i("createMultiUserRoom", "success");
				} else
					Log.i("createMultiUserRoom", "not success");
			}

		});

	}

	private void MessageAdapter(){
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem, messages);
		listview.setAdapter(adapter);
	}
	// Select contact function

	private void popupContactList(String username, String password) {

		final View v = getLayoutInflater().inflate(R.layout.friendlist, null,
				false);
		int h = getWindowManager().getDefaultDisplay().getHeight();
		int w = getWindowManager().getDefaultDisplay().getWidth();

		listMap = new ArrayList<Map<String, String>>();

		popFriends = new PopupWindow(v, w - 10, (int) (((2.8) * h) / 4));
		popFriends.setAnimationStyle(R.style.MyDialogStyleBottom);
		popFriends.setFocusable(true);
		popFriends.setBackgroundDrawable(new BitmapDrawable());
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				popFriends.showAtLocation(v, Gravity.BOTTOM, 0, 0);
			}
		}, 1000L);

		friendlistView = (ListView) v.findViewById(R.id.friendlist);
		friendlistView.setItemsCanFocus(true);
		//friendsAdapter = new FriendsAdapter(username, password, this, listMap);

		friendlistView.setAdapter(friendsAdapter);
		friendlistView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long arg3) {
				// TODO Auto-generated method stub
				TextView name = (TextView) v.findViewById(R.id.friend_username);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_box);
				checkbox.toggle();

				friendsAdapter.getIsSelected().put(position,
						checkbox.isChecked());

				if (checkbox.isChecked()) {
					selectedListMap.add(listMap.get(position).get("username"));

				}
			}
		});

		btn_Send = (Button) v.findViewById(R.id.btn_play);
		btn_Send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				System.out.println(selectedListMap.size());
				for (int i = 0; i < selectedListMap.size(); i++) {
					Log.i("XMPPChatDemoActivity", "Sending text "
							+ streaminglink + " to " + selectedListMap.get(i));
					Message msg = new Message(selectedListMap.get(i),
							Message.Type.chat);
					msg.setBody(streaminglink);
					if (connection != null) {
						connection.sendPacket(msg);
						messages.add(connection.getUser().split("@")[0] + ":");
						messages.add(streaminglink);
						popFriends.dismiss();
					}
				}
			}
		});
		Button btn_send_cancel = (Button) v.findViewById(R.id.btn_send_cancel);
		btn_send_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				popFriends.dismiss();
			}
		});
	}

	private XMPPConnection GetConnection(String username, String passwd) {
		try {
			if (null == connection || !connection.isAuthenticated()) {
				XMPPConnection.DEBUG_ENABLED = true;

				ConnectionConfiguration config = new ConnectionConfiguration(
						UserServiceImpl.SERVER_HOST,
						UserServiceImpl.SERVER_PORT,
						UserServiceImpl.SERVER_NAME);
				config.setReconnectionAllowed(true);
				config.setSendPresence(true);
				config.setSASLAuthenticationEnabled(true);
				connection = new XMPPConnection(config);
				connection.connect();
				connection.login(username, passwd);

				return connection;
			}
		} catch (XMPPException xe) {
			Log.e("XMPPChatDemoActivity", xe.toString());
		}
		return null;
	}

	public void ReceiveMsgListenerConnection(XMPPConnection connection) {

		this.connection = connection;
		if (connection != null) {
			// Add a packet listener to get messages sent to us
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			connection.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						final String[] fromName = StringUtils.parseBareAddress(
								message.getFrom()).split("@");
						Log.i("XMPPChatDemoActivity", "Text Recieved "+ message.getBody() + " from " + fromName[0]);
						messages.add(fromName[0] + ":");
						messages.add(message.getBody());
						final String msg = message.getBody().toString();
						// Add the incoming message to the list view
						
						mHandler.post(new Runnable() {
							public void run() {
								// notification or chat...
								if (msg.equals(streaminglink)){
									Log.i("XMPPChatDemoActivity", msg);
									popupReceiveStreamingLinkMessage(msg);
								}
								else {
									// display message like chatting
									MessageAdapter();
									Toast.makeText(getApplicationContext(),fromName[0] + ": " + msg,Toast.LENGTH_LONG).show();
									
								}
							}
						});
					}
				}
			}, filter);
		}
	}

	private void popupReceiveStreamingLinkMessage(String message) {

		final View v = getLayoutInflater().inflate(R.layout.streaminglink,
				null, false);

		int h = getWindowManager().getDefaultDisplay().getHeight();
		int w = getWindowManager().getDefaultDisplay().getWidth();

		popStreamingLink = new PopupWindow(v, w - 10, 3* h / 4);
		popStreamingLink.setAnimationStyle(R.style.MyDialogStyleBottom);
		popStreamingLink.setFocusable(true);
		popStreamingLink.setBackgroundDrawable(new BitmapDrawable());
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				popStreamingLink.showAtLocation(v, Gravity.BOTTOM, 0, 0);
			}
		}, 1000L);

		TextView stramingLink = (TextView) v.findViewById(R.id.streaming_link);
		stramingLink.setText(message);
		btn_Send = (Button) v.findViewById(R.id.btn_play_streaming);
		btn_Send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// DO PLAYING
				popStreamingLink.dismiss();
			}
		});
		Button btn_cancel = (Button) v.findViewById(R.id.btn_cancle);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				popStreamingLink.dismiss();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (connection != null)
				connection.disconnect();
		} catch (Exception e) {

		}
	}

	// Add user
	public static boolean addUsers(Roster roster, String userName, String name) {
		try {
			roster.createEntry(userName, name, null/*
													 * roster.getGroup(groupName)
													 */);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// create a multi-user chat room & invite them to join
	public boolean createMultiUserRoom(XMPPConnection connection,
			String roomName/*, ArrayList<String> friendlist*/) {

		// Get the MultiUserChatManager
		// Create a MultiUserChat using an XMPPConnection for a room
		MultiUserChat muc = new MultiUserChat(connection, roomName
				+ "@conference.myria");

		try {

			// Create the room
			muc.create(roomName);

			// Get the the room's configuration form
			Form form = muc.getConfigurationForm();
			// Create a new form to submit based on the original form
			Form submitForm = form.createAnswerForm();
			// Add default answers to the form to submit
			for (Iterator fields = form.getFields(); fields.hasNext();) {
				FormField field = (FormField) fields.next();
				if (!FormField.TYPE_HIDDEN.equals(field.getType())
						&& field.getVariable() != null) {
					// Sets the default value as the answer
					submitForm.setDefaultAnswer(field.getVariable());
				}
			}
			// Send the completed form (with default values) to the server to
			// configure the room
			muc.sendConfigurationForm(submitForm);
			// Create a MultiUserChat using an XMPPConnection for a room

			muc.invite("user11@myria", "come baby");
			muc.join("user11@myria");
			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		return false;
	}

	public void configureProviderManager(ProviderManager pm) {

		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time",
					Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
			Log.w("TestClient",
					"Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());

		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version",
					Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}

		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		pm.addIQProvider("open", "http://jabber.org/protocol/ibb",
				new IBBProviders.Open());

		pm.addIQProvider("close", "http://jabber.org/protocol/ibb",
				new IBBProviders.Close());

		pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb",
				new IBBProviders.Data());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.SessionExpiredError());
	}
}
