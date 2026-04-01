package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.List;

public class SessionsResponse {

    public List<SessionResponse> sessions;

    public SessionsResponse(List<SessionResponse> sessions) {
        this.sessions = sessions;
    }
}
