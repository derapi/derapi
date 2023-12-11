// DerapiAuthExample.java: example client code to access Derapi services, in Java

// this code has one external dependency: org.json (https://github.com/stleary/JSON-java)
import org.json.JSONObject;
import org.json.JSONTokener;

// all other imports are standard in JDK
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

public class DerapiAuthExample {

    /** A simple class to hold login credentials for different services */
    static class LoginInfo {
        // URL from which user can obtain token for this service
        public String tokenUrl;
        // Client credentials
        public String clientId;
        public String clientSecret;

        public LoginInfo(String tokenUrl, String clientId, String clientSecret) {
            this.tokenUrl = tokenUrl;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }
    
    // Sandbox credentials for Derapi and some backends. Normally, in
    // production, these would come from secure, encrypted storage. We
    // include them here in cleartext for illustration purposes only.
    static LoginInfo DERAPI_LOGIN =
                           new LoginInfo("https://auth.derapi.com/oauth2/token",         "3tb5s5326rpd726gk8vcpllhj", "1nrvnklg39mlulik7u4q717q093q0htprng1p7teneg20vl6d020");
    static Map<String,LoginInfo> BACKEND_LOGINS =
        Map.of("smasbox", new LoginInfo("https://sandbox-auth.smaapis.de/oauth2/token", "derapi_api",                "1q14ur3PxuiNlvlQGBzLrMPjyCzkCZAE"),
               "solis",   new LoginInfo("https://api.derapi.com/oauth/solis",           "1300386381676488475",       "e1596fd6a4f84e888327dbbc82ed8bd2"));


    /** Fetches an OAuth2 token from `token_url' using client credentials.
        An example token may look like this:
        {
            "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA",
            "expires_in":300,
            "refresh_expires_in":1800,
            "refresh_token":"eyJhbGcIgOiAiSldUIiwia2lkIiA6ICJhNmJlZjg4NS0yNT",
        }

        This is a very basic implementation without external
        dependencies. More complete client OAuth2 implementations are
        available as third-party libraries, e.g., https://oauth.net/code/java/
    */
    static JSONObject fetchToken(String tokenUrl, String clientId, String clientSecret) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)new URL(tokenUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        String b64 = Base64.getEncoder().encodeToString((clientId +
                                                         ":" +
                                                         clientSecret).getBytes());
        connection.setRequestProperty("Authorization", "Basic " + b64);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.getOutputStream().write("grant_type=client_credentials".getBytes());

        return new JSONObject(new JSONTokener(connection.getInputStream()));
    }


    /** Obtain OAuth2 tokens for Derapi and backends. You only need to do
        this once per session (or until tokens expire)
    */
    public static Map<String, JSONObject> tokens(LoginInfo derapiLogin,
                                             Map<String, LoginInfo> backendLogins) throws Exception {
        Map<String, JSONObject> tokens = new HashMap<>();

        JSONObject derapiToken = fetchToken(derapiLogin.tokenUrl,
                                            derapiLogin.clientId,
                                            derapiLogin.clientSecret);
        // The token for Derapi goes into the map with a `null' key,
        // to differentiate it from other tokens. Its value goes into
        // the standard `Authentication' header and does not require a
        // name.
        tokens.put(null, derapiToken);

        for (String backendName : backendLogins.keySet()) {
            // Tokens for data partner backends go into the map under
            // their respective parner names.
            tokens.put(backendName,
                       fetchToken(backendLogins.get(backendName).tokenUrl,
                                  backendLogins.get(backendName).clientId,
                                  backendLogins.get(backendName).clientSecret));
        }
        return tokens;
    }

    /** Constructs a request for a Derapi resource. Populates request
        headers with Authorization header for Derapi and X-Authorization-*
        headers for backends. You need to do this for every Derapi URL you
        want to access. The headers may look like this:

        GET /sites HTTP/1.1
        Host: api.derapi.com
        Authorization: Bearer ZGVyYXBpIHRva2VuCg==
        X-Authorization-smasbox: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA
        X-Authorization-solis: Bearer c29saXMgdG9rZW4K
    */
    public static HttpURLConnection derapiRequest(String url,
                                                  Map<String, JSONObject> tokens) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
    	Map<String, String> xAuthHeaders = new HashMap<>();
        for (String backendName : tokens.keySet()) {
            String accessToken = tokens.get(backendName).getString("access_token");
            if (backendName == null) {
                connection.setRequestProperty("Authorization",
                                              "Bearer " + accessToken);
            } else {
                connection.setRequestProperty("X-Authorization-" + backendName,
                                              "Bearer " + accessToken);
            }
        }
        return connection;
    }

    public static void main(String[] args) throws Exception {
    	// obtain OAuth2 tokens for Derapi, backends
        Map<String, JSONObject> tokens = tokens(DERAPI_LOGIN, BACKEND_LOGINS);
        // construct request for a Derapi resource using OAuth2 tokens
        HttpURLConnection connection = derapiRequest("https://api.derapi.com/sites", tokens);
        // execute the request and read response
        JSONObject response = new JSONObject(new JSONTokener(connection.getInputStream()));
        // do something useful with the data
        System.out.println(response.toString(2));
    }
}
