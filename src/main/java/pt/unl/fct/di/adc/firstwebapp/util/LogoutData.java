package pt.unl.fct.di.adc.firstwebapp.util;

public class LogoutData {

    public String username;

    public LogoutData() {
    }

    public boolean hasValidUsername() {
        return username != null && !username.isBlank();
    }
}
