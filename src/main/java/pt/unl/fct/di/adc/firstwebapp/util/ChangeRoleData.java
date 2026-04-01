package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeRoleData {

    public String username;
    public String role;

    public ChangeRoleData() {
    }

    public boolean validChange() {
        return username != null && !username.isBlank() && role != null && !role.isBlank();
    }
}
