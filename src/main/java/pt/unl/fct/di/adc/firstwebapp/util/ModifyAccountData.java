package pt.unl.fct.di.adc.firstwebapp.util;

public class ModifyAccountData {

    public String username;
    public Attributes attributes;

    public ModifyAccountData() {
    }

    public boolean hasName() {
        return attributes != null && attributes.name != null && !attributes.name.isBlank();
    }

    public boolean hasPhone() {
        return attributes != null && attributes.phone != null && !attributes.phone.isBlank();
    }

    private boolean validPhone() {
        return attributes != null && attributes.phone != null && !attributes.phone.isBlank();
    }

    public String name() {
        return attributes == null ? null : attributes.name;
    }

    public String phone() {
        return attributes == null ? null : attributes.phone;
    }

    public boolean validUpdate() {
        boolean validUsername = username != null && !username.isBlank();
        boolean hasValidPhone = !hasPhone() || validPhone();
        return validUsername && (hasName() || hasPhone()) && hasValidPhone;
    }

    public static class Attributes {
        public String name;
        public String phone;
    }
}
