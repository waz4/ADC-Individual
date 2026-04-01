package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(15);

	public String tokenId;
	public String username;
	public String role;
	public long issuedAt;
	public long expiresAt;

	public AuthToken() {
	}

	public AuthToken(String username) {
		this(username, "USER");
	}

	public AuthToken(String username, String role) {
		long now = System.currentTimeMillis();
		this.tokenId = UUID.randomUUID().toString();
		this.username = username;
		this.role = role;
		this.issuedAt = now;
		this.expiresAt = now + EXPIRATION_TIME;
	}

	public AuthToken(String tokenId, String username, String role, long issuedAt, long expiresAt) {
		this.tokenId = tokenId;
		this.username = username;
		this.role = role;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public boolean isValid() {
		return hasText(tokenId)
				&& hasText(username)
				&& hasText(role)
				&& issuedAt > 0
				&& expiresAt > issuedAt;
	}

	// So para verificar se os campos existem e nao estao vazios
	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
