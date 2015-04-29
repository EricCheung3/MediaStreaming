package easydarwin.android.videostreaming;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.Session.Callback;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtp.RtpThread;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class VideoStreamingActivity extends Activity implements Callback,
		RtspClient.Callback, android.view.SurfaceHolder.Callback,
		OnClickListener {

	private static final int REQUEST_SETTING = 1000;
	// current system info msg
	private static final int msgKey1 = 1;
	private PowerManager.WakeLock wl;
	private BroadcastReceiver mReceiver;
	private String mAddress;
	private String mPort;
	private String mVideoName;
	protected Session mSession;
	protected RtspClient mClient;

	/** Default quality of video streams. */
	public VideoQuality videoQuality;
	/** By default AMRNB is the audio encoder. */
	public int audioEncoder = SessionBuilder.AUDIO_AMRNB;
	/** By default H.264 is the video encoder. */
	public int videoEncoder = SessionBuilder.VIDEO_H264;
	private static final int mOrientation = 0;
	private Button btnOption;
	private Button btnSelectContact;
	private Button btnStop;
	private Button btnSendMessage;
	private TextView ipView;
	private TextView mTime;
	private boolean alive = false;
	private SurfaceView mSurfaceView;
	private static SurfaceHolder surfaceHolder;
	private SharedPreferences preferences;

	private Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
	private String username;
	private String password;
	private String entries;
	private List<Map<String, String>> friendList;
	private boolean messageFlag = true;
	private XMPPConnection connection;
	private String streaminglink = "rtsp://129.128.184.46:8554/";
	private String curDateTime;
	private String to = "admin@myria";


	//===============
//	 private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private PaintView paintView;
	 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.streaming_main);
		// set provider
		configureProviderManager(ProviderManager.getInstance());
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wl = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
						"com.control.wakelock");
		// SharedPreferences pref =
		// PreferenceManager.getDefaultSharedPreferences(this);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		preferences.registerOnSharedPreferenceChangeListener(spcl);
		// ConnectivityManager cm = (ConnectivityManager)
		// getSystemService(CONNECTIVITY_SERVICE);
		// NetworkInfo info = cm.getActiveNetworkInfo();
		// if (info != null) {
		// if (info.getType() == ConnectivityManager.TYPE_WIFI) {
		// pref.edit().putString("bit_rate", "4").commit();
		// } else {
		// pref.edit().putString("bit_rate", "2").commit();
		// }
		// }

		initView();
		curDateTime = new SimpleDateFormat(
				"yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime());
		streaminglink = streaminglink + getDefaultDeviceId()+ curDateTime + ".sdp";
		System.out.println(streaminglink);
		boolean bParamInvalid = (TextUtils.isEmpty(mAddress)
				|| TextUtils.isEmpty(mPort) || TextUtils.isEmpty(mVideoName));
		if (EasyCameraApp.sState != EasyCameraApp.STATE_DISCONNECTED) {
			setStateDescription(EasyCameraApp.sState);
		}
		if (bParamInvalid) {
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_SETTING);
		} else {
//			streaminglink = String.format("rtsp://%s:%d/%s.sdp", mAddress,
//					Integer.parseInt(mPort), mVideoName);
			ipView.setText(String.format("rtsp://%s:%d/%s.sdp", mAddress,
					Integer.parseInt(mPort), mVideoName));
			
		}
		//ipView.setText(streaminglink);

		/** TODO================================================================ */

//		connection = GetConnection(username, password);
		new GetXMPPConnection().execute();
		// Set the status to available
//		Presence presence = new Presence(Presence.Type.available);
//		connection.sendPacket(presence);
//		// get message listener
//		ReceiveMsgListenerConnection(connection);
		// (new ReceiveMessageThread()).start();
		

		
		btnSelectContact.setOnClickListener(this);
		btnOption.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnSendMessage.setOnClickListener(this);
		// EditText: set android keyboard enter button as send button
		textMessage.setOnEditorActionListener(new OnEditorActionListener() {
		    
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//				String to = "admin@myria";
				String text = textMessage.getText().toString();
				if(!text.equals("")&&text!=null){
					Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
					Message msg = new Message(to, Message.Type.chat);
					msg.setBody(text);
					if (connection != null) {
						connection.sendPacket(msg);
						messages.add(connection.getUser().split("@")[0] + ":");
						messages.add(text);
						Toast.makeText(getApplicationContext(), text,
								Toast.LENGTH_SHORT).show();
					}
					textMessage.setText("");
				}else{
					Toast.makeText(getApplicationContext(), "The input cannot be null!",
							Toast.LENGTH_SHORT).show();
				}
				return true;	    	
		    }
		});

//		mSurfaceView.setOnTouchListener(mSurfaceView);

		
//		mSurfaceView.setOnTouchListener(new OnTouchListener(){
//
//			@Override
//			public boolean onTouch(View arg0, MotionEvent arg1) {
//				// TODO Auto-generated method stub
//				Log.i("mSurfaceView","touch screen");
//				setContentView(new DrawingView(VideoStreamingActivity.this));
//				return false;
//			}
//		});
	}

	public void initView() {

		mAddress = preferences.getString("key_server_address", null);
		mPort = preferences.getString("key_server_port", null);
		mVideoName = preferences.getString("key_device_id",null/*getDefaultDeviceId()*/);
		ipView = (TextView) findViewById(R.id.main_text_description);
		mTime = (TextView) findViewById(R.id.timeDisplay);
		// draw paint View
		paintView = (PaintView)findViewById(R.id.drawView);
		
		mSurfaceView = (net.majorkernelpanic.streaming.gl.SurfaceView) findViewById(R.id.surface);
		mSurfaceView.setAspectRatioMode(SurfaceView.ASPECT_RATIO_PREVIEW);
		surfaceHolder = mSurfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// needed
																		// for
																		// sdk<11

		btnSelectContact = (Button) findViewById(R.id.btnPlay);
		btnOption = (Button) findViewById(R.id.btnOptions);
		btnStop = (Button) findViewById(R.id.btnStop);

		username = getIntent().getStringExtra("username");
		password = getIntent().getStringExtra("password");
		entries = getIntent().getStringExtra("entries");

		textMessage = (EditText) findViewById(R.id.edit_say_something);
		btnSendMessage = (Button) findViewById(R.id.btn_send_message);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnPlay:
			if (!alive) {
				// popupContactList();
				popupContactList(entries);
			} else {
				alive = false;
				stopStream();
				btnSelectContact.setBackgroundResource(R.drawable.play);
				ipView.setText(String.format("rtsp://%s:%d/%s.sdp", mAddress,
						Integer.parseInt(mPort), mVideoName));
			}

			break;
		case R.id.btnOptions:
			Intent intent = new Intent();
			intent.setClass(VideoStreamingActivity.this, SettingsActivity.class);
			startActivityForResult(intent, REQUEST_SETTING);

			break;
		case R.id.btnStop:
			if (alive) {
				alive = false;
				stopStream();
				btnSelectContact.setBackgroundResource(R.drawable.play);
				ipView.setText(String.format("rtsp://%s:%d/%s.sdp", mAddress,
						Integer.parseInt(mPort), mVideoName));
			}
			VideoStreamingActivity.this.finish();

			break;
		case R.id.btn_send_message:
//			String to = "admin@myria";
			String text = textMessage.getText().toString();
			if(!text.equals("")&&text!=null){
				Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				if (connection != null) {
					connection.sendPacket(msg);
					messages.add(connection.getUser().split("@")[0] + ":");
					messages.add(text);
					Toast.makeText(getApplicationContext(), text,
							Toast.LENGTH_SHORT).show();
				}
				textMessage.setText("");
			}else{
				Toast.makeText(getApplicationContext(), "The input cannot be null!",
						Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	/**
	 * Get All the Friends of user
	 * 
	 * @param entries
	 * @return
	 */
	private List<Map<String, String>> getFriendsList(String entries) {
		String[] entryList = entries.split(", ");
		friendList = new ArrayList<Map<String, String>>();
		for (String entry : entryList) {

			Map<String, String> map = new HashMap<String, String>();
			String[] s = entry.toString().split(" ");
			if (s.length == 2) {
				map.put("name", s[0].substring(0, s[0].length() - 1));
				map.put("username", s[1]);
			} else if (s.length == 3) {
				map.put("name", s[0].substring(0, s[0].length() - 1));
				map.put("username", s[1]);
				map.put("group", s[2].substring(1, s[2].length() - 1));
			}
			friendList.add(map);
		}
		System.out.println(friendList.toString());
		return friendList;
	}

	/**
	 * start video streaming function
	 */
	private void PLAYVideoStreaming() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		/**draw a circle when user touch the screen*/
		paintView.setOnTouchListener(paintView);
		
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected void onProgressUpdate(Void... values) {

				super.onProgressUpdate(values);
				alive = true;
				// start time thread
				new CurrentTimeThread().start();
				btnSelectContact.setBackgroundResource(R.drawable.pause);
			}

			@Override
			protected Integer doInBackground(Void... params) {

				publishProgress();

				if (mSession == null) {// try to load video info directly...
					boolean audioEnable = preferences.getBoolean(
							"p_stream_audio", true);
					boolean videoEnable = preferences.getBoolean(
							"p_stream_video", true);
					audioEncoder = Integer.parseInt(preferences.getString(
							"p_audio_encoder", String.valueOf(audioEncoder)));
					videoEncoder = Integer.parseInt(preferences.getString(
							"p_video_encoder", String.valueOf(videoEncoder)));

					Matcher matcher = pattern.matcher(preferences.getString(
							"video_resolution", "640x480"));
					matcher.find();

					videoQuality = new VideoQuality(Integer.parseInt(matcher
							.group(1)), Integer.parseInt(matcher.group(2)),
							Integer.parseInt(preferences.getString(
									"video_framerate", "15")),
							Integer.parseInt(preferences.getString(
									"video_bitrate", "300")) * 1000);
					mSession = SessionBuilder.getInstance()
							.setContext(getApplicationContext())
							.setAudioEncoder(audioEnable ? audioEncoder : 0)
							.setVideoQuality(videoQuality)
							.setAudioQuality(new AudioQuality(8000, 32000))
							.setVideoEncoder(videoEnable ? videoEncoder : 0)
							.setOrigin("127.0.0.0").setDestination(mAddress)
							.setSurfaceView(mSurfaceView)
							.setPreviewOrientation(mOrientation)
							.setCallback(VideoStreamingActivity.this).build();
				}

				if (mClient == null) {
					// Configures the RTSP client
					mClient = new RtspClient();

					String tranport = preferences.getString(
							EasyCameraApp.KEY_TRANPORT, "0");
					if ("0".equals(tranport)) {
						mClient.setTransportMode(RtspClient.TRANSPORT_TCP);
					} else {
						mClient.setTransportMode(RtspClient.TRANSPORT_UDP);
					}

					// mClient.setTransportMode(RtspClient.TRANSPORT_TCP);
					mClient.setSession(mSession);
					mClient.setCallback(VideoStreamingActivity.this);
				}

				mClient.setCredentials("", "");
				mClient.setServerAddress(mAddress, Integer.parseInt(mPort));
				mClient.setStreamPath(String.format("/%s.sdp",preferences.getString("key_device_id", Build.MODEL)));
				mClient.setStreamPath(String.format("/%s.sdp",getDefaultDeviceId()+curDateTime));
				
				/**
				 * IMPORTANT, start push stream.*/
				mClient.startStream();
				return 0;
			}

		}.execute();

	}

	private void stopStream() {
		if (mClient != null) {
			mClient.release();
			mClient.stopStream();
			mClient = null;
		}

		if (mSession != null) {
			mSession.release();
//			mSession.stop();
			mSession = null;
		}

		paintView.setVisibility(View.GONE);
		// mSurfaceView.getHolder().removeCallback(MainActivity.this);
		// mSurfaceView.setVisibility(View.GONE);
		// mSurfaceView.setVisibility(View.VISIBLE);
		// mSurfaceView.getHolder().addCallback(MainActivity.this);

		// finish();
	}

	public class CurrentTimeThread extends Thread {
		@Override
		public void run() {
			do {
				try {
					Thread.sleep(1000);
					android.os.Message msg = new android.os.Message();
					msg.what = msgKey1;
					mHandler.sendMessage(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (alive);
		}

		@SuppressLint({ "HandlerLeak", "SimpleDateFormat" })
		private Handler mHandler = new Handler() {
			@Override
			public void handleMessage(android.os.Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case msgKey1:
					Calendar cal = Calendar.getInstance();
					String curDateTime = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss").format(cal.getTime());
					mTime.setText(curDateTime);
					break;

				default:
					break;
				}
			}
		};
	}

	public class ReceiveMessageThread extends Thread {
		@Override
		public void run() {
			do {
				// Thread.sleep(1000);
				android.os.Message msg = new android.os.Message();
				msg.what = 2;
				mHandler.sendMessage(msg);
			} while (messageFlag);
		}

		@SuppressLint({ "HandlerLeak", "SimpleDateFormat" })
		private Handler mHandler = new Handler() {
			@Override
			public void handleMessage(android.os.Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case 2:
					// get message listener
					ReceiveMsgListenerConnection(connection);
					Log.i("ReceiveMessageThread", connection.getHost());
					break;

				default:
					break;
				}
			}
		};
	}

	private ArrayList<String> messages = new ArrayList<String>();
	private Handler mHandler = new Handler();
	private ListView listview;
	private EditText textMessage;
	private Button btn_Send;
	// private Button btn_Cancel;
	private ListView friendlistView;
	private PopupWindow popFriends;
	private PopupWindow popStreamingLink;
	private FriendsAdapter friendsAdapter;

	private ArrayList<String> selectedListMap = new ArrayList<String>();

	//

	private void MessageAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem, messages);
		listview.setAdapter(adapter);
	}

	// Select contact function

	private void popupContactList(String entries) {

		// connection = GetConnection(username, password);

		final View v = getLayoutInflater().inflate(R.layout.friendlist, null,
				false);
		int h = getWindowManager().getDefaultDisplay().getHeight();
		int w = getWindowManager().getDefaultDisplay().getWidth();

		friendList = getFriendsList(entries);

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
		friendsAdapter = new FriendsAdapter(this, friendList);

		friendlistView.setAdapter(friendsAdapter);
		friendlistView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long arg3) {
				// TODO Auto-generated method stub
				// TextView name = (TextView)
				// v.findViewById(R.id.friend_username);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_box);
				checkbox.toggle();

				friendsAdapter.getIsSelected().put(position,
						checkbox.isChecked());

				if (checkbox.isChecked()) {
					selectedListMap.add(friendList.get(position)
							.get("username"));

				}
			}
		});

		btn_Send = (Button) v.findViewById(R.id.btn_play);
		btn_Send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// START TO PUSH VIDEO
				PLAYVideoStreaming();
				Log.i("PLAY", "following should be streainglink====");
				if (popFriends != null)
					popFriends.dismiss();

				mHandler.post(new Runnable() {
					public void run() {
						for (int i = 0; i < selectedListMap.size(); i++) {
							Log.i("XMPPChatDemoActivity",
									"Sending text " + streaminglink + " to "
											+ selectedListMap.get(i));
							Message msg = new Message(selectedListMap.get(i),
									Message.Type.chat);
							msg.setBody(streaminglink);
							if (connection != null) {

								connection.sendPacket(msg);
								messages.add(selectedListMap.get(i).split("@")[0]
										+ ":");
								// Log.i("XMPPChatDemoActivity",connection.getUser());
								messages.add(streaminglink);
								Toast.makeText(getApplicationContext(),
										streaminglink, Toast.LENGTH_SHORT)
										.show();
							}
						}
					}
				});
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
						Log.i("XMPPChatDemoActivity", "Text Recieved "
								+ message.getBody() + " from " + fromName[0]);
						messages.add(fromName[0] + ":");
						messages.add(message.getBody());
						final String msg = message.getBody().toString();
						// Add the incoming message to the list view
						Log.i("XMPPChatDemoActivity", msg);
						mHandler.post(new Runnable() {
							public void run() {
								// notification or chat...
								if (msg.contains("rtsp://129.128.184.46:8554/")/*equals(streaminglink)*/)	
									popupReceiveStreamingLinkMessage(msg);
								else 
									Toast.makeText(getApplicationContext(),
											fromName[0] + ": " + msg,
											Toast.LENGTH_SHORT).show();
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

		popStreamingLink = new PopupWindow(v, w - 10, 3 * h / 4);
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
			stopStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onPause() {
		super.onPause();
		stopStream();
	}

	@Override
	public void onStart() {
		super.onStart();
		wl.acquire();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (wl.isHeld())
			wl.release();
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
			String roomName, ArrayList<String> friendlist) {

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

			muc.invite("user1@myria", "come baby");

			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		// int id = item.getItemId();
		// if (id == R.id.action_settings) {
		// startActivityForResult(new Intent(this,
		// SettingsActivity.class),
		// REQUEST_SETTING);
		// return true;
		// }
		return super.onOptionsItemSelected(item);
	}

	private void setStateDescription(byte state) {

		switch (state) {
		case EasyCameraApp.STATE_DISCONNECTED:
			ipView.setText(null);
			break;
		case EasyCameraApp.STATE_CONNECTED:
			ipView.setText(String.format(
					"Input this URL in VLC player:\nrtsp://%s:%d/%s.sdp",
					mAddress, mPort, mVideoName));
			break;
		case EasyCameraApp.STATE_CONNECTING:
			ipView.setText(null);
			break;
		default:
			break;
		}
	}

	@Override
	public void onBitrareUpdate(long bitrate) {
		if (mClient != null) {
			if (bitrate / 1000 > 300)
				ipView.setText("	" + bitrate / 1000 + " kbps");
			else
				ipView.setText(" The current network is not stable ");
		}
	}

	@Override
	public void onRtspUpdate(int message, Exception exception) {
		if (message == RtpThread.WHAT_THREAD_END_UNEXCEPTION) {
			this.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					btnSelectContact.setBackgroundResource(R.drawable.pause);
					alive = true;
					stopStream();
					ipView.setText("Disconnect with server，stop transfer");

				}
			});
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					mReceiver);
			mReceiver = null;
		}
		if (mClient != null) {
			mClient.release();
			mClient.stopStream();
			mClient = null;
		}

		if (mSession != null) {
			mSession.release();
			mSession.stop();
			mSession = null;
		}
	}

	@Override
	public void surfaceCreated(final SurfaceHolder holder) {// Configures the
		// SessionBuilder

		mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (EasyCameraApp.ACTION_COMMOND_STATE_CHANGED.equals(intent
						.getAction())) {
					byte state = intent.getByteExtra(EasyCameraApp.KEY_STATE,
							EasyCameraApp.STATE_DISCONNECTED);
					// setStateDescription(state);

					if (state == EasyCameraApp.STATE_CONNECTED) {
						ipView.setText(String.format("rtsp://%s:%d/%s.sdp",
								mAddress, Integer.parseInt(mPort), mVideoName));
					}

				} else {
					if ("REDIRECT".equals(intent.getAction())) {
						String location = intent.getStringExtra("location");
						if (!TextUtils.isEmpty(location)) {
							// ======================
						}
					} else if ("PAUSE".equals(intent.getAction())) {
						// ==========================
					} else if (ConnectivityManager.CONNECTIVITY_ACTION
							.equals(intent.getAction())) {
						boolean success = false;
						// get the network connection
						ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
						// State state =
						// connManager.getActiveNetworkInfo().getState();
						State state = connManager.getNetworkInfo(
								ConnectivityManager.TYPE_WIFI).getState();
						if (State.CONNECTED == state) {
							success = true;
						}
						state = connManager.getNetworkInfo(
								ConnectivityManager.TYPE_MOBILE).getState();
						if (State.CONNECTED != state) {
							success = true;
						}
						if (success) {
							// startService(new Intent(MainActivity.this,
							// CommandService.class));
							ipView.setText(String.format("rtsp://%s:%d/%s.sdp",
									mAddress, Integer.parseInt(mPort),
									mVideoName));
						}
					}
				}
			}

		};

		ConnectivityManager cm = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);

			mAddress = pref.getString("key_server_address", null);
			mPort = pref.getString("key_server_port", null);
			mVideoName = pref.getString("key_device_id", null);
			boolean bParamInvalid = (TextUtils.isEmpty(mAddress)
					|| TextUtils.isEmpty(mPort) || TextUtils
					.isEmpty(mVideoName));
			if (!bParamInvalid) {
				// startService(new Intent(this, CommandService.class));
				//
				// IntentFilter inf = new
				// IntentFilter(EasyCameraApp.ACTION_COMMOND_STATE_CHANGED);
				// inf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				// inf.addAction("REDIRECT");
				// inf.addAction("PAUSE");
				// LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mReceiver,
				// inf);
				// setStateDescription(EasyCameraApp.sState);
			}
		} else {

			ipView.setText("Network is unavailable,please open the network and try again");
		}

	}

	private final OnSharedPreferenceChangeListener spcl = new OnSharedPreferenceChangeListener() {

		public void onSharedPreferenceChanged(SharedPreferences pre, String key) {
			if (key.equals("p_audio_encoder") || key.equals("p_stream_audio")) {
				audioEncoder = Integer.parseInt(pre.getString(
						"p_audio_encoder", String.valueOf(audioEncoder)));
				SessionBuilder.getInstance().setAudioEncoder(audioEncoder);
				if (!pre.getBoolean("p_stream_audio", true))
					SessionBuilder.getInstance().setAudioEncoder(0);
			}

			else if (key.equals("p_stream_video")
					|| key.equals("p_video_encoder")) {
				videoEncoder = Integer.parseInt(pre.getString(
						"p_video_encoder", String.valueOf(videoEncoder)));
				SessionBuilder.getInstance().setVideoEncoder(videoEncoder);
				if (!pre.getBoolean("p_stream_video", true))
					SessionBuilder.getInstance().setVideoEncoder(0);

			} else if (key.equals("video_resolution")) {
				Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
				Matcher matcher = pattern.matcher(preferences.getString(
						"video_resolution", "320x240"));
				matcher.find();
				//videoQuality.
				Log.i("Integer.parseInt(matcher.group(1))",matcher.group(1));
//				videoQuality.resX = Integer.parseInt(matcher.group(1));
//				videoQuality.resY = Integer.parseInt(matcher.group(2));
			} else if (key.equals("video_framerate")) {
				videoQuality.framerate = Integer.parseInt(preferences
						.getString("video_framerate", "15"));
			} else if (key.equals("video_bitrate")) {
				videoQuality.bitrate = Integer.parseInt(preferences.getString(
						"video_bitrate", "300")) * 1000;
			} else if (key.equals("video_camera")) {
				SessionBuilder.getInstance().setCamera(
						Integer.parseInt(preferences.getString("video_camera",
								"0")));
			}
		}
	};

	public String getDefaultDeviceId() {
		return Build.MODEL.replaceAll(" ", "_");
	}
	
	
	// create a multi-user chat room & invite them to join
	public boolean createMultiUserRoom(XMPPConnection connection,
			String roomName/*, ArrayList<String> friendlist*/) {

		if(connection==null)
			return false;
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

			// configure the room
			// 设置聊天室是持久聊天室，即将要被保存下来  
	        submitForm.setAnswer("muc#roomconfig_persistentroom", false);  
	        // 房间仅对成员开放  
	        submitForm.setAnswer("muc#roomconfig_membersonly", false);  
	        // 允许占有者邀请其他人  
	        submitForm.setAnswer("muc#roomconfig_allowinvites", true);  
	        // 进入是否需要密码  
	        //submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", false);  

	        // 登录房间对话  
	        submitForm.setAnswer("muc#roomconfig_enablelogging", true);  
	        // 仅允许注册的昵称登录  
	        submitForm.setAnswer("x-muc#roomconfig_reservednick", true);  
	        // 允许使用者修改昵称  
	        submitForm.setAnswer("x-muc#roomconfig_canchangenick", true);  
	        // 允许用户注册房间  
	        submitForm.setAnswer("x-muc#roomconfig_registration", false);  
	        // 发送已完成的表单（有默认值）到服务器来配置聊天室  
	        submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
	        
			muc.sendConfigurationForm(submitForm);
			// Create a MultiUserChat using an XMPPConnection for a room
			
			//muc.invite("user11@myria", "come baby");
			muc.join(muc.getNickname());

			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		return false;
	}
	/**
	 * Configure the provider manager
	 * @param pm
	 */
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

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void onSessionError(int reason, int streamType, Exception e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPreviewStarted() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSessionConfigured() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSessionStarted() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSessionStopped() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// nothing to do
			
		} else {
			// nothing to do
		}
	}

	@SuppressWarnings("rawtypes")
	private class GetXMPPConnection extends AsyncTask {
		@Override
		protected XMPPConnection doInBackground(Object... urls) {
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
					connection.login(username, password);
					// Set the status to available
					Presence presence = new Presence(Presence.Type.available);
					connection.sendPacket(presence);
					// get message listener
					ReceiveMsgListenerConnection(connection);
					
				}
//				MultiUserChat muc = new MultiUserChat(connection, "room3"
//						+ "@conference.myria");
//				muc.join("aaaaaa");
				
//				if(createMultiUserRoom(connection,"room3"))
//					Log.i("createMultiUserRoom","sucess");
//				else
//					Log.i("createMultiUserRoom","failure");
				
				return connection;
			} catch (XMPPException e) {
				e.printStackTrace();
			}

			return connection;
		}
	}

	
	//Requested audio with 32kbps at 8kHz
}
