package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePasswordData {

    public String username;
    public String oldPassword;
    public String newPassword;

    public ChangePasswordData() {
    }

    public boolean validChange() {
        return username != null
                && !username.isBlank()
                && oldPassword != null
                && !oldPassword.isBlank()
                && newPassword != null
                && !newPassword.isBlank();
    }
}
