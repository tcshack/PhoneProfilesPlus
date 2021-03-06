package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;

class MobileDataStateChangedContentObserver extends ContentObserver {

    //public static boolean internalChange = false;

    private static boolean previousState = false;

    private final Context context;

    MobileDataStateChangedContentObserver(Context c, Handler handler) {
        super(handler);

        context=c;

        previousState = ActivateProfileHelper.isMobileData(context);
    }

    /*
    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }
    */


    @Override
    public void onChange(boolean selfChange, Uri uri) {
//        PPApplication.logE("[IN_OBSERVER] MobileDataStateChangedContentObserver.onChange", "uri="+uri);

        //CallsCounter.logCounter(context, "MobileDataStateChangedContentObserver.onChange", "MobileDataStateChangedContentObserver_onChange");

        if (PPApplication.HAS_FEATURE_TELEPHONY) {
            boolean actualState = ActivateProfileHelper.isMobileData(context);
            if (previousState != actualState) {

                if (Event.getGlobalEventsRunning()) {
                    final Context appContext = context.getApplicationContext();
                    PPApplication.startHandlerThreadBroadcast(/*"MobileDataStateChangedContentObserver.onChange"*/);
                    final Handler handler = new Handler(PPApplication.handlerThreadBroadcast.getLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", "START run - from=MobileDataStateChangedContentObserver.onChange");

                            PowerManager powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                            PowerManager.WakeLock wakeLock = null;
                            try {
                                if (powerManager != null) {
                                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PPApplication.PACKAGE_NAME + ":MobileDataStateChangedContentObserver_onChange");
                                    wakeLock.acquire(10 * 60 * 1000);
                                }

//                                PPApplication.logE("[EVENTS_HANDLER_CALL] MobileDataStateChangedContentObserver.onChange", "sensorType=SENSOR_TYPE_RADIO_SWITCH");
                                EventsHandler eventsHandler = new EventsHandler(appContext);
                                eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_RADIO_SWITCH);

                                //PPApplication.logE("****** EventsHandler.handleEvents", "END run - from=MobileDataStateChangedContentObserver.onChange");
                            } catch (Exception e) {
//                                PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", Log.getStackTraceString(e));
                                PPApplication.recordException(e);
                            } finally {
                                if ((wakeLock != null) && wakeLock.isHeld()) {
                                    try {
                                        wakeLock.release();
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    });
                }

                previousState = actualState;
            }
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

}
