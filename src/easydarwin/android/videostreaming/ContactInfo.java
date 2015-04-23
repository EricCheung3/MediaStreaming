package easydarwin.android.videostreaming;

public class ContactInfo {
	private String contactName;
	private String userNumber;
	private String contactEmail;
	private boolean isChecked;
	
//	ContactInfo(){}
//	ContactInfo(String n, String E, String P){
//		this.contactName = n;
//		this.contactEmail = n;
//		this.userNumber = n;					
//	}
	


	public String getContactName() {
		return contactName;
	}
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}
	public String getUserNumber() {
		return userNumber;
	}
	public void setUserNumber(String userNumber) {
		this.userNumber = userNumber;
	}
	public String getContactEmail() {
		return contactEmail;
	}
	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}
	public boolean getChecked() {
		return isChecked;
	}
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
	
}