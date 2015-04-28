package openfire.chat.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openfire.chat.activity.RegisterActivity;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;

import android.text.TextUtils;
import android.util.Log;

public class UserServiceImpl implements UserService {

	public static final int SERVER_PORT = 5220; //server port
	public static String SERVER_HOST = "129.128.184.46";// server ip
	public static String SERVER_NAME = "myria";// server name
	private static XMPPConnection connection = null;

	@Override
	public XMPPConnection userLogin(String username, String password)
			throws Exception {

		try {
			connection = GetConnection();
			connection.login(username, password);
			
			return connection;
		} catch (XMPPException xe) {
			Log.e("XMPPChatDemoActivity", "Failed to log in as " + username);
			Log.e("XMPPChatDemoActivity", xe.toString());
		}

		return null;
	}

	public XMPPConnection userRegister(String username, String name, String email, String password,
			String confirmPassword) throws Exception {

		if (TextUtils.isEmpty(username)) {// username == null ||
											// username.equals("")
			throw new ServiceException(
					RegisterActivity.REGISTER_USERNAME_FAILED);
		}
		if (TextUtils.isEmpty(email)) {
			throw new ServiceException(RegisterActivity.REGISTER_EMAIL_FAILED);
		}
		if (password.length() < 6 || password.length() > 20) {
			throw new ServiceException(
					RegisterActivity.REGISTER_PASSWORD_LENGTH);
		}
		if (!password.equals(confirmPassword)) {
			throw new ServiceException(RegisterActivity.REGISTER_PASSWORD_DIFF);
		}
		if (!(validteUsername(username))) {
			throw new ServiceException(RegisterActivity.USERNAME_NOT_VALIDATE);
		}
		if (!(validteEmail(email))) {
			throw new ServiceException(RegisterActivity.EMAIL_NOT_VALIDATE);
		}

		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
//		if (null == connection || !connection.isAuthenticated()) {
//			XMPPConnection.DEBUG_ENABLED = true;
//
//			ConnectionConfiguration config = new ConnectionConfiguration(
//					SERVER_HOST, SERVER_PORT, SERVER_NAME);
//			config.setReconnectionAllowed(true);
//			config.setSendPresence(true);
//			config.setSASLAuthenticationEnabled(true);
//			connection = new XMPPConnection(config);
//			connection.connect();
//		}
		connection = GetConnection();
		
		reg.setTo(connection.getServiceName());
		reg.setUsername(username);
		reg.setPassword(password);
		reg.addAttribute("name", name);
		reg.addAttribute("email", email);

		reg.addAttribute("android", "geolo_createUser_android");
		PacketFilter filter = new AndFilter(new PacketIDFilter(
				reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = connection.createPacketCollector(filter);
		connection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration
				.getPacketReplyTimeout());
		// Stop querying and to get results
		collector.cancel();

		if (result == null) {
			throw new ServiceException(RegisterActivity.UNKNOW_ERROR);
		} else if (result.getType() == IQ.Type.ERROR) {
			if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
				throw new ServiceException(
						RegisterActivity.REGISTER_USERNAME_FAILED);
			} else {
				throw new ServiceException(RegisterActivity.REGISTER_FAILED);
			}
		} else if (result.getType() == IQ.Type.RESULT) {
			return connection;
		}

		return null;
	}

	public XMPPConnection GetConnection(){
		try {
			if (null == connection || !connection.isAuthenticated()) {
				XMPPConnection.DEBUG_ENABLED = true;

				ConnectionConfiguration config = new ConnectionConfiguration(
						SERVER_HOST, SERVER_PORT, SERVER_NAME);
				config.setReconnectionAllowed(true);
				config.setSendPresence(true);
				config.setSASLAuthenticationEnabled(true);
				connection = new XMPPConnection(config);
				connection.connect();

				return connection;
			}
		} catch (XMPPException xe) {
			Log.e("XMPPChatDemoActivity", xe.toString());
		}
		return null;
	}
	/** check validate of username & email */
	private boolean validteUsername(String username) {
		String strUser = "^[a-zA-Z]|[0-9]|[a-zA-Z0-9]|[a-zA-Z0-9_]{5,20}";
		Pattern pUser = Pattern.compile(strUser);
		Matcher mUser = pUser.matcher(username);
		return mUser.matches();
	}

	private boolean validteEmail(String email) {
		String strPattern = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
		Pattern pEmail = Pattern.compile(strPattern);
		Matcher mEmail = pEmail.matcher(email);
		return mEmail.matches();
	}

}
