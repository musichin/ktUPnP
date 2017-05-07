# ktUPnP [![Kotlin](https://img.shields.io/badge/Kotlin-1.1.2-blue.svg)](http://kotlinlang.org) [![Build Status](https://travis-ci.org/musichin/ktUPnP.svg?branch=master)](https://travis-ci.org/musichin/ktUPnP) [![jcenter](https://api.bintray.com/packages/musichin/maven/ktUPnP/images/download.svg) ](https://bintray.com/musichin/maven/ktUPnP/_latestVersion)

## Discovery

### Search
Send a `M-SEARCH` message and receive `OK` responses
```kotlin
Ssdp.search("upnp:rootdevice").subscribe {
  println(it.location)
  // "http://192.168.0.1:9561/description.xml"
  println(it.server)
  // "Linux/2.6.32.5"
}
```

### Notifications
Listen for `NOTIFY` messages
```kotlin
Ssdp.notifications().filter { it.st == "upnp:rootdevice" }.subscribe {
  println(it.nt)
  // "urn:schemas-upnp-org:device:InternetGatewayDevice:1"
}
```

### Publish
Response to all matching `M-SEARCH` messages
```kotlin
val message = SsdpMessage.Builder()
  .ok()
  .st("upnp:rootdevice")
  .build()

Ssdp.publish(message).subscribe()
```

### Notify
Send one shot `NOTIFY` message
```kotlin
val message = SsdpMessage.Builder()
  .type(SsdpMessage.NOTIFY_TYPE)
  .st(ST_TEST)
  .build()

Ssdp.notify(message).subscribe()
```

### Binaries
```groovy
repositories {
    maven { url 'https://bintray.com/musichin/maven' }
}

dependencies {
    compile 'com.github.musichin.ktupnp:ktupnp-discovery:x.y.z'
}
```

## License

    Copyright (C) 2016 Anton Musichin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


