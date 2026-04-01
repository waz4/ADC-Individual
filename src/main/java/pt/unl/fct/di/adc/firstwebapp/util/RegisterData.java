package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {
	
	public String username;
	public String password;
	public String confirmation;
	public String phone;
	public String name;
	
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String password, String confirmation, String phone, String name) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.phone = phone;
		this.name = name;
	}
	
	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validRegistration() {
		

		return nonEmptyOrBlankField(username) &&
			   nonEmptyOrBlankField(password) &&
				nonEmptyOrBlankField(phone) &&
				nonEmptyOrBlankField(name) &&
			   	password.equals(confirmation);
	}
}
