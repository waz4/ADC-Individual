package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginResponse {

    public AuthToken token;

    public LoginResponse(AuthToken token) {
        this.token = token;
    }
}
