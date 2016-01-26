# NewbornsWatcher
Simple Android app for jenkins and monitoring integration

Just create a config file on the GCM control panel, download it and copy it to app/ folder.

Next, configure your jenkins or other application to fire a POST as follows:

```
  url: https://gcm-http.googleapis.com/gcm/send
  headers: 
    Content-type: application/json
    Authorization: key=your_api_key_here
  payload:
  {
      "to": "/topics/global",
      "data": {
          "message": {
              "origin": "the push notification and card tile",
              "message": "the card subtitle/description"
          }
      }
  }
```

and you're set!
