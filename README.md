# Derapi.com API Guide

## Summary

Derapi provides a RESTful API that allows users to access and control individual Distributed Energy Resources (DERs).
Examples of DERs include solar and storage inverters, heat pumps, smart thermostats, and EV chargers.
Many vendors provide API access to their DER products, but typically APIs differ both in terms of the data they expose, and in the mechanics of access.
Some APIs are pull; some are push.
Some use REST, some use other techniques.
Derapi provides a simple abstraction layer over these differences to present a uniform API for each type of DER.

The Derapi API Reference is available at https://api.derapi.com/apidocs/

## Getting Started
Follow these steps to get started building your application using Derapi’s API:
1. Review the docs – starting with this guide and the [API Reference](https://api.derapi.com/apidocs/#/)
2. Get credentials – if you don’t have your Derapi client_id and client_secret please email sales@derapi.com to request access
3. Test your credentials – follow the steps in [Making a request](#making-a-request) to exchange your credentials for an authentication token and make your first API call

## Security and Privacy
Derapi's policy is to retain as little customer data as possible.
In particular, Derapi avoids storing sensitive information like passwords and credentials users need to log into vendor's APIs.

## JSON over REST
Derapi uses JSON over REST.
Derapi uses HTTPS.
Users issue GET requests to receive information about DERs.
Users issue POST requests to modify aspects of DER state.
Derapi responses are always in JSON format, including error messages.

## Versioning
The Derapi API maintains versions in the format v1, v2, etc.
The current version is v1.
Future versions will include a mechanism to set and specify a version for requests.

## Error Handling
Derapi uses standard HTTP response codes to signal success or failure.
In cases of failure, Derapi's response includes a description of the failure in JSON format.
The following sections detail the error codes Derapi can return.

### Success

#### 200 OK
Normal response.

### Client Errors
400-series HTTP responses signify problems with client requests.
The implication is that the client can modify the request to obtain the information.

#### 400 Client Error
Derapi server is functioning properly and has understood the request, but cannot fulfill it.
Example: malformed date strings in query arguments.

#### 403 Forbidden
User failed to include relevant credentials in the request, or credentials are invalid for this resource.
User must supply both Derapi authentication and authentication for backend(s) which it wishes to access.
The section on Authentication, below, provides additional information.

<!-- describe difference between 403s for bad Dearpi credentials and 403s for bad backend credentials once https://github.com/derapi/cloud-base/issues/363 is complete -->

#### 404 Not Found
The resource which the client requested does not exist.
In some cases, backends report existing resources as nonexistent if client credentials are insufficient to view them, for privacy concerns.
In these cases, Derapi returns 404.

### Backend Errors
500-series HTTP responses signify backend problems.
The implication is that there is nothing the client can do to resolve the problem, except reissue the same request at a later time.

#### 500 Derapi Internal Error
A problem in Derapi code.
Derapi policy is to notify our engineering team of these types of errors, so we can address them as quickly as possible.
While we notify the engineering team automatically of these errors, it can be helpful for clients to report them to Derapi technical support, with any additional details that may help Derapi engineers debug the problem.

#### 502 Bad Gateway
An internal problem in a backend API.
Derapi is able to communicate with the backend using its normal protocol, but the backend is reporting an internal error.
An example is a backend server returning a 5xx-series HTTP code.
Derapi policy is to notify backend operators of these types of errors so they may improve their services.
While we notify our backend vendors of these problems, it can be helpful for clients to report them to Derapi technical support, with any additional details that may help Derapi engineers communicate the problem to backend operators.

#### 504 Gateway Timeout
A network communication timeout prevents normal communication with a backend.
This indicates an infrastructure problem that may or may not be under a backend's control.
Examples include network outages and DNS misconfiguration.

### Partial-Success Responses
A number of Derapi endpoints report lists of resources, e.g., https://api.derapi.com/solar-inverters.
These lists aggregate responses from multiple backends.
For these endpoints Derapi always reports success, even if one or more of the backend calls fail.
For example, one of the backends may reject client's credentials, or be unavailable due to a network outage.
In this case, Derapi reports responses from all other backends, and includes the name of the failing backend and a description of the problem in the `errors` object.
An example response might look like this:

    {
      "solar-inverters": [
        "https://api.derapi.com/solar-inverters/sma:12800016",
        "https://api.derapi.com/solar-inverters/sma:12800017",
        "https://api.derapi.com/solar-inverters/sma:12800018",
        "https://api.derapi.com/solar-inverters/sma:12800023",
        "https://api.derapi.com/solar-inverters/sma:12800024"
      ],
      "errors":{
        "solis": "Unauthorized: https://www.soliscloud.com:13333/v1/api/inverterList"
      }
    }

If all backend calls succeed, the `error` object is empty.

## Making a request

### Vendor Authentication
When clients make requests against Derapi, they include an OAuth Bearer token for each vendor to which a they have access.
To accommodate multiple bearer tokens in one HTTP request, the client includes one header per backend.
The headers take the form `X-Authorization-<vendor>: Bearer` where `<vendor>` is one of the backends Derapi supports, e.g., `sma`, `solis`, `se` (solaredge), etc.
The `X-Authorization-*` headers follow the syntax and semantics of RFC 6750.
Clients obtain bearer tokens directly from each backend, making it unnecessary to share their credentials with Derapi.

### Example: client acquires and transmits tokens, using curl.
To acquire a bearer token from SMA, the client posts this request to SMA's `token` endpoint:

    $ curl -u sma_client_id:sma_client_secret \
           -H "Content-Type: application/x-www-form-urlencoded" \
           -d grant_type=client_credentials&scope=monitoringApi:read \
           -X POST https://auth.smaapis.de/oauth2/token

This produces the following headers and body:

    POST /oauth2/token HTTP/1.1
    Host: sandbox-auth.smaapis.de
    Authorization: Basic c21hX2NsaWVudF9pZDpzbWFfY2xpZW50X3NlY3JldA==
    Content-Type: application/x-www-form-urlencoded
    Content-Length: 54
    
    grant_type=client_credentials&scope=monitoringApi:read

If the endpoint accepts client's credentials, it responds with JSON similar to the following:

    {
     "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA",
     "expires_in":300,
     "refresh_expires_in":1800,
     "refresh_token":"eyJhbGcIgOiAiSldUIiwia2lkIiA6ICJhNmJlZjg4NS0yNT",
     "scope":"monitoringApi:read gridControlApi_EnergyTrader:read"
    }

The client repeats this process for other backends, saving the value of `access_token` each time (`eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA` in this example).
Naturally, credentials and scopes will differ for each backend.

#### Derapi Authentication
Using the same process, client acquires a token for Derapi as well, using its Derapi `client_id` and `client_secret` at https://auth.derapi.com/oauth2/token.
Derapi access tokens expire after 1 hour but we recommend requesting a new token whenever one is needed. 
If your access token expires you should repeat the same process to obtain a new access token.

#### Example Derapi API request
Having acquired tokens for Derapi and all relevant backends, the client passes them as headers in requests to Derapi.
For example, a client can use this command to retrieve a list of all solar inverters from Derapi:

    $ curl -H "Authorization: Bearer ZGVyYXBpIHRva2VuCg==" \
           -H "X-Authorization-sma: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA" \
           -H "X-Authorization-solis: Bearer c29saXMgdG9rZW4K" \
            https://api.derapi.com/solar-inverters

This request produces the following headers:

    GET /solar-inverters HTTP/1.1
    Host: api.derapi.com
    Authorization: Bearer ZGVyYXBpIHRva2VuCg==
    X-Authorization-sma: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA
    X-Authorization-solis: Bearer c29saXMgdG9rZW4K

Derapi's response may look like this:

    {
      "solar-inverters": [
        "https://api.derapi.com/solar-inverters/sma:12800016",
        "https://api.derapi.com/solar-inverters/sma:12800017",
        "https://api.derapi.com/solar-inverters/sma:12800018",
        "https://api.derapi.com/solar-inverters/sma:12800023",
        ...
        "https://api.derapi.com/solar-inverters/sma:1280110",
        "https://api.derapi.com/solar-inverters/sma:1280111",
        "https://api.derapi.com/solar-inverters/solis:1308675217947229038",
        "https://api.derapi.com/solar-inverters/solis:1308675217947229037",
      ],
      "errors": {}
    }

### Example: client acquires and transmits tokens, using Python.
This repository includes a complete, working example in Python: [derapi_auth_example.py](derapi_auth_example.py).

### Example: client acquires and transmits tokens, using Java.
This repository includes a complete, working example in Java: [DerapiAuthExample.java](DerapiAuthExample.java).


## Auxiliary OAuth endpoints
`https://api.derapi.com/oauth/*`

Most backends have standardized on OAuth as the authentication mechanism.
One benefit of third-party authentication schemes like OAuth is that Derapi never sees clients' credentials for different backends.
Information Derapi does not have cannot be hacked by malicious attackers.
OAuth's ubiquity means client engineers are familiar with its workflows.
A number of backends use other authentication schemes, ranging from HTTP `Basic Auth` to custom designs.
For these backends, Derapi provides auxiliary OAuth endpoints to present a uniform interface to clients.

### Auxiliary OAuth: Solis Cloud

`https://api.derapi.com/oauth/solis`

Derapi provides an auxiliary OAuth endpoint for Solis Cloud.
A client is required to send its Solis credentials to this endpoint and receive a Bearer token on success.
The client then sends this token in the `X-Authentication-solis` header, same as regular OAuth tokens obtained directly from backends.

## Obtaining Vendor Credentials and Customer Enrollment
The approach to issuing vendor API credentials and enrolling customer devices varies between DER vendors.
This section contains summarized instructions on how to get credentials for each vendor and associate customer-owned devices with your credentials.
Each vendor is marked with whether customer enrollment is via [OAuth](https://oauth.net/2/) or a custom authorization scheme.

Derapi also offers a hosted option for customer enrollment and maintaining vendor credentials. Reach out to sales@derapi.com to learn how we can help streamline customer and credential management.

## Solis (`Custom`)
### Summary
Solis uses a custom authorization system using API ID and Key for each [Solis portal](https://www.soliscloud.com/) account.
Once your customer's systems are associated with your Solis portal account then follow the instructions to get your API ID and Key.
### Enrolling Customers
If you are using a single Solis portal account to make API requests then follow the instructions to add all your customer systems.
This is referred to as "Add Plant" in the Solis documentation.
Alternatively, you can collect API ID/Keys from your customers and use those to make API requests.
### Obtaining Credentials
Follow the steps outlined below to generate a Solis API ID and Key.
Please reference **Auxiliary OAuth: Solis Cloud** for more information on generating a Bearer token for making Solis API requests.
``` mermaid
graph TD
	subgraph Solis API
		a("Sign in to the Solic Cloud Portal") --> b("Account -> Basic Settings -> API Management to get Key")
		b --> c("Copy the API ID and Key and save")
		c --> d("Use the Derapi Auxiliary Auth endpoint to generate a bearer token")
		d --> e("Make API requests with this token")
	end
```

## SMA (`OAuth`)
### Summary
SMA offers OAuth2 for enrolling customer systems for API access. To create a SMA API account [contact SMA](https://developer.sma.de/contact.html). 
### Enrolling Customers
Follow the [SMA Code Grant Flow instructions](https://developer.sma.de/api-access-control.html#c441249) to set up OAuth for customers to authorize your application. 
### Obtaining Credentials
Follow these steps to create a SMA API Account and get an access token.
SMA refresh tokens expire by default.
Derapi recommends using SMA’s [offline_token option](https://developer.sma.de/api-access-control.html) to acquire a refresh token that does not expire.
``` mermaid
graph TD
	subgraph SMA API
		da1(Request API Account) --> da4(End customer/sytem owner authorizes Application via OAuth)
		da4 --> da5(Application receives Authorization Code)
		da5 --> da6(Get access and refresh tokens)
		da6 --> da7(Make API requests)
	end
```

## SolarEdge (`Custom`)
### Summary
SolarEdge uses a custom authorization system using an API Key.
This key is retrieved from the SolarEdge [Monitoring Portal](https://monitoring.solaredge.com/solaredge-web/p/login?locale=en_US).
Once your customer's systems are associated with your SolarEdge monitoring portal account then follow the instructions to get your API Key.
Derapi recommends using an Account API Key which provides access to all sites in your account.
### Enrolling Customers
If you are using a single SolarEdge monitoring portal account to make API requests then follow the instructions to add all your customer systems.
This is referred to as "Add Inverter or Gateway" in the SolarEdge documentation.
Alternatively, you can collect API Keys from your customers and use those to make API requests.
### Obtaining Credentials
Follow the steps outlined below to generate a SolarEdge Account API Key.
``` mermaid
graph TD
	subgraph SolarEdge API
		a(Sign in to the SolarEdge Monitoring Portal) --> b(Generate Account API Key)
		b --> c(Copy the API Key and save)
		c --> d(Click Save in SolarEdge Portal)
		d --> e(Make API requests with the key)
	end
```

## Enphase (`OAuth` or `Custom`)
### Summary
Enphase offers two options for enrolling and authorizing customer systems.
If you are an in Installer with Enphase portal access to customer systems then the custom approach, via Partner API, is recommended.
If you are an Application Developer we recommend using OAuth for customers to authorize your application.
### Enrolling Customers
If you are an Installer then then follow the Enphase instructions to add all your customer systems to your Enphase account.
If you are an Application Developer then follow the [Enphase instructions](https://developer-v4.enphase.com/docs/quickstart.html) to set up OAuth for customers to authorize your application. 
### Obtaining Credentials
If you are an Installer follow these steps to create a Partner Plan Developer Account and get an access token.
Please note that Enphase access tokens expire after 1 day and refresh tokens expire after 1 month.
Your application should refresh the refresh token before it expires to avoid having to manually reauthorize.
``` mermaid
graph TD
	subgraph Partner API
		pa1(Create Partner Plan Developer Account) --> pa2(Configure Application)
		pa2 --> pa3(Get access and refresh tokens)
		pa3 --> pa4(Make API requests)
		pa3 --> pa5(Periodically refresh your refresh token)
		pa5 --> pa3
	end
```
If you are an Application Developer follow these steps to create a Developer Account and get an access token.
Please note that Enphase access tokens expire after 1 day and refresh tokens expire after 1 month.
Your application should refresh the refresh token before it expires to avoid having to ask customers to manually reauthorize.
``` mermaid
graph TD
	subgraph Developer API
		da1(Create Developer Account) --> da2(Configure Application)
		da2 --> da3(Get API Key, Auth URL, client secret)
		da3 --> da4(End customer/sytem owner authorizes Application via OAuth)
		da4 --> da5(Dev/Application receives Auth Code)
		da5 --> da6(Get access and refresh tokens)
		da6 --> da7(Make API requests)
		da6 --> da8(Periodically refresh your refresh token)
		da8 --> da6
	end
```
## How to get help
Have questions on the API, how to use it, or a request for a new feature?
Please reach out via email to support@derapi.com.
