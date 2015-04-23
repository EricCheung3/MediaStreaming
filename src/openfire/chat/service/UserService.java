package openfire.chat.service;

import org.jivesoftware.smack.XMPPConnection;

public interface UserService {

	public XMPPConnection userLogin(String email, String password) throws Exception;
	
	public XMPPConnection userRegister(String username,String name, String email, String password,String confirmPasswd) throws Exception;

	public XMPPConnection GetConnection();
}
