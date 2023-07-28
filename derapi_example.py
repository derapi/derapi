# derapi_example.py: Example client code to access Derapi services, in Python

import sys

import requests
import oauthlib.oauth2
import requests_oauthlib

class LoginInfo():
    """A simple class to hold login credentials for different services"""
    def __init__(self, token_url, client_id, client_secret):
        self.token_url = token_url
        self.client_id = client_id
        self.client_secret = client_secret

DERAPI_LOGIN = LoginInfo("https://auth.derapi.com/oauth2/token", "3tb5s5326rpd726gk8vcpllhj", "1nrvnklg39mlulik7u4q717q093q0htprng1p7teneg20vl6d020")

BACKEND_LOGINS = {
    "sma-sbox": LoginInfo("https://sandbox-auth.smaapis.de/oauth2/token", "derapi_api", "1q14ur3PxuiNlvlQGBzLrMPjyCzkCZAE"),
    "solis": LoginInfo("https://api.derapi.com/oauth/solis","1300386381676488475","e1596fd6a4f84e888327dbbc82ed8bd2"),
}

def token(token_url, client_id, client_secret):
    """Fetches an OAuth2 token from `token_url' using client credentials."""
    client = oauthlib.oauth2.BackendApplicationClient(client_id=client_id)
    session = requests_oauthlib.OAuth2Session(client=client)
    token = session.fetch_token(
        token_url=token_url,
        client_id=client_id,
        client_secret=client_secret)
    return token

def tokens(derapi_login, backend_logins):
    derapi_token = token(derapi_login.token_url,
                         derapi_login.client_id,
                         derapi_login.client_secret)
    backend_tokens = {}
    for backend_name in backend_logins:
        backend_tokens[backend_name] = token(backend_logins[backend_name].token_url,
                                             backend_logins[backend_name].client_id,
                                             backend_logins[backend_name].client_secret)
    return (derapi_token, backend_tokens)

def derapi_request(url, derapi_token, backend_tokens):
    x_auth_headers = {}
    for backend_name in backend_tokens:
        x_auth_headers[f"X-Authorization-{backend_name}"] = backend_tokens[backend_name]["access_token"]
    request = requests.Request("GET",
                               url=url,
                               auth=requests_oauthlib.OAuth2(token=derapi_token),
                               headers=x_auth_headers)
    return request.prepare()

def main():
    derapi_token, backend_tokens = tokens(DERAPI_LOGIN, BACKEND_LOGINS)
    request = derapi_request("https://api.derapi.com/sites", derapi_token, backend_tokens)
    response = requests.Session().send(request)
    print(response.text)

if __name__ == "__main__":
    sys.exit(main())
