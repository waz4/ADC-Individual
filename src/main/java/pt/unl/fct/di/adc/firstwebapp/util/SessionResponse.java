package pt.unl.fct.di.adc.firstwebapp.util;

public class SessionResponse {

    public String tokenId;
    public String username;
    public String role;
    public long expiresAt;

    public SessionResponse(String tokenId, String username, String role, long expiresAt) {
        this.tokenId = tokenId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }
}
