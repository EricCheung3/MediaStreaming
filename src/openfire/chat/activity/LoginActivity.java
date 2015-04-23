package openfire.chat.activity;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import openfire.chat.service.ServiceException;
import openfire.chat.service.UserService;
import openfire.chat.service.UserServiceImpl;

import org.easydarwin.android.camera.R;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;
import easydarwin.android.videostreaming.VideoStreamingActivity;

public class LoginActivity extends Activity implements OnClickListener {

	public static final int LOGIN_SUCCESS = 1;
	public final static String LOGIN_SUCCESSFULL = "login success";
	public static final String URL_DISCONNECTION = "Application Error! Response isn't ok!";
	public static final String USER_PASSWORD_NULL = "username or password is null!";
	public static final String USERNAME_EMAIL_NOT_EXIST = "username or email is not exist (registed)!";
	public static final String LOGIN_FAILED = "Connect time out,check network and login again!";
	public static final String PASSWORD_IS_WRONG = "password is wrong !";
	public static final String UNKNOW_ERROR = "unknow error!";
	public static final String CONNECT_TIME_OUT = "Connect time out, please check the network!";
	public static final String REQUEST_TIME_OUT = "Request time out, try again later!!";
	public static final String HTTP_CONNECTION_REFUSED = "http://... refused, please login later.";
	
	private Button loginBtn;
	private Button registerBtn;
	private EditText inputUsername;
	private EditText inputPassword;
	private CheckBox cheRempwd;
	private static ProgressDialog mDialog;
	public SharedPreferences sp;
	private UserService userService = new UserServiceImpl();

	boolean registerFlag;
	private XMPPConnection connection;

	private PackageInfo info;
	private String username;
	private String password;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		initView();
		sp = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);

		username = inputUsername.getText().toString();
		password = inputPassword.getText().toString();
		
		if (sp.getBoolean("ISCHECK", false)) {
			// set checked default is true
			cheRempwd.setChecked(true);
			inputUsername.setText(sp.getString("USERNAME", username));
			inputPassword.setText(sp.getString("PASSWORD", password));
		}

	}

	public void initView() {

		loginBtn = (Button) findViewById(R.id.btn_login);
		registerBtn = (Button) findViewById(R.id.btn_register);
		inputUsername = (EditText) findViewById(R.id.edit_username);
		inputPassword = (EditText) findViewById(R.id.edit_password);
		cheRempwd = (CheckBox) findViewById(R.id.cb_savepwd);
		loginBtn.setOnClickListener(this);
		registerBtn.setOnClickListener(this);
		cheRempwd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (cheRempwd.isChecked()) {
					sp.edit().putBoolean("ISCHECK", true).commit();
				} else {
					sp.edit().putBoolean("ISCHECK", false).commit();
				}
			}
		});

		CheckNetworkState();

		/** register success, get the username & password. */
		if (!registerFlag) {
			Intent intent = getIntent();
			inputUsername.setText(intent.getStringExtra("username"));
			inputPassword.setText(intent.getStringExtra("password"));
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			login();
			break;
		case R.id.btn_register:
			register();
			break;
		default:
			break;
		}
	}

	private void register() {
		sp.edit().putBoolean("firstStart", true);
		sp.edit().commit();
		Intent intent = new Intent(this, RegisterActivity.class);
		startActivity(intent);
		intent = null;
	}

	private void login() {

		username = inputUsername.getText().toString();
		password = inputPassword.getText().toString();

		mDialog = new ProgressDialog(LoginActivity.this);
		mDialog.setTitle("login");
		mDialog.setMessage("loading......");
		mDialog.show();
		Thread loginThread = new Thread(new LoginThread());
		loginThread.start();

	}

	// LoginThread
	List<Map<String,String>> listMap = new ArrayList<Map<String,String>>();
	private class LoginThread implements Runnable {
		@Override
		public void run() {

			try {

				connection = userService.userLogin(username, password);

			} catch (ServiceException e) {
				e.printStackTrace();
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putSerializable("ErrorMsg", e.getMessage());
				msg.setData(data);
				handler.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();

			}
			if (connection!=null) {
				handler.sendEmptyMessage(LOGIN_SUCCESS);
				if (cheRempwd.isChecked()) {
					// remember username & paw
					Editor editor = sp.edit();
					editor.putString("USERNAME", username);
					editor.putString("PASSWORD", password);
					editor.commit();
				}
				// Set the status to available
				Presence presence = new Presence(Presence.Type.available);
				connection.sendPacket(presence);
/*				
				Roster roster = connection.getRoster();
				Object[] entries = roster.getEntries().toArray();
				for (Object entry : entries) {
					Map<String,String> map = new HashMap<String,String>();
					
					String[] s = entry.toString().split(" ");
					Log.i("s[0]", s[0]);
					Log.i("s[1]", s[1]);
					if(s.length==2){
						map.put("name", s[0]);
						map.put("username", s[1]);
					}else if(s.length==3){
						map.put("name", s[0]);
						map.put("username", s[1]);
						map.put("group", s[2]);
					}
					listMap.add(map);
				}
				System.out.println(listMap.toString());
*/
//				Collection<RosterEntry> entries = roster.getEntries();
//				for (RosterEntry entry : entries) {
//					Log.i("RosterEntry",
//							"------"+entry.toString()+"----");
//					//user4: user4@myria [myFriends]
//					Log.i("RosterEntry","getUser: " + entry.getUser());//user4@myria
//					Log.i("RosterEntry","getGroups: " + entry.getGroups().toString());
//					Log.i("RosterEntry","getName: " + entry.getName());//user4
//				}
				//Log.i("connection","getUser: " + connection.getUser());//user2@myria/Smack

				Roster roster = connection.getRoster();
				String entries = roster.getEntries().toString();
				Log.i("entries",entries);
				Intent intent = new Intent();
				intent.putExtra("username", username);
				intent.putExtra("password", password);
				intent.putExtra("entries", entries);
				intent.setClass(LoginActivity.this, VideoStreamingActivity.class);
				startActivity(intent);
				LoginActivity.this.finish();
			}

		}

	}

	private static class IHandler extends Handler {
		private final WeakReference<Activity> mActivity;

		public IHandler(LoginActivity loginActivity) {
			mActivity = new WeakReference<Activity>(loginActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			if (mDialog != null)
				mDialog.dismiss();
			switch (msg.what) {
			case 0:
				String message = (String) msg.getData().getSerializable(
						"ErrorMsg");
				((LoginActivity) mActivity.get()).showInfo(message);
				break;
			case LOGIN_SUCCESS:
				// ((RegisterActivity)mActivity.get()).showInfo(REGISTER_SUCCESSFULL);
				Toast.makeText((LoginActivity) mActivity.get(),
						LOGIN_SUCCESSFULL, Toast.LENGTH_SHORT).show();

				break;

			default:
				break;
			}
		}
	};

	private IHandler handler = new IHandler(this);

	private void showInfo(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	// check network state function
	public void CheckNetworkState() {
		// /boolean flag = false;
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		State mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState();
		State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		// if 3G, wifi or 2G network is connected, return; else setting network
		if (mobile == State.CONNECTED || mobile == State.CONNECTING)
			return;
		if (wifi == State.CONNECTED || wifi == State.CONNECTING)
			return;
		showNeworkTips();
	}

	private void showNeworkTips() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("Network Disconnected");
		builder.setMessage("Current network is unavailable, set network?");
		builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// setting network activity
				startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
			}
		});
		builder.setNegativeButton("cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						// LoginActivity.this.finish();
					}
				});
		builder.create();
		builder.show();
	}
}
