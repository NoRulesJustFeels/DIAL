/*
 * Copyright (C) 2013 ENTERTAILION, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.entertailion.android.dial;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.codebutler.android_websockets.WebSocketClient;

/**
 * DIAL protocol client:
 * http://www.dial-multiscreen.org/dial-protocol-specification
 * 
 * @author leon_nicholls
 * 
 */
public class MainActivity extends Activity {
	private static final String LOG_TAG = "MainActivity";

	public static final String PREFS_NAME = "preferences";

	private static final String YOU_TUBE = "YouTube";
	private static final String FLING = "Fling";
	private static final String NETFLIX = "Netflix";
	private static final String CHROME_CAST = "ChromeCast";
	private static final String PLAY_MOVIES = "PlayMovies";
	private static final String TIC_TAC_TOE = "TicTacToe";

	private static final String STATE_RUNNING = "running";
	private static final String STATE_STOPPED = "stopped";

	private static final String HEADER_CONNECTION = "Connection";
	private static final String HEADER_CONNECTION_VALUE = "keep-alive";
	private static final String HEADER_ORIGN = "Origin";
	private static final String HEADER_ORIGIN_VALUE = "chrome-extension://boadgeojelhgndaghljhdicfkmllpafd";
	private static final String HEADER_USER_AGENT = "User-Agent";
	private static final String HEADER_USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.71 Safari/537.36";
	private static final String HEADER_DNT = "DNT";
	private static final String HEADER_DNT_VALUE = "1";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String HEADER_ACCEPT_ENCODING_VALUE = "gzip,deflate,sdch";
	private static final String HEADER_ACCEPT = "Accept";
	private static final String HEADER_ACCEPT_VALUE = "*/*";
	private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
	private static final String HEADER_ACCEPT_LANGUAGE_VALUE = "en-US,en;q=0.8";
	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String HEADER_CONTENT_TYPE_JSON_VALUE = "application/json";
	private static final String HEADER_CONTENT_TYPE_TEXT_VALUE = "text/plain";

	/**
	 * Request code used by this activity.
	 */
	protected static final int CODE_SWITCH_SERVER = 1;

	private DialServer target;

	private LinkedHashMap<InetAddress, DialServer> recentlyConnected = new LinkedHashMap<InetAddress, DialServer>();

	private WebSocketClient client;
	private String connectionServiceUrl;
	private String state;
	private String protocol;
	private String response;

	/*
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startActivityForResult(ServerFinder.createConnectIntent(this, target, getRecentlyConnected()), CODE_SWITCH_SERVER);
	}

	/*
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/*
	 * The user has selected a DIAL server to connect to
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == CODE_SWITCH_SERVER) {
			if (resultCode == RESULT_OK && data != null) {
				final DialServer dialServer = data.getParcelableExtra(ServerFinder.EXTRA_DIAL_SERVER);
				if (dialServer != null) {
					Toast.makeText(MainActivity.this, getString(R.string.finder_connected, dialServer.toString()), Toast.LENGTH_LONG).show();
					new Thread(new Runnable() {
						public void run() {
							try {
								String device = "http://" + dialServer.getIpAddress().getHostAddress() + ":" + dialServer.getPort();
								Log.d(LOG_TAG, "device=" + device);
								Log.d(LOG_TAG, "apps url=" + dialServer.getAppsUrl());

								// application instance url
								String location = null;
								String app = YOU_TUBE;

								DefaultHttpClient defaultHttpClient = HttpRequestHelper.createHttpClient();
								CustomRedirectHandler handler = new CustomRedirectHandler();
								defaultHttpClient.setRedirectHandler(handler);
								BasicHttpContext localContext = new BasicHttpContext();

								// check if any app is running
								HttpGet httpGet = new HttpGet(dialServer.getAppsUrl());
								httpGet.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
								httpGet.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
								httpGet.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
								httpGet.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
								httpGet.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
								httpGet.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
								HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
								if (httpResponse != null) {
									int responseCode = httpResponse.getStatusLine().getStatusCode();
									Log.d(LOG_TAG, "get response code=" + httpResponse.getStatusLine().getStatusCode());
									if (responseCode == 204) {
										// nothing is running
									} else if (responseCode == 200) {
										// app is running

										// Need to get real URL after a redirect
										// http://stackoverflow.com/a/10286025/594751
										String lastUrl = dialServer.getAppsUrl();
										if (handler.lastRedirectedUri != null) {
											lastUrl = handler.lastRedirectedUri.toString();
											Log.d(LOG_TAG, "lastUrl=" + lastUrl);
										}

										String response = EntityUtils.toString(httpResponse.getEntity());
										Log.d(LOG_TAG, "get response=" + response);
										parseXml(MainActivity.this, new StringReader(response));

										Header[] headers = httpResponse.getAllHeaders();
										for (int i = 0; i < headers.length; i++) {
											Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
										}

										// stop the app instance
										HttpDelete httpDelete = new HttpDelete(lastUrl);
										httpResponse = defaultHttpClient.execute(httpDelete);
										if (httpResponse != null) {
											Log.d(LOG_TAG, "delete response code=" + httpResponse.getStatusLine().getStatusCode());
											response = EntityUtils.toString(httpResponse.getEntity());
											Log.d(LOG_TAG, "delete response=" + response);
										} else {
											Log.d(LOG_TAG, "no delete response");
										}
									}

								} else {
									Log.i(LOG_TAG, "no get response");
									return;
								}

								// Check if app is installed on device
								int responseCode = getAppStatus(defaultHttpClient, dialServer.getAppsUrl() + app);
								if (responseCode != 200) {
									return;
								}
								parseXml(MainActivity.this, new StringReader(response));
								Log.d(LOG_TAG, "state=" + state);

								// start the app with POST
								HttpPost httpPost = new HttpPost(dialServer.getAppsUrl() + app);
								httpPost.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
								httpPost.setHeader(HEADER_ORIGN, HEADER_ORIGIN_VALUE);
								httpPost.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
								httpPost.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
								httpPost.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
								httpPost.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
								httpPost.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
								httpPost.setHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_TEXT_VALUE);
								// Set variable values as the body of the POST;
								// v is the YouTube video id.
								httpPost.setEntity(new StringEntity("pairingCode=eac4ae42-8b54-4441-9be3-d8a9abb5c481&v=cKG5HDyTW8o&t=0")); // http://www.youtube.com/watch?v=cKG5HDyTW8o

								httpResponse = defaultHttpClient.execute(httpPost, localContext);
								if (httpResponse != null) {
									Log.d(LOG_TAG, "post response code=" + httpResponse.getStatusLine().getStatusCode());
									response = EntityUtils.toString(httpResponse.getEntity());
									Log.d(LOG_TAG, "post response=" + response);
									Header[] headers = httpResponse.getHeaders("LOCATION");
									if (headers.length > 0) {
										location = headers[0].getValue();
										Log.d(LOG_TAG, "post response location=" + location);
									}

									headers = httpResponse.getAllHeaders();
									for (int i = 0; i < headers.length; i++) {
										Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
									}
								} else {
									Log.i(LOG_TAG, "no post response");
									return;
								}

								// Keep trying to get the app status until the
								// connection service URL is available
								state = STATE_STOPPED;
								do {
									responseCode = getAppStatus(defaultHttpClient, dialServer.getAppsUrl() + app);
									if (responseCode != 200) {
										break;
									}
									parseXml(MainActivity.this, new StringReader(response));
									Log.d(LOG_TAG, "state=" + state);
									Log.d(LOG_TAG, "connectionServiceUrl=" + connectionServiceUrl);
									Log.d(LOG_TAG, "protocol=" + protocol);
									try {
										Thread.sleep(1000);
									} catch (Exception e) {
									}
								} while (state.equals(STATE_RUNNING) && connectionServiceUrl == null);

								if (connectionServiceUrl == null) {
									Log.i(LOG_TAG, "connectionServiceUrl is null");
									return; // oops, something went wrong
								}

								// get the websocket URL
								String webSocketAddress = null;
								httpPost = new HttpPost(connectionServiceUrl); // "http://192.168.0.17:8008/connection/YouTube"
								httpPost.setHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
								httpPost.setHeader(HEADER_ORIGN, HEADER_ORIGIN_VALUE);
								httpPost.setHeader(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE);
								httpPost.setHeader(HEADER_DNT, HEADER_DNT_VALUE);
								httpPost.setHeader(HEADER_ACCEPT_ENCODING, HEADER_ACCEPT_ENCODING_VALUE);
								httpPost.setHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
								httpPost.setHeader(HEADER_ACCEPT_LANGUAGE, HEADER_ACCEPT_LANGUAGE_VALUE);
								httpPost.setHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_JSON_VALUE);
								httpPost.setEntity(new StringEntity("{\"channel\":0,\"senderId\":{\"appName\":\"ChromeCast\", \"senderId\":\"7v3zqrpliq3i\"}}"));

								httpResponse = defaultHttpClient.execute(httpPost, localContext);
								if (httpResponse != null) {
									responseCode = httpResponse.getStatusLine().getStatusCode();
									Log.d(LOG_TAG, "post response code=" + responseCode);
									if (responseCode == 200) {
										// should return JSON payload
										response = EntityUtils.toString(httpResponse.getEntity());
										Log.d(LOG_TAG, "post response=" + response);
										Header[] headers = httpResponse.getAllHeaders();
										for (int i = 0; i < headers.length; i++) {
											Log.d(LOG_TAG, headers[i].getName() + "=" + headers[i].getValue());
										}

										JSONObject jObject;
										try {
											jObject = new JSONObject(response); // {"URL":"ws://192.168.0.17:8008/session?33","pingInterval":0}
											webSocketAddress = jObject.getString("URL");
											Log.d(LOG_TAG, "webSocketAddress: " + webSocketAddress);
											int pingInterval = jObject.optInt("pingInterval"); // TODO
										} catch (JSONException e) {
											Log.e(LOG_TAG, "JSON", e);
										}
									}
								} else {
									Log.i(LOG_TAG, "no post response");
									return;
								}

								// Make a web socket connection for doing RAMP
								// to control media playback
								if (webSocketAddress != null) {
									// https://github.com/koush/android-websockets
									List<BasicNameValuePair> extraHeaders = Arrays.asList(new BasicNameValuePair(HEADER_ORIGN, HEADER_ORIGIN_VALUE),
											new BasicNameValuePair("Pragma", "no-cache"), new BasicNameValuePair("Cache-Control", "no-cache"),
											new BasicNameValuePair(HEADER_USER_AGENT, HEADER_USER_AGENT_VALUE));
									client = new WebSocketClient(URI.create(webSocketAddress), new WebSocketClient.Listener() { // ws://192.168.0.17:8008/session?26
												@Override
												public void onConnect() {
													Log.d(LOG_TAG, "Websocket Connected!");

													// TODO RAMP commands
												}

												@Override
												public void onMessage(String message) {
													Log.d(LOG_TAG, String.format("Websocket Got string message! %s", message));
												}

												@Override
												public void onMessage(byte[] data) {
													Log.d(LOG_TAG, String.format("Websocket Got binary message! %s", data));
												}

												@Override
												public void onDisconnect(int code, String reason) {
													Log.d(LOG_TAG, String.format("Websocket Disconnected! Code: %d Reason: %s", code, reason));
												}

												@Override
												public void onError(Exception error) {
													Log.e(LOG_TAG, "Websocket Error!", error);
												}

											}, extraHeaders);
									client.connect();
								} else {
									Log.i(LOG_TAG, "webSocketAddress is null");
								}

							} catch (Exception e) {
								Log.e(LOG_TAG, "run", e);
							}
						}
					}).start();
				}
			}
		}
	}

	/**
	 * Do HTTP GET for app status to determine response code and response body
	 * 
	 * @param defaultHttpClient
	 * @param url
	 * @return
	 */
	private int getAppStatus(DefaultHttpClient defaultHttpClient, String url) {
		int responseCode = 200;
		try {
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
			if (httpResponse != null) {
				responseCode = httpResponse.getStatusLine().getStatusCode();
				Log.d(LOG_TAG, "get response code=" + responseCode);
				response = EntityUtils.toString(httpResponse.getEntity());
				Log.d(LOG_TAG, "get response=" + response);
			} else {
				Log.i(LOG_TAG, "no get response");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "getAppStatus", e);
		}
		return responseCode;
	}

	/**
	 * Parse the App status description XML
	 * 
	 * @param context
	 * @param reader
	 */
	private void parseXml(Context context, Reader reader) {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(reader);
			int eventType = parser.getEventType();
			String lastTagName = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					String tagName = parser.getName();
					lastTagName = tagName;
					break;
				case XmlPullParser.TEXT:
					if (lastTagName != null) {
						if ("connectionSvcURL".equals(lastTagName)) {
							connectionServiceUrl = parser.getText();
						} else if ("state".equals(lastTagName)) {
							state = parser.getText();
						} else if ("protocol".equals(lastTagName)) {
							protocol = parser.getText();
						}
					}
					break;
				case XmlPullParser.END_TAG:
					tagName = parser.getName();
					lastTagName = null;
					break;
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "parseXml", e);
		}
	}

	/**
	 * @return list of recently connected devices
	 */
	public ArrayList<DialServer> getRecentlyConnected() {
		ArrayList<DialServer> devices = new ArrayList<DialServer>(recentlyConnected.values());
		Collections.reverse(devices);
		return devices;
	}

	/*
	 * Menu selection
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_switch:
			startActivityForResult(ServerFinder.createConnectIntent(this, target, getRecentlyConnected()), CODE_SWITCH_SERVER);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Custom HTTP redirection handler to keep track of the redirected URL
	 * ChromeCast web server will redirect "/apps" to "/apps/YouTube" if that is the active/last app
	 * 
	 */
	public class CustomRedirectHandler extends DefaultRedirectHandler {

		public URI lastRedirectedUri;

		@Override
		public boolean isRedirectRequested(HttpResponse response, HttpContext context) {

			return super.isRedirectRequested(response, context);
		}

		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {

			lastRedirectedUri = super.getLocationURI(response, context);

			return lastRedirectedUri;
		}

	}
}
