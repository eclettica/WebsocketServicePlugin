package it.linup.cordova.plugin.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.content.res.Resources;

import org.json.JSONObject;



//import com.idra.R;
import it.linup.cordova.plugin.utils.LogUtils;


public class NotificationService  {


    private static enum NotificationServiceSingleton {
        INSTANCE;

        NotificationService singleton = new NotificationService();

        public NotificationService getSingleton() {
            return singleton;
        }

    }


    private NotificationService() {
        LogUtils.printLog(TAG,"NOTIFICATION SERVICE SINGLETON CREATION");
    }

    public String uri;
    private static final String TAG = "NotificationService ";

    private String userCompleteName = "";
    private String userEmail = "";
    private Integer numNotification = 0;
    private Boolean status;

    private static JSONObject defaultSettings = new JSONObject();


    // Default title of the background notification
    private static final String NOTIFICATION_TITLE =
            "App is running in background";

    // Default text of the background notification
    private static final String NOTIFICATION_TEXT =
            "Doing heavy tasks.";

    // Default icon of the background notification
    private static final String NOTIFICATION_ICON = "ic_launcher";

    // Activity to handle the click event
    private Class<?> clickActivity;

    public void setUserName(String userName) {
        this.userCompleteName = userName;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setNumNotification(Integer numNotification){
        this.numNotification = numNotification;
    }

    public void setNumNotification(String numNotification){
        try{
            Integer nn = Integer.parseInt(numNotification);
            this.numNotification = nn;
            LogUtils.printLog(TAG,"Num notification parsing " + nn);

        } catch(Exception e){
            LogUtils.printLog(TAG,"Num notification parsing error");
        }
    }

    private final String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";


    public static NotificationService instance() {
        return NotificationService.NotificationServiceSingleton.INSTANCE.getSingleton();
    }

    public void configure(JSONObject settings)
    {
        LogUtils.printLog(TAG,"configure: " + (settings != null ? settings : "null"));

        this.defaultSettings = settings;
        //NotificationService.instance().configure(this.defaultSettings);
    }

    /**
     * Set click activity.
     *
     * @param activity The activity to handler the click event.
     */
    public NotificationService setClickActivity(Class<?> activity) {
        this.clickActivity = activity;
        return this;
    }

    public void updateNotification(Boolean status, Context c, Class<?> caClass) {


        Notification notification = makeNotification(status, null, c, caClass);
        NotificationManager manager = getNotificationManager(c);
        manager.notify(9999, notification);

        /*if (ca != null) {
            manager = (NotificationManager) ca.getSystemService(ca.NOTIFICATION_SERVICE);
            manager.notify(9999, notification);
        }*/
    }

    public void updateNotification(Boolean status, Integer notNum, Context c, Class<?> caClass) {
        LogUtils.printLog(TAG,"updateNotification " + (notNum != null ? notNum : "null"));
        LogUtils.printLog(TAG,"updateNotification context " + (c != null ? c : "null"));
        LogUtils.printLog(TAG,"updateNotification caClass " + (caClass != null ? caClass : "null"));
        Notification notification = makeNotification(status, notNum, c, caClass);
        NotificationManager manager = getNotificationManager(c);
        manager.notify(9999, notification);

    }
    /*private Notification makeNotification(Boolean status, Context c, Class<?> caClass) {
        return makeNotification(status, null, c, caClass);
    }*/

    private Notification makeNotification(Boolean status, Integer notNum, Context c, Class<?> caClass) {
        LogUtils.printLog(TAG,"makeNotification " + (notNum != null ? notNum : "null"));

        if(status == null) {
            status = this.status;
        } else {
            this.status = status;
        }
        if(status == null) {
            status = false;
        }
        NotificationCompat.Builder builder = null;
        LogUtils.printLog(TAG,"makeNotification1 " + (notNum != null ? notNum : "null"));
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager notificationManager = getNotificationManager(c);
                notificationChannel.setDescription("Channel description");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationManager.createNotificationChannel(notificationChannel);
            } else {
            }
        } catch(Exception e) {
            LogUtils.printLog(TAG,"makeNotification-- " + e.getMessage());
        }
        builder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID);
        LogUtils.printLog(TAG,"makeNotification2 " + (notNum != null ? notNum : "null"));
        //builder = new NotificationCompat.Builder(c);
        String statusText = "-";
        //builder.setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.mipmap.ic_idra));
        int iconR = getSmallIcon(status, c);
        //LogUtils.printLog(TAG,"Small icon " + iconR);
        builder.setSmallIcon(iconR);
        if(status){
            statusText = "Online";

            LogUtils.printLog(TAG,"Notification online ");
            //builder.setLargeIcon(R.mipmap.ic_idra);
        } else {
            statusText = "Offline";
            //builder.setSmallIcon(R.mipmap.ic_offline);
        }
        LogUtils.printLog(TAG,"makeNotification3 " + (notNum != null ? notNum : "null"));

        builder.setTicker(defaultSettings.optString("title", "WebSocketService ticker" ));

        String terza = ""; //status + " ";
        LogUtils.printLog(TAG,"notNum " + (notNum != null ? notNum : "null"));
        if(notNum != null)
            this.numNotification = notNum;
            
        if(notNum != null && notNum > 0) {
            statusText += " - " + "notifiche: " + notNum;
            terza += "notifiche: " + notNum;
        } else if(this.numNotification > 0) {
            terza += "notifications: " + this.numNotification;
            statusText += " - " + "notifiche: " + this.numNotification;
        }

        LogUtils.printLog(TAG,"set title: " + (defaultSettings != null ? defaultSettings : "null"));
        builder.setContentTitle(defaultSettings.optString("title", "WebSocketService title" ));
        //builder.setContentText(statusText + " - " + defaultSettings.optString("text", "" ));
        builder.setContentText(statusText);
        //builder.setContentTitle("WebSocketService - "  + this.userCompleteName);
        //builder.setContentText(statusText + " - " + this.userEmail);

        builder.setSubText(terza);
        if(caClass != null ) {
            Intent t = new Intent(c, caClass);
            PendingIntent pi = PendingIntent.getActivity(c, 0, t, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pi);
        }
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);

        return builder.build();

    }

    private int getSmallIcon(Boolean status, Context c) {
        if(status == null)
            status = false;
        String ict = "icon_";
        if(status)
            ict += "online";
        else
            ict += "offline";
        String icon = null;
        try {
            icon = defaultSettings.getString(ict);
        } catch(Exception ex){}
        if(icon == null)
            icon = defaultSettings.optString("icon", NOTIFICATION_ICON);

        int resId = getIconResId(icon, "mipmap", c);

        if (resId == 0) {
            resId = getIconResId(icon, "drawable", c);
        }

        return resId;
    }

    /*private int getIconResId (JSONObject settings)
    {
        String icon = settings.optString("icon", NOTIFICATION_ICON);

        int resId = getIconResId(icon, "mipmap");

        if (resId == 0) {
            resId = getIconResId(icon, "drawable");
        }

        return resId;
    }*/

    /**
     * Retrieve resource id of the specified icon.
     *
     * @param icon The name of the icon.
     * @param type The resource type where to look for.
     *
     * @return The resource id or 0 if not found.
     */
    private int getIconResId (String icon, String type, Context c)
    {
        Resources res  = c.getResources();
        String pkgName = c.getPackageName();

        int resId = res.getIdentifier(icon, type, pkgName);

        if (resId == 0) {
            resId = res.getIdentifier("icon", type, pkgName);
        }

        return resId;
    }

    private NotificationManager getNotificationManager(Context c) {
        LogUtils.printLog(TAG,"getNotificationManager context " + (c != null ? c.getClass() : "null"));
        LogUtils.printLog(TAG,"getNotificationManager NOTIFICATION_SERVICE " + (c.NOTIFICATION_SERVICE != null ? c.NOTIFICATION_SERVICE : "null"));
        return (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
    }


    private void getMainActivity(Context context) {
        String  packageName = context.getPackageName();
        Intent  launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String  className = launchIntent.getComponent().getClassName();

        try {
            //loading the Main Activity to not import it in the plugin
            Class mainActivity = Class.forName(className);
            setClickActivity(mainActivity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void clear(Context c, Activity ca) {

        if(clickActivity == null) {
            getMainActivity(c);
        }

        if(clickActivity == null)
            return;
        Intent t = new Intent(c, clickActivity);
        PendingIntent pi = PendingIntent.getActivity(c, 0, t, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(c,NOTIFICATION_CHANNEL_ID);
        //builder.setSmallIcon(R.mipmap.ic_idra);
        builder.setTicker("IDRA");
        builder.setContentTitle("IDRA");
        builder.setContentText("Synchronize Service");
        builder.setContentIntent(pi);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);

        Notification notification = builder.build();
        if(ca != null) {
            NotificationManager manager = (NotificationManager) ca.getSystemService(ca.NOTIFICATION_SERVICE);
            manager.notify(9999, notification);
        }
    }



}