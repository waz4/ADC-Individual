package pt.unl.fct.di.adc.firstwebapp.util;

public class UserData {

    public String username;

    public UserData() {
    }

    public boolean hasValidUsername() {
        return username != null && !username.isBlank();
    }
}
