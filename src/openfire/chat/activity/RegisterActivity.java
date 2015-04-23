package openfire.chat.activity;


import java.lang.ref.WeakReference;

import openfire.chat.service.ServiceException;
import openfire.chat.service.UserService;
import openfire.chat.service.UserServiceImpl;

import org.easydarwin.android.camera.R;
import org.jivesoftware.smack.XMPPConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {

	public final static int REGISTER_SUCCESS = 1;
	
	public final static String REGISTER_SUCCESSFULL = "Register success, use your account to login!";
	public final static String REGISTER_FAILED = "Register failed";
	public final static String REGISTER_EMAIL_FAILED = "Email cannot be null!!";
	public final static String REGISTER_PASSWORD_DIFF = "Two password is not same,please try again!";
	public final static String REGISTER_PASSWORD_LENGTH = "Password length should between 6-20!";
	public final static String REGISTER_RESPONSE_ERROR = "Server is error";

	public static final String REGISTER_USERNAME_FAILED = "Username cannot be null!";
	public static final String USERNAME_NOT_VALIDATE = "Username can be letters, digits ,_ or together and at least 6 characters!";
	public static final String EMAIL_NOT_VALIDATE = "Email format is not right!";
	public static final String URL_DISCONNECTION = "URL is disconnected!";
	public static final String UNKNOW_ERROR = "No response from server";
	public static final String INCOMPLETE_INFO = "The username or email cannot be null!";
	public static final String MYSQL_ERROR = "Mysql error!";
	public static final String USERNAME_EXISTED = "Sorry, the username is already existed!";
	public static final String EMAIL_EXISTED = "Sorry, the email is already existed!";
	
	private XMPPConnection connection;
	
	
	private UserService userService = new UserServiceImpl();
	private EditText edit_username;
	private EditText edit_displayName;
	private EditText edit_email;
	private EditText edit_password;
	private EditText edit_confirmPassword;
	private Button btn_register;
	private Button btn_reset;
	private static ProgressDialog mDialog;

	private void initView() {
		this.edit_username = (EditText) findViewById(R.id.edit_username);
		this.edit_displayName = (EditText) findViewById(R.id.edit_name);
		this.edit_email = (EditText) findViewById(R.id.edit_email);
		this.edit_password = (EditText) findViewById(R.id.edit_password);
		this.edit_confirmPassword = (EditText) findViewById(R.id.edit_confirmPassword);
		this.btn_register = (Button) findViewById(R.id.btn_register);
		this.btn_reset = (Button) findViewById(R.id.btn_reset);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);

		this.initView();

		btn_register.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mDialog = new ProgressDialog(RegisterActivity.this);
				mDialog.setTitle("register");
				mDialog.setMessage("loading......");
				mDialog.show();
				Thread registerThread = new Thread(new registerThread());
				registerThread.start();
				
			}
		});
		
		btn_reset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				edit_username.setText("");
				edit_displayName.setText("");
				edit_email.setText("");
				edit_password.setText("");
				edit_confirmPassword.setText("");
			}
		});
	}

	// RegisterThread
	private class registerThread implements Runnable {

		@Override
		public void run() {
			String username = edit_username.getText().toString();
			String displayname = edit_displayName.getText().toString();
			String email = edit_email.getText().toString();
			String password = edit_password.getText().toString();
			String confirmPassword = edit_confirmPassword.getText().toString();
			try {
				connection = userService.userRegister(username, displayname,email, password,confirmPassword);								
			
			}catch(ServiceException e){
				e.printStackTrace();
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putSerializable("ErrorMsg", e.getMessage());
				msg.setData(data);
				handler.sendMessage(msg);
			}catch (Exception e) {			
				e.printStackTrace();
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putSerializable("ErrorMsg", REGISTER_FAILED);
				msg.setData(data);
				handler.sendMessage(msg);
			}
			if(connection!=null){
				handler.sendEmptyMessage(REGISTER_SUCCESS);
				Intent intent = new Intent();
				Bundle data = new Bundle();
				data.putString("username", username);  
				data.putString("password", password);  
				intent.putExtras(data);  	
				intent.setClass(RegisterActivity.this,LoginActivity.class);  
				startActivity(intent);
				intent = null;
				RegisterActivity.this.finish();

			}else{
				try {
					throw new ServiceException(REGISTER_FAILED);
				} catch (ServiceException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

	// Handler
	private static class IHandler extends Handler{
		private final WeakReference<Activity> mActivity;
		public IHandler(RegisterActivity activity){
			mActivity = new WeakReference<Activity>(activity);
		}
		@Override
		public void handleMessage(Message msg) {
			if(mDialog!=null)
				mDialog.dismiss();
			
			switch (msg.what) {
			case 0:
				String message = (String) msg.getData().getSerializable("ErrorMsg");
				((RegisterActivity)mActivity.get()).showInfo(message);
				break;
				
			case REGISTER_SUCCESS:
				//((RegisterActivity)mActivity.get()).showInfo(REGISTER_SUCCESSFULL);
				Toast.makeText((RegisterActivity)mActivity.get(), REGISTER_SUCCESSFULL, Toast.LENGTH_LONG).show();
				break;
				
			default:
				break;
			}
		}
	};
	private IHandler handler = new IHandler(this); 
	
	
	private void showInfo(String str){		
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
}
