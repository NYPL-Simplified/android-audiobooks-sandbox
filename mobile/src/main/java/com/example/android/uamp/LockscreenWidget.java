package com.example.android.uamp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.android.uamp.utils.LogHelper;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link LockscreenWidgetConfigureActivity LockscreenWidgetConfigureActivity}
 * This is the widget's broadcast receiver.
 * The AppWidgetProvider class implements the onReceive() method, extracts the required information and calls the onUpdate/onDeleted/onEnabled/onDisabled widget life cycle methods.

 A widget has the same runtime restrictions as a normal broadcast receiver, i.e., it has only 5 seconds to finish its processing.

 A receive (widget) should therefore perform time consuming operations in a service and perform the update of the widgets from the service.

 *A widget gets its data on a periodic timetable. There are two methods to update a widget, one is based on an XML configuration file and the other is based on the Android AlarmManager service.
 *
 * In the widget configuration file you can specify a fixed update interval. The system will wake up after this time interval and call your broadcast receiver to update the widget. The smallest update interval is 1800000 milliseconds (30 minutes).

 The AlarmManager allows you to be more resource efficient and to have a higher frequency of updates. To use this approach, you define a service and schedule this service via the AlarmManager regularly. This service updates the widget.

 Please note that a higher update frequency will wake up the phone from the energy safe mode. As a result your widget consumes more energy.


 */
public class LockscreenWidget extends AppWidgetProvider {
	private static final String TAG = LogHelper.makeLogTag(LockscreenWidget.class);

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

		// Create an Intent to launch ExampleActivity
		//Intent intent = new Intent(context, ExampleActivity.class);
		//PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		CharSequence widgetText = LockscreenWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lockscreen_widget);
		views.setTextViewText(R.id.appwidget_text, widgetText);

		// Get the layout for the App Widget and attach an on-click listener to the button
		//views.setOnClickPendingIntent(R.id.button, pendingIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}


	/**
	 * Called for every update of the widget. Contains the ids of appWidgetIds for which an update is needed. Note that this may be all of the AppWidget instances for this provider, or just a subset of them, as stated in the method’s JavaDoc. For example, if more than one widget is added to the home screen, only the last one changes (until reinstall).
	 *
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetIds
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}


	/**
	 * Widget instance is removed from the home screen.
	 * @param context
	 * @param appWidgetIds
	 */
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// When the user deletes the widget, delete the preference associated with it.
		for (int appWidgetId : appWidgetIds) {
			LockscreenWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
		}
	}


	/**
	 * Called the first time an instance of your widget is added to the home screen.
	 * not sure what does for lockscreen widgets
	 * @param context
	 */
	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
		Log.d(TAG, "onEnabled\n");
	}


	/**
	 * Called once the last instance of your widget is removed from the home screen.
	 * not sure what does for lockscreen widgets
	 * @param context
	 */
	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}
}

