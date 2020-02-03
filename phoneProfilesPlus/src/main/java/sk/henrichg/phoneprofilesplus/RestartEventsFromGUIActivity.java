package sk.henrichg.phoneprofilesplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import me.drakeet.support.toast.ToastCompat;

public class RestartEventsFromGUIActivity extends AppCompatActivity
{
    private boolean activityStarted = false;

    private DataWrapper dataWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        //PPApplication.logE("RestartEventsFromGUIActivity.onCreate", "xxx");

        PhoneProfilesService instance = PhoneProfilesService.getInstance();
        if (instance == null) {
            return;
        }

        if (instance.getWaitForEndOfStart()) {
            return;
        }

        activityStarted = true;

        // close notification drawer - broadcast pending intent not close it :-/
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(it);

        dataWrapper = new DataWrapper(getApplicationContext(), false, 0, false);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        boolean applicationStarted = PPApplication.getApplicationStarted(true);
        boolean waitForEndOfStart = false;
        if (applicationStarted) {
            PhoneProfilesService instance = PhoneProfilesService.getInstance();
            waitForEndOfStart = instance.getWaitForEndOfStart();
            applicationStarted = !waitForEndOfStart;
        }
        if (!applicationStarted) {
            String text = getString(R.string.app_name) + " " + getString(R.string.application_is_not_started);
            if (waitForEndOfStart)
                text = getString(R.string.app_name) + " " + getString(R.string.application_is_starting_toast);
            Toast msg = ToastCompat.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
            msg.show();
            finish();
            return;
        }

        if (activityStarted) {
            // set theme and language for dialog alert ;-)
            // not working on Android 2.3.x
            GlobalGUIRoutines.setTheme(this, true, false/*, false*/, false);
            //GlobalGUIRoutines.setLanguage(this);

            //dataWrapper.addActivityLog(DatabaseHandler.ALTYPE_RESTARTEVENTS, null, null, null, 0);

            //PPApplication.logE("RestartEventsFromGUIActivity.onStart", "xxx");
            dataWrapper.restartEventsWithAlert(this);
        }
        else
            finish();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //dataWrapper.invalidateDataWrapper();
        dataWrapper = null;
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(0, 0);
    }

}