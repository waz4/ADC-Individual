package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.List;

public class UsersResponse {

    public List<UserRoleResponse> users;

    public UsersResponse(List<UserRoleResponse> users) {
        this.users = users;
    }
}
