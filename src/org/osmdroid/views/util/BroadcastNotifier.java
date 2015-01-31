package org.osmdroid.views.util;

import android.content.Context;
import android.content.Intent;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Tony on 30-Jan-15.
 */
public class BroadcastNotifier {
    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "org.osmdroid.views.util.BROADCAST";
    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "org.osmdroid.views.util.STATUS";

    // Defines the key for the log "extra" in an Intent
    public static final String EXTENDED_STATUS_LOG = "org.osmdroid.views.util.LOG";

    private LocalBroadcastManager mBroadcaster;

    public BroadcastNotifier(Context context) {

        // Gets an instance of the support library local broadcastmanager
        mBroadcaster = LocalBroadcastManager.getInstance(context);

    }

    public void broadcastIntentWithState(int status) {

        Intent localIntent = new Intent();

        // The Intent contains the custom broadcast action for this app
//        localIntent.setAction(BROADCAST_ACTION);

        // Puts the status into the Intent
        localIntent.putExtra("unzip_status", status);
//        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Broadcasts the Intent
        mBroadcaster.sendBroadcast(localIntent);

    }

}
