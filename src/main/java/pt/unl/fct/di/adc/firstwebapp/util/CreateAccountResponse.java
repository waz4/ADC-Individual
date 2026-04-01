package pt.unl.fct.di.adc.firstwebapp.util;

public class CreateAccountResponse {

    public String username;
    public String role;

    public CreateAccountResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }
}
