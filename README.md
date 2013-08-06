DIAL
====
<p><img src="http://chromecast.entertailion.com/chromecastanimation100.gif"/></p>

<p>This is DIAL client written in Android. The <a href="http://www.dial-multiscreen.org/dial-protocol-specification">DIAL protocol</a> allows TV devices to be discovered and controlled.</p>

<p>The current version will discover both Google TV and ChromeCast devices. The client is a proof of concept for controlling ChromeCast devices using open API's. The current code does not rely on the Google Cast SDK and the OS on the ChromeCast device does not need to be hacked.
The ChromeCast device also does not need to have developer options enabled.
</p>

<p>After the DIAL servers are discovered and the user selects a particular device in the UI, an attempt is made to connect to the ChromeCast device and play a YouTube video.
Most of the ChromeCast-specific logic is contained in MainActivity.onActivityResult. 
Operations are done via HTTP and Web Sockets. ChromeCast apps use a protocol called RAMP for media playback which is not currently supported by the client.
</p>

<p>This holds promise for being to control other aspects of the ChromeCast device using open API's. How the CromeCast device works is now better understood (especially since the low level protocol details aren't documented by Google). 
It is possible to remotely control the device from a third-party app. There might be other aspects of the device that might be controlled in ways the Google apps don't support. 
This also shows that it might be possible to develop apps that don't use the cloud based solution of the official Google Cast SDK.﻿
</p>


<p>Other apps developed by Entertailion:
<ul>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tvremote">Able Remote for Google TV</a>: The ultimate Google TV remote</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.launcher">Open Launcher for Google TV</a>: The ultimate Google TV launcher</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlay">Overlay for Google TV</a>: Live TV effects for Google TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlaynews">Overlay News for Google TV</a>: News headlines over live TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.videowall">Video Wall</a>: Wall-to-Wall Youtube videos</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tasker">GTV Tasker Plugin</a>: Control your Google TV with Tasker actions</li>
</ul>
</p>