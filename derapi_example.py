# derapi_example.py: Example client code to access Derapi services, in Python

import requests
import oauthlib.oauth2
import requests_oauthlib

class LoginInfo():
    """A simple class to hold login credentials for different services"""
    def __init__(self, token_url, client_id, client_secret):
        # URL from which user can obtain token for this service
        self.token_url = token_url
        # Client credentials
        self.client_id = client_id
        self.client_secret = client_secret

# Sandbox credentials for Derapi and some backends. Normally, in
# production, these would come from secure, encrypted storage. We
# include them here in cleartext for illustration purposes only.
DERAPI_LOGIN =  LoginInfo("https://auth.derapi.com/oauth2/token",         "3tb5s5326rpd726gk8vcpllhj", "1nrvnklg39mlulik7u4q717q093q0htprng1p7teneg20vl6d020")
BACKEND_LOGINS = {
    "sma-sbox": LoginInfo("https://sandbox-auth.smaapis.de/oauth2/token", "derapi_api",                "1q14ur3PxuiNlvlQGBzLrMPjyCzkCZAE"),
    "solis":    LoginInfo("https://api.derapi.com/oauth/solis",           "1300386381676488475",       "e1596fd6a4f84e888327dbbc82ed8bd2"),
}


def fetch_token(token_url, client_id, client_secret):
    """Fetches an OAuth2 token from `token_url' using client credentials,
    using standard Requests library facilities. An example token may
    look like this:
    {
        "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA",
        "expires_in":300,
        "refresh_expires_in":1800,
        "refresh_token":"eyJhbGcIgOiAiSldUIiwia2lkIiA6ICJhNmJlZjg4NS0yNT",
    }
    """
    client = oauthlib.oauth2.BackendApplicationClient(client_id=client_id)
    session = requests_oauthlib.OAuth2Session(client=client)
    token = session.fetch_token(
        token_url=token_url,
        client_id=client_id,
        client_secret=client_secret)
    return token


def tokens(derapi_login, backend_logins):
    """Obtain OAuth2 tokens for Derapi and backends. You only need to do
    this once per session (or until tokens expire)"""
    derapi_token = fetch_token(derapi_login.token_url,
                               derapi_login.client_id,
                               derapi_login.client_secret)
    backend_tokens = {}
    for backend_name in backend_logins:
        backend_tokens[backend_name] = fetch_token(backend_logins[backend_name].token_url,
                                                   backend_logins[backend_name].client_id,
                                                   backend_logins[backend_name].client_secret)
    return (derapi_token, backend_tokens)


def derapi_request(url, derapi_token, backend_tokens):
    """Constructs a request for a Derapi resource. Populates request
    headers with Authorization header for Derapi and X-Authorization-*
    headers for backends. You need to do this for every Derapi URL you
    want to get. The headers may look like this:

    GET /sites HTTP/1.1
    Host: api.derapi.com
    Authorization: Bearer ZGVyYXBpIHRva2VuCg==
    X-Authorization-sma-sbox: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA
    X-Authorization-solis: Bearer c29saXMgdG9rZW4K
    """

    # construct a dictionary of X-Authorization-* headers
    x_auth_headers = {}
    for backend_name in backend_tokens:
        access_token = backend_tokens[backend_name]["access_token"]
        x_auth_headers[f"X-Authorization-{backend_name}"] = f"Bearer {access_token}"

    # create a request including Derapi URL, Derapi authorization and
    # and X-Authorization-* headers
    request = requests.Request("GET",
                               url=url,
                               auth=requests_oauthlib.OAuth2(token=derapi_token),
                               headers=x_auth_headers)
    return request.prepare()


if __name__ == "__main__":
    # obtain OAuth2 tokens for Derapi, backends
    derapi_token, backend_tokens = tokens(DERAPI_LOGIN, BACKEND_LOGINS)
    # construct request for a Derapi resource using OAuth2 tokens
    request = derapi_request("https://api.derapi.com/sites", derapi_token, backend_tokens)
    # execute the request
    response = requests.Session().send(request)
    # do something useful with the data
    print(response.text)

#  LocalWords:  derapi
