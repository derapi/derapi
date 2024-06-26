# Derapi Join

Derapi Join allows end-customers to grant you access to their systems.

### Integrate with your website 

To integrate Derapi Join into your website, simply follow the instructions below.
Add this script tag to your HTML

```html
<script src="https://raw.githubusercontent.com/derapi/derapi/main/join/derapi-join.js"></script>
```

Derapi Join requires a Join session to be created.  A POST request to https://api.derapi.com/join/session/start will return a `session_id` as documented in the [Derapi API Reference](https://api.derapi.com/apidocs/#/Join/post_join_session_start).

Once the session ID token is created, pass it into the JS function `derapi.createJoin()` like this:

```javascript
        import derapi from "https://raw.githubusercontent.com/derapi/derapi/main/join/derapi-join.js";
        const onSuccess = () => {
            alert('success!');
        }
        const onClose = () => {
            alert('closed');
        }
        const join = derapi.createJoin(sessionToken, onSuccess, onClose, derapiToken);
        join.open();
```

[Here](https://raw.githubusercontent.com/derapi/derapi/main/join/join-sample.html) is a sample `.html` file to help you get started.

Once the end-customer has authenticated and you receive the `onSuccess()` callback, simply call https://api.derapi.com/join/session/{session_id}/fetch-token to retrieve the public token that can be used to access the end-customer's system.  The call is documented in the [Derapi API Reference](https://api.derapi.com/apidocs/#/Join/get_join_session__session_id__fetch_token)
