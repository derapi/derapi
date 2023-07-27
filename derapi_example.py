# derapi_example.py: Example client code to access Derapi services, in Python

import sys

import requests
import oauthlib.oauth2
import requests_oauthlib

DERAPI_LOGIN = {
    "token_url": "https://auth.derapi.com/oauth2/token",
    "client_id": "3tb5s5326rpd726gk8vcpllhj",
    "client_secret": "1nrvnklg39mlulik7u4q717q093q0htprng1p7teneg20vl6d020",
}

BACKEND_LOGINS = {
    "sma-sbox": {
        "token_url": "https://sandbox-auth.smaapis.de/oauth2/token",
        "client_id": "derapi_api",
        "client_secret": "1q14ur3PxuiNlvlQGBzLrMPjyCzkCZAE",
    },
    "solis": {
        "token_url": "https://api.derapi.com/oauth/solis",
        "client_id": "1300386381676488475",
        "client_secret": "e1596fd6a4f84e888327dbbc82ed8bd2",
    },
}

def token(login):
    client = oauthlib.oauth2.BackendApplicationClient(client_id=login["client_id"])
    session = requests_oauthlib.OAuth2Session(client=client)
    token = session.fetch_token(
        token_url=login["token_url"],
        client_id=login["client_id"],
        client_secret=login["client_secret"])
    return token

def main():
    derapi_token = token(DERAPI_LOGIN)
    print("derapi:", derapi_token)
    for backend in BACKEND_LOGINS:
        backend_token = token(BACKEND_LOGINS[backend])
        print(backend+":", backend_token)

if __name__ == "__main__":
    sys.exit(main())
