package it.linup.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import android.app.Activity;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

//import com.facebook.react.modules.core.DeviceEventManagerModule;
//import com.idra.services.NotificationService;
import it.linup.cordova.plugin.services.WebsocketService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import tech.gusavila92.websocketclient.WebSocketClient;

import it.linup.cordova.plugin.utils.LogUtils;
import it.linup.cordova.plugin.utils.FileUtils;


public class WebSocketNativeModule extends CordovaPlugin {

    private String tag = "==:== WebSocketNativeModule :";

    // Event types for callbacks
    private enum Event { ACTIVATE, DEACTIVATE, FAILURE }

    // Plugin namespace
    private static final String JS_NAMESPACE = "cordova.plugin.WebsocketServicePlugin";

    private static Handler m_handler;
    private Runnable m_handlerTask;
    private static WebSocketNativeModule instance;
    protected static boolean isConnected = false;
    private String info = "";
    protected static boolean requestHeartBit = false;
    protected static int failedHeartBit = 0;
    protected static JSONArray lstUser;
    protected static boolean isEnableHearbitCheck;
    private static String uriToReconnect="";

    public static boolean active = true;

    private CallbackContext callbackContext;

    public static WebSocketNativeModule getInstance() {
        return instance;
    }




    public WebSocketNativeModule() {
        instance = this;
        isEnableHearbitCheck = false;
        if (m_handler == null) {
            m_handler = new Handler(Looper.getMainLooper());
            m_handlerTask = new Runnable() {
                @Override
                public void run() {

                    //checkAndSendHeartBit();
                }
            };
            m_handlerTask.run();
        }
    }

    public void kill() {
        m_handler.removeCallbacks(m_handlerTask);
        m_handler = null;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking)
    {
        try {
            active = false;

        } finally {
            //clearKeyguardFlags(cordova.getActivity());
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop () {
        //clearKeyguardFlags(cordova.getActivity());
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume (boolean multitasking)
    {
        active = true;
        //stopService();
    }

    @Override
    public boolean execute(String action, JSONArray args,
                           final CallbackContext callbackContext) {
        try {
            JSONObject options = null;
            if (args != null && args.length() > 0)
                options = args.getJSONObject(0);
            switch (action) {
                case "enable":
                    Boolean isEnable = options.getBoolean("isEnable");
                    if (isEnable != null)
                        this.enable(isEnable);
                    break;
                case "connect":
                    String uri = options.getString("uri");
                    this.callbackContext = callbackContext;
                    if (uri != null)
                        this.connect(uri);
                    break;
                case "setInfo": {
                    String params = options.getString("params");
                    if (params != null)
                        this.setInfo(params);
                    break;
                }
                case "send": {
                    String params = options.getString("params");
                    if (params != null)
                        this.send(params);
                    break;
                }
                case "checkModule":
                    this.checkModule();
                    break;
                case "close":
                    this.close();
                    this.callbackContext = null;
                    break;
                case "show": {
                    String message = options.getString("message");
                    String duration = options.getString("duration");
                    this.show(message, duration);
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    callbackContext.sendPluginResult(pluginResult);
                    break;
                }
                default:
                    callbackContext.error("\"" + action + "\" is not a recognized action.");
                    return false;
            }
        } catch(JSONException e) {
            callbackContext.error("Error encountered: " + e.getMessage());
            return false;
        }

        return true;
    }

    public void show(String message, String duration) {
        Toast toast = Toast.makeText(cordova.getActivity(), message,
                "long".equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        // Display toast
        toast.show();
    }

    //@ReactMethod
    public void enable(boolean isEnable) {
        isEnableHearbitCheck = isEnable;
        failedHeartBit = 0;
    }

    //@Override
    public String getName() {
        return "WebSocketNativeModule";
    }

    public static WebSocketClient _sock;
    private static String uri;


    public static void sendMesg(String mess)
    {
        _sock.send("{\"event\":\"heartbit\",\"data\":\""+mess+"\"}");
    }



    public String getCallName(String from) {

        String name = "";
        try {
            for (int i = 0; i < lstUser.length(); i++) {
                if (lstUser.getJSONObject(i).has("token")&&lstUser.getJSONObject(i).getString("token").equals(from)) {
                    name = lstUser.getJSONObject(i).getString("name");
                    break;
                }
            }
        } catch (Exception e) {

        }
        return name;
    }


    // DA SPOSTARE IN ALTRI LUOGHI E DA CAPIERE SE SERVE VERAMENTE SE SI TOGLIE L?APP SCHIATTA PERCHE' VIENE CHIAMTA DALLA PARTE NATIVA
    //@ReactMethod
    /*public void doMute(){
        AudioManager audioManager = (AudioManager) getCurrentActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        if (audioManager.isMicrophoneMute() == false) {
            audioManager.setMicrophoneMute(true);
        } else {
            audioManager.setMicrophoneMute(false);
        }
    }*/


    public Activity getCurrentActivity() {
        return this.cordova.getActivity();
    }

    //@ReactMethod
    public void connect(String uri) {
        FileUtils.writeToFile("idrauri", uri, this.getApplicationContext());
        Intent i  = new Intent(this.getApplicationContext(), WebsocketService.class);
        i.putExtra("idrauri",uri);
        this.getCurrentActivity().startService(i);

        // BISOGNA GESTIRE IL CASO IN CUI LA CONNESSIONE E' ATTIVA E VOGLIO CAMBIARE SERVER
        WebsocketService.instance().connect(uri);
    }



    public void updateNotification(Boolean status) {
        try
        {
            LogUtils.printLog(tag,"NOTIFICATION SERVICE " + status);
            //NotificationService.instance().updateNotification(status, getApplicationContext(), getCurrentActivity());
        }
        catch (Exception e) {
            LogUtils.printLog(tag,"ERRORE IN NOTIFICATION SERVICE");

        }
    }

    public void sendEvent(String eventName, String params) {
        try {
            LogUtils.printLog(tag,"SendEventToJS " + eventName);
            //WebSocketNativeModule.this.getApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
            JSONObject obj = new JSONObject();
            obj.put("event", eventName);
            obj.put("param", params);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
            this.callbackContext.sendPluginResult(pluginResult);

        } catch (Exception e) {
            LogUtils.printLog(tag,"problem in sendEvent");
            e.printStackTrace();
        }
    }

    public Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    //@ReactMethod
    public void setInfo(String params) {
        info = params;
    }

    //@ReactMethod
    public void send(String params) {
        //if(_sock!=null) {
        //_sock.send(params);
        new SendOperation().execute(params);


    }
    //@ReactMethod
    public void checkModule() {
        sendEvent("onCheckModule", "{\"failed:\"" + this.failedHeartBit + ",\"connected\":"+this.isConnected+"}" );
    }

    //@ReactMethod
    public void close() {
        FileUtils.writeToFile("idrauri","", this.getApplicationContext());
        WebsocketService.instance().closeSocket();
    }

    private class SendOperation extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if(params != null && params.length == 1) {
                WebsocketService.instance().send(params);
                //_sock.send(params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    /**
     * Fire vent with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    private void fireEvent (Event event, String params)
    {
        String eventName = event.name().toLowerCase();
        Boolean active   = event == Event.ACTIVATE;

        String str = String.format("%s._setActive(%b)",
                JS_NAMESPACE, active);

        str = String.format("%s;%s.on('%s', %s)",
                str, JS_NAMESPACE, eventName, params);

        str = String.format("%s;%s.fireEvent('%s',%s);",
                str, JS_NAMESPACE, eventName, params);

        final String js = str;

        cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
    }

}
