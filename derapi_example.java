import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DerapiExample {

    public static String fetchToken(String tokenUrl, String clientId, String clientSecret) throws Exception {
        // Fetches an OAuth2 token from `token_url' using client credentials,
        // using standard Requests library facilities. An example token may
        // look like this:
        // {
        //   "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA",
        //   "expires_in":300,
        //   "refresh_expires_in":1800,
        //   "refresh_token":"eyJhbGcIgOiAiSldUIiwia2lkIiA6ICJhNmJlZjg4NS0yNT",
        // }
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        String data = "grant_type=client_credentials";
        connection.getOutputStream().write(data.getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        String jsonResponse = response.toString();
        int start = jsonResponse.indexOf("\"access_token\":\"") + "\"access_token\":\"".length();
        int end = jsonResponse.indexOf("\"", start);
        return jsonResponse.substring(start, end);
    }


    public static Map tokens(LoginInfo derapiLogin, Map<String, LoginInfo> backendLogins) throws Exception {
        // Obtain OAuth2 tokens for Derapi and backends
        String derapiToken = fetchToken(derapiLogin.tokenUrl, derapiLogin.clientId, derapiLogin.clientSecret);

        Map backendTokens = new HashMap();

        for (Entry<String, LoginInfo> backend : backendLogins.entrySet()) {
            String backendToken = fetchToken(backend.getValue().tokenUrl, backend.getValue().clientId, backend.getValue().clientSecret);
            backendTokens.put(backend.getKey(), backendToken);
        }
        Map tokens = new HashMap();
        tokens.put("derapiToken", derapiToken);
        tokens.put("backendTokens", backendTokens);
        return tokens;
    }
    public static HttpURLConnection derapiRequest(String url, String derapiToken, Map<String, String> backendTokens) throws Exception {
    	// headers with Authorization header for Derapi and X-Authorization-*
    	// headers for backends. You need to do this for every Derapi URL you
    	// want to get. The headers may look like this:
    	// GET /sites HTTP/1.1
    	// Host: api.derapi.com
    	// Authorization: Bearer ZGVyYXBpIHRva2VuCg==
    	// X-Authorization-sma-sbox: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA
    	// X-Authorization-solis: Bearer c29saXMgdG9rZW4K
    	Map<String, String> xAuthHeaders = new HashMap<>();
        for (String backendName : backendTokens.keySet()) {
            String accessToken = backendTokens.get(backendName);
            xAuthHeaders.put("X-Authorization-" + backendName, "Bearer " + accessToken);
        }

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + derapiToken);
        for (String headerName : xAuthHeaders.keySet()) {
            connection.setRequestProperty(headerName, xAuthHeaders.get(headerName));
        }
        return connection;
    }
    public static void main(String[] args) throws Exception {
    	// obtain OAuth2 tokens for Derapi, backends
        LoginInfo derapiLogin = new LoginInfo("https://auth.derapi.com/oauth2/token", "3tb5s5326rpd726gk8vcpllhj", "1nrvnklg39mlulik7u4q717q093q0htprng1p7teneg20vl6d020");
        Map backendLogins = new HashMap();

        LoginInfo smaLogin = new LoginInfo("https://sandbox-auth.smaapis.de/oauth2/token", "derapi_api", "1q14ur3PxuiNlvlQGBzLrMPjyCzkCZAE");
        LoginInfo solisLogin = new LoginInfo("https://api.derapi.com/oauth/solis", "1300386381676488475", "e1596fd6a4f84e888327dbbc82ed8bd2");
        backendLogins.put("sma-sbox", smaLogin);
        backendLogins.put("solis", solisLogin);
        // construct request for a Derapi resource using OAuth2 tokens
        Map<String, Object> tokens = tokens(derapiLogin, backendLogins);
        String derapiToken = (String) tokens.get("derapiToken");
        Map<String, String> backendTokens = (Map<String, String>) tokens.get("backendTokens");
        HttpURLConnection response = derapiRequest("https://api.derapi.com/sites", derapiToken, backendTokens);
        System.out.println(response.getResponseCode());

    }
}



// Remember to add this class in a different file.
public  class LoginInfo {
        public String tokenUrl;
        public String clientId;
        public String clientSecret;

        public LoginInfo(String tokenUrl, String clientId, String clientSecret) {
            this.tokenUrl = tokenUrl;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

 }
