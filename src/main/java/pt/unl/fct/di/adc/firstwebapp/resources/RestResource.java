package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.ChangePasswordData;
import pt.unl.fct.di.adc.firstwebapp.util.ChangeRoleData;
import pt.unl.fct.di.adc.firstwebapp.util.ChangeUserRoleResponse;
import pt.unl.fct.di.adc.firstwebapp.util.CreateAccountResponse;
import pt.unl.fct.di.adc.firstwebapp.util.DeleteAccountResponse;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.LoginResponse;
import pt.unl.fct.di.adc.firstwebapp.util.LogoutData;
import pt.unl.fct.di.adc.firstwebapp.util.LogoutResponse;
import pt.unl.fct.di.adc.firstwebapp.util.ModifyAccountData;
import pt.unl.fct.di.adc.firstwebapp.util.ModifyAccountResponse;
import pt.unl.fct.di.adc.firstwebapp.util.PasswordChangeResponse;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SessionResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SessionsResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UserData;
import pt.unl.fct.di.adc.firstwebapp.util.UserRoleResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UsersResponse;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class RestResource {

    private static final String ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final int ERROR_CODE_INVALID_CREDENTIALS = 9900;
    private static final String MESSAGE_INVALID_CREDENTIALS = "The username-password pair is not valid.";

    private static final String ERROR_USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    private static final int ERROR_CODE_USER_ALREADY_EXISTS = 9901;
    private static final String MESSAGE_USER_ALREADY_EXISTS = "Error in creating an account because the username already exists.";

    private static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final int ERROR_CODE_USER_NOT_FOUND = 9902;
    private static final String MESSAGE_USER_NOT_FOUND = "The username referred in the operation doesn’t exist in registered accounts.";

    private static final String ERROR_INVALID_TOKEN = "INVALID_TOKEN";
    private static final int ERROR_CODE_INVALID_TOKEN = 9903;
    private static final String MESSAGE_INVALID_TOKEN = "The operation is called with an invalid token (wrong format for example).";

    private static final String ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    private static final int ERROR_CODE_TOKEN_EXPIRED = 9904;
    private static final String MESSAGE_TOKEN_EXPIRED = "The operation is called with a token that is expired.";

    private static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    private static final int ERROR_CODE_UNAUTHORIZED = 9905;
    private static final String MESSAGE_UNAUTHORIZED = "The operation is not allowed for the user role.";

    private static final String ERROR_INVALID_INPUT = "INVALID_INPUT";
    private static final int ERROR_CODE_INVALID_INPUT = 9906;
    private static final String MESSAGE_INVALID_INPUT = "The call is using input data not following the correct specification.";

    private static final String ERROR_FORBIDDEN = "FORBIDDEN";
    private static final int ERROR_CODE_FORBIDDEN = 9907;
    private static final String MESSAGE_FORBIDDEN = "The operation generated a forbidden error by other reason.";

    private static final Logger LOG = Logger.getLogger(RestResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final String USER_KIND = "User";
    private static final String SESSION_KIND = "Session";

    private static final String USER_NAME = "user_name";
    private static final String USER_PHONE = "user_phone";
    private static final String USER_PASSWORD = "user_pwd";
    private static final String USER_ROLE = "user_role";
    private static final String USER_CREATION_TIME = "user_creation_time";

    private static final String SESSION_USERNAME = "session_username";
    private static final String SESSION_ISSUED_AT = "issued_at";
    private static final String SESSION_EXPIRES_AT = "expires_at";

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BACKOFFICE = "BOFFICER";
    private final Gson g = new Gson();

    @POST
    @Path("/createaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(ApiRequest<RegisterData> request) {
        try {
            if (request == null || request.input == null || !request.input.validRegistration()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            RegisterData registerData = request.input;
            String username = trim(registerData.username);
            Key userKey = userKey(username);
            String assignedRole = !hasRegisteredUsers() ? ROLE_ADMIN : ROLE_USER;

            Transaction txn = datastore.newTransaction();
            try {
                Entity existingUser = txn.get(userKey);
                if (existingUser != null) {
                    return errorResponse(ERROR_CODE_USER_ALREADY_EXISTS, ERROR_USER_ALREADY_EXISTS, MESSAGE_USER_ALREADY_EXISTS);
                }

                Entity newUser = Entity.newBuilder(userKey)
                        .set(USER_NAME, trim(registerData.name))
                        .set(USER_PHONE, trim(registerData.phone))
                        .set(USER_PASSWORD, DigestUtils.sha512Hex(registerData.password))
                        .set(USER_ROLE, assignedRole)
                        .set(USER_CREATION_TIME, Timestamp.now())
                        .build();

                txn.put(newUser);
                txn.commit();

                LOG.info("created account for user " + username + " with role " + assignedRole);
                return successResponse(new CreateAccountResponse(username, assignedRole));
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                }
            }
        } catch (Exception e) {
            LOG.severe("Unexpected error on createacount: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(ApiRequest<LoginData> request) {
        try {
            if (request == null || request.input == null || !request.input.validLogin()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            LoginData loginData = request.input;
            String username = trim(loginData.username);
            Entity user = datastore.get(userKey(username));

            if (user == null) {
                return errorResponse(ERROR_CODE_INVALID_CREDENTIALS, ERROR_INVALID_CREDENTIALS, MESSAGE_INVALID_CREDENTIALS);
            }

            String databaseHash = user.getString(USER_PASSWORD);
            String receivedHash = DigestUtils.sha512Hex(loginData.password);
            if (!databaseHash.equals(receivedHash)) {
                return errorResponse(ERROR_CODE_INVALID_CREDENTIALS, ERROR_INVALID_CREDENTIALS, MESSAGE_INVALID_CREDENTIALS);
            }

            AuthToken token = new AuthToken(username, getUserRole(user));
            Entity sessionEntity = Entity.newBuilder(sessionKey(token.tokenId))
                        .set(SESSION_USERNAME, token.username)
                        .set(SESSION_ISSUED_AT, token.issuedAt)
                        .set(SESSION_EXPIRES_AT, token.expiresAt)
                        .build();
            datastore.put(sessionEntity);

            LOG.info("Login successful for user " + username);
            return successResponse(new LoginResponse(token));
        } catch (Exception e) {
            LOG.severe("Unexpected error on login: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/showusers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(ApiRequest<Object> request) {
        try {
            if (request == null) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            if (!hasAnyRole(auth.currentRole(), List.of(ROLE_ADMIN, ROLE_BACKOFFICE) ) ) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind(USER_KIND)
                    .setOrderBy(OrderBy.asc(USER_CREATION_TIME))
                    .build();

            QueryResults<Entity> results = datastore.run(query);
            List<UserRoleResponse> users = new ArrayList<>();
            while (results.hasNext()) {
                Entity user = results.next();
                String username = user.getKey().getName();
                users.add(new UserRoleResponse(username, getUserRole(user)));
            }

            return successResponse(new UsersResponse(users));
        } catch (Exception e) {
            LOG.severe("Unexpected error on showusers: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/deleteaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAccount(ApiRequest<UserData> request) {
        try {
            if (request == null || request.input == null || !request.input.hasValidUsername()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            if (!ROLE_ADMIN.equals(auth.currentRole())) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            UserData userData = request.input;
            String username = trim(userData.username);
            Key targetKey = userKey(username);

            Transaction txn = datastore.newTransaction();
            try {
                Entity targetUser = txn.get(targetKey);
                if (targetUser == null) {
                    return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
                }

                txn.delete(targetKey);
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                }
            }

            deleteSessionsForUser(username);

            return successResponse(new DeleteAccountResponse("Account deleted successfully"));
        } catch (Exception e) {
            LOG.severe("Unexpected error on deleteaccount: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/modaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAccount(ApiRequest<ModifyAccountData> request) {
        try {
            if (request == null || request.input == null || !request.input.validUpdate()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            ModifyAccountData modifyAccountData = request.input;
            String username = trim(modifyAccountData.username);
            Entity user = datastore.get(userKey(username));
            if (user == null) {
                return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
            }

            boolean canModifyOwnAccount = auth.username().equals(username);
            boolean canModifyOthers = hasAnyRole(auth.currentRole(), List.of(ROLE_ADMIN, ROLE_BACKOFFICE));

            // checks if user is changing its own account or is ADMIN/BOffice
            if (!canModifyOwnAccount && !canModifyOthers) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            // check if its a Bofficer trying to change an ADMIN account
            if (ROLE_BACKOFFICE.equals(auth.currentRole()) && ROLE_ADMIN.equals(getUserRole(user))) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set(USER_NAME, modifyAccountData.hasName() ? trim(modifyAccountData.name()) : getString(user, USER_NAME))
                    .set(USER_PHONE, modifyAccountData.hasPhone() ? trim(modifyAccountData.phone()) : getString(user, USER_PHONE))
                    .build();

            datastore.put(updatedUser);
            return successResponse(new ModifyAccountResponse("Updated successfully"));
        } catch (Exception e) {
            LOG.severe("Unexpected error on modaccount: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/showauthsessions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(ApiRequest<Object> request) {
        try {
            if (request == null) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            if (!ROLE_ADMIN.equals(auth.currentRole())) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind(SESSION_KIND)
                    .setOrderBy(OrderBy.desc(SESSION_ISSUED_AT))
                    .build();

            QueryResults<Entity> results = datastore.run(query);
            List<SessionResponse> sessions = new ArrayList<>();
            while (results.hasNext()) {
                Entity session = results.next();
                AuthToken token = tokenFromSession(session);
                if (isExpired(token)) {
                    datastore.delete(session.getKey());
                    continue;
                }

                Entity user = datastore.get(userKey(token.username));
                if (user == null) {
                    datastore.delete(session.getKey());
                    continue;
                }

                sessions.add(new SessionResponse(token.tokenId, token.username, getUserRole(user), token.expiresAt));
            }

            return successResponse(new SessionsResponse(sessions));
        } catch (Exception e) {
            LOG.severe("Unexpected eror on showauthsessions: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/showuserrole")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUserRole(ApiRequest<UserData> request) {
        try {
            if (request == null || request.input == null || !request.input.hasValidUsername()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            if (!hasAnyRole(auth.currentRole(), List.of(ROLE_ADMIN, ROLE_BACKOFFICE) ) ) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            UserData userData = request.input;
            String username = trim(userData.username);
            Entity user = datastore.get(userKey(username));
            if (user == null) {
                return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
            }

            return successResponse(new UserRoleResponse(username, getUserRole(user)));
        } catch (Exception e) {
            LOG.severe("Unexpected error on showuserrole: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/changeuserrole")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserRole(ApiRequest<ChangeRoleData> request) {
        try {
            if (request == null || request.input == null || !request.input.validChange()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            if (!ROLE_ADMIN.equals(auth.currentRole())) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            ChangeRoleData changeRoleData = request.input;
            String username = trim(changeRoleData.username);
            String newRole = normalizeRole(changeRoleData.role);
            if (!isSupportedRole(newRole)) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            Entity user = datastore.get(userKey(username));
            if (user == null) {
                return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set(USER_ROLE, newRole)
                    .build();

            datastore.put(updatedUser);

            return successResponse(new ChangeUserRoleResponse("Role updated successfully"));
        } catch (Exception e) {
            LOG.severe("Unexpected error on changeuserole: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/changeuserpwd")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserPassword(ApiRequest<ChangePasswordData> request) {
        try {
            if (request == null || request.input == null || !request.input.validChange()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            ChangePasswordData changePasswordData = request.input;
            String username = trim(changePasswordData.username);
            if (!auth.username().equals(username)) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            Entity user = datastore.get(userKey(username));
            if (user == null) {
                return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
            }

            String databaseHash = user.getString(USER_PASSWORD);
            String receivedHash = DigestUtils.sha512Hex(changePasswordData.oldPassword);
            if (!databaseHash.equals(receivedHash)) {
                return errorResponse(ERROR_CODE_INVALID_CREDENTIALS, ERROR_INVALID_CREDENTIALS, MESSAGE_INVALID_CREDENTIALS);
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set(USER_PASSWORD, DigestUtils.sha512Hex(changePasswordData.newPassword))
                    .build();

            datastore.put(updatedUser);

            return successResponse(new PasswordChangeResponse("Password changed successfully"));
        } catch (Exception e) {
            LOG.severe("Unexpected error on changeuserpwd: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(ApiRequest<LogoutData> request) {
        try {
            if (request == null || request.input == null || !request.input.hasValidUsername()) {
                return errorResponse(ERROR_CODE_INVALID_INPUT, ERROR_INVALID_INPUT, MESSAGE_INVALID_INPUT);
            }

            TokenValidationResult auth = authenticate(request.token);
            if (!auth.valid()) {
                return auth.toResponse();
            }

            LogoutData logoutData = request.input;
            String targetUsername = trim(logoutData.username);
            Entity targetUser = datastore.get(userKey(targetUsername));
            if (targetUser == null) {
                return errorResponse(ERROR_CODE_USER_NOT_FOUND, ERROR_USER_NOT_FOUND, MESSAGE_USER_NOT_FOUND);
            }

            if (!auth.username().equals(targetUsername) && !ROLE_ADMIN.equals(auth.currentRole())) {
                return errorResponse(ERROR_CODE_UNAUTHORIZED, ERROR_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            }

            deleteSessionsForUser(targetUsername);

            return successResponse(new LogoutResponse("Logout successful"));
        } catch (Exception e) {
            LOG.severe("Unexpected error on logout: " + e.getMessage());
            return errorResponse(ERROR_CODE_FORBIDDEN, ERROR_FORBIDDEN, MESSAGE_FORBIDDEN);
        }
    }

    private TokenValidationResult authenticate(AuthToken requestToken) {
        //token null ou vazio
        if (requestToken == null || !requestToken.isValid()) {
            return TokenValidationResult.invalid(ERROR_CODE_INVALID_TOKEN, ERROR_INVALID_TOKEN, MESSAGE_INVALID_TOKEN);
        }

        Entity session = datastore.get(sessionKey(requestToken.tokenId));

        // Token nao existe na BD
        if (session == null) {
            return TokenValidationResult.invalid(ERROR_CODE_INVALID_TOKEN, ERROR_INVALID_TOKEN, MESSAGE_INVALID_TOKEN);
        }

        AuthToken storedToken = tokenFromSession(session);

        // Token esta expirado
        if (isExpired(storedToken)) {
            datastore.delete(session.getKey());
            return TokenValidationResult.invalid(ERROR_CODE_TOKEN_EXPIRED, ERROR_TOKEN_EXPIRED, MESSAGE_TOKEN_EXPIRED);
        }

        // Dados do token nao condizem com os dados da entry da BD associada ao token
        if (!storedToken.username.equals(trim(requestToken.username))
                || storedToken.issuedAt != requestToken.issuedAt
                || storedToken.expiresAt != requestToken.expiresAt) {
            return TokenValidationResult.invalid(ERROR_CODE_INVALID_TOKEN, ERROR_INVALID_TOKEN, MESSAGE_INVALID_TOKEN);
        }


        Entity user = datastore.get(userKey(storedToken.username));
        // Utilizador associado ao token nao existe na base de dados ex: se for apagado a meio da sessao
        if (user == null) {
            datastore.delete(session.getKey());
            return TokenValidationResult.invalid(ERROR_CODE_INVALID_TOKEN, ERROR_INVALID_TOKEN, MESSAGE_INVALID_TOKEN);
        }

        String currentRole = getUserRole(user);
        AuthToken authoritativeToken = new AuthToken(storedToken.tokenId, storedToken.username, currentRole, storedToken.issuedAt, storedToken.expiresAt);

        return TokenValidationResult.valid(authoritativeToken, currentRole);
    }


    private AuthToken tokenFromSession(Entity session) {
        String tokenId = session.getKey().getName();
        String username = session.getString(SESSION_USERNAME);
        long issuedAt = session.getLong(SESSION_ISSUED_AT);
        long expiresAt = session.getLong(SESSION_EXPIRES_AT);

        Entity user = datastore.get(userKey(username));
        String role = user == null ? ROLE_USER : getUserRole(user);
        return new AuthToken(tokenId, username, role, issuedAt, expiresAt);
    }

    private void deleteSessionsForUser(String username) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(SESSION_KIND)
                .setFilter(PropertyFilter.eq(SESSION_USERNAME, username))
                .build();

        QueryResults<Entity> results = datastore.run(query);

        while (results.hasNext()) {
            Entity session = results.next();
            datastore.delete(session.getKey());
        }
    }

    private boolean hasRegisteredUsers() {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USER_KIND)
                .setLimit(1)
                .build();
        return datastore.run(query).hasNext();
    }

    private Key userKey(String username) {
        return datastore.newKeyFactory().setKind(USER_KIND).newKey(username);
    }

    private Key sessionKey(String tokenId) {
        return datastore.newKeyFactory().setKind(SESSION_KIND).newKey(tokenId);
    }

    private String getUserRole(Entity user) {
        if (user.contains(USER_ROLE)) {
            String storedRole = normalizeRole(user.getString(USER_ROLE));
            if (isSupportedRole(storedRole)) {
                return storedRole;
            }
        }
        return ROLE_USER;
    }

    private boolean hasAnyRole(String role, Collection<String> allowedRoles) {
        return allowedRoles.contains(role);
    }

    private boolean isSupportedRole(String role) {
        return ROLE_USER.equals(role) || ROLE_ADMIN.equals(role) || ROLE_BACKOFFICE.equals(role);
    }

    private boolean isExpired(AuthToken token) {
        return System.currentTimeMillis() >= token.expiresAt;
    }

    private String getString(Entity entity, String propertyName) {
        return entity.contains(propertyName) ? entity.getString(propertyName) : "";
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    //Capitaliza a string role caso nao esteja e da trim
    private String normalizeRole(String role) {
        return trim(role) == null ? null : trim(role).toUpperCase();
    }

    private Response successResponse(Object data) {
        return Response.ok(g.toJson(new ApiResponse<>("success", data))).build();
    }

    private Response errorResponse(int errorCode, String errorName, String errorMessage) {
        LOG.fine("Returning error " + errorCode + " (" + errorName + ")");
        return Response.ok(g.toJson(new ApiResponse<>(Integer.toString(errorCode), errorMessage))).build();
    }

    private static final class TokenValidationResult {
        private final AuthToken token;
        private final String currentRole;
        private final Integer errorCode;
        private final String errorName;
        private final String errorMessage;

        private TokenValidationResult(AuthToken token, String currentRole, Integer errorCode, String errorName, String errorMessage) {
            this.token = token;
            this.currentRole = currentRole;
            this.errorCode = errorCode;
            this.errorName = errorName;
            this.errorMessage = errorMessage;
        }

        static TokenValidationResult valid(AuthToken token, String currentRole) {
            return new TokenValidationResult(token, currentRole, null, null, null);
        }

        static TokenValidationResult invalid(int errorCode, String errorName, String errorMessage) {
            return new TokenValidationResult(null, null, errorCode, errorName, errorMessage);
        }

        boolean valid() {
            return token != null;
        }

        String username() {
            return token.username;
        }

        String currentRole() {
            return currentRole;
        }

        AuthToken token() {
            return token;
        }

        Response toResponse() {
            return Response.ok(new Gson().toJson(new ApiResponse<>(Integer.toString(errorCode), errorMessage))).build();
        }
    }
}
