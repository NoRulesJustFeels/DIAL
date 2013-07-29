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

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

public class Analytics {
	private static final String LOG_CAT = "Analytics";

	public static final String ANALYTICS = "Analytics";

	private static Context context;

	public static void createAnalytics(Context context) {
		try {
			Analytics.context = context;
			EasyTracker.getInstance().setContext(context);
		} catch (Exception e) {
			Log.e(LOG_CAT, "createAnalytics", e);
		}
	}

	public static void startAnalytics(final Activity activity) {
		try {
			if (activity != null && activity.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getInstance().activityStart(activity);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "startAnalytics", e);
		}
	}

	public static void stopAnalytics(Activity activity) {
		try {
			if (activity != null && activity.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getInstance().activityStop(activity);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "stopAnalytics", e);
		}
	}

	public static void logEvent(String event) {
		try {
			if (context != null && context.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getTracker().trackEvent(ANALYTICS, event, event, 1L);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "logEvent", e);
		}
	}

	public static void logEvent(String event, Map<String, String> parameters) {
		try {
			if (context != null && context.getResources().getInteger(R.integer.development) == 0) {
				EasyTracker.getTracker().trackEvent(ANALYTICS, event, event, 1L);
			}
		} catch (Exception e) {
			Log.e(LOG_CAT, "logEvent", e);
		}
	}
}
