package it.linup.cordova.plugin.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import android.os.Binder;

//import com.idra.modules.DbSql.DBManager;
//import com.idra.modules.DbSql.DbCustomLogic;
import it.linup.cordova.plugin.WebsocketServicePlugin;
import it.linup.cordova.plugin.services.NotificationService;
import it.linup.cordova.plugin.services.SendOperation;
import it.linup.cordova.plugin.utils.LogUtils;

import it.linup.cordova.plugin.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;


import tech.gusavila92.websocketclient.WebSocketClient;

//import org.pgsqlite.SQLitePlugin;
//import org.pgsqlite.SQLitePluginPackage;

public class WebsocketService extends Service  {

    public static WebsocketServicePlugin plugin;
    public static String uri;
    public static String origUri;
    private boolean isConnected = false;
    protected static boolean requestHeartBit = false;
    protected static int failedHeartBit = 0;
    protected static JSONArray lstUser;
    protected static boolean isEnableHearbitCheck;
    public static WebSocketClient _sock;
    private Handler m_handler;
    private Runnable m_handlerTask;
    public static boolean reconnect = true;
    public static Context mContext;

    protected static Set<WebsocketListnerInterface> listners = new HashSet<WebsocketListnerInterface>();

    public static String tag="WEBSOCKETSERVICE - WebsocketService";

	// Binder given to clients
    private final IBinder binder = new ForegroundBinder();

     /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class ForegroundBinder extends Binder
    {
        public WebsocketService getService()
        {
            // Return this instance of ForegroundService
            // so clients can call public methods
            return WebsocketService.this;
        }
    }


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public WebsocketService() {

    }

    public void addListner(WebsocketListnerInterface impl) {
        listners.add(impl);
    }

    public void setPlugin(WebsocketServicePlugin plugin) {
        LogUtils.printLog(tag," SET PLUGIN");
        WebsocketService.plugin = plugin;
    }

    public WebsocketServicePlugin getPlugin() {
        return plugin;
    }

    public void send(String[] params) {
        LogUtils.printLog(tag," send " + params[0]);
        if(params != null && params.length == 1 && _sock != null) {
            _sock.send(params[0]);
        }
    }

    public void asyncSend(String... params) {
        LogUtils.printLog(tag," asyncSend " + params[0]);
        new SendOperation().execute(params);
    }

    public boolean getConnected() {
        if(_sock == null)
            this.isConnected = false;
        return this.isConnected;
    }

    private static enum WebsocketServiceSingleton {
        INSTANCE;

        WebsocketService singleton = new WebsocketService();

        public WebsocketService getSingleton() {
            return singleton;
        }

    }

    public static WebsocketService instance() {
        return WebsocketService.WebsocketServiceSingleton.INSTANCE.getSingleton();
    }

    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MYSERVICE:ALLARM");
        super.onCreate();
        mContext = getApplicationContext();
        wl.acquire();

        LogUtils.printLog(tag," >>>onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
	return binder;
    }

    /*@Override
    protected void onHandleIntent(@Nullable Intent intent) {
        LogUtils.printLog(tag,"WEBSOCKETSERVICE onHandleIntent");
        this.uri = intent.getStringExtra("uri");
        _onStartCommand();
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, startId, startId);
        String uri = null;
        if(intent != null) {
            this.uri = intent.getStringExtra("websocketserviceuri");
            uri = this.uri;
        } else {
            LogUtils.printLog(tag," intent is null");
        }
        _onStartCommand(uri, true);

        // PREFERISCO UTILIZZARE LO START_STICKY PERCHE' NEL CASO DI CAMBIO SERVER ATTRVERSO IL CONNECT DI WebSocketNativeModulo riscrivo l'uri e reinizializzo tutto
        return START_STICKY;

        //return START_REDELIVER_INTENT;
    }

    private static Thread tProcess = null;

    public void publicStartCommand(String uri) {
        this._onStartCommand(uri, false);
    }

    private void _onStartCommand(String uri, boolean canReadFromFile){
        LogUtils.printLog(tag," onStartCommand " + uri);
        this.origUri = this.uri;
        LogUtils.printLog(tag," onStartCommand origUri:" + uri);
        /*if(uri == null && this.uri == null) {
            uri = FileUtils.readFromFile("websocketserviceuri", this);
            this.uri = uri;
        } else if(uri != null) {
            this.uri = uri;
        }*/
        this.uri = uri;
        if(canReadFromFile) {
            LogUtils.printLog(tag," this is null??? " + (this == null ? "true" : "false"));
            this.uri = FileUtils.readFromFile("websocketserviceuri", this);
        }

        if (tProcess == null) {
            tProcess = new Thread(new Runnable() {
                public void run() {
                    LogUtils.printLog(tag," start NEW THREED ");

                    while (true) {
                        try {
                            if ((!checkUriEquals() || WebsocketService._sock == null) && WebsocketService.reconnect) {

                                _connect();
                                LogUtils.printLog(tag," SOCKET IN ATTIVAZIONE");
                                Thread.sleep(10000);
                            } else {
                                if(WebsocketService._sock == null) {
                                    LogUtils.printLog(tag, " SOCKET NON ATTIVATA");
                                    updateNotification(false);
                                } else {
                                    LogUtils.printLog(tag, " SOCKET ATTIVATA");
                                }
                                Thread.sleep(5000);
                            }

                        } catch (InterruptedException e) {
                            //e.printStackTrace();
                            LogUtils.printLog(tag," SOCKET IN ERRORE");

                        }
                    }
                }
            });
            tProcess.start();
            updateNotification(false);

        }
        else {
            LogUtils.printLog(tag, " start command fired");
            WebsocketService.reconnect = true;
        }
    }

    public boolean checkUriEquals() {
        if(this.uri == null && this.origUri == null)
            return true;
        if(this.uri == null)
            return false;
        return this.uri.equals(this.origUri);
    }

    public void connect(String uri) {
        this.uri = uri;
        this._connect();
    }


    public void closeConnectionIfNeeded(){
        if(this._sock != null) {
            try {
                LogUtils.printLog(tag," ON OPEN - closing old socket...");
                isConnected = false;
                this._sock.close();
                this._sock = null;
            } catch (Exception e) {
                Log.d("Error",  e.getMessage());
            }
        }
    }

    public void _connect(){
        reconnect = false;
        this.origUri = this.uri;
        if (this.uri == null) {
            LogUtils.printLog(tag," URI IS NULL");
            this.closeConnectionIfNeeded();
            reconnect = true;
            return;
        } else {
            LogUtils.printLog(tag," URI IS " + uri);
            //NotificationService.instance().uri = this.uri;
        }
        if(this.uri == null || this.uri.trim().isEmpty()){
            LogUtils.printLog(tag," URI IS NULL!!!");
            reconnect = true;
            return;
        }
        this.startHeartbitService();
        this.closeConnectionIfNeeded();
        LogUtils.printLog(tag," OPENING");

        this._sock = new WebSocketClient(URI.create(uri))
        {
            @Override
            public void onOpen() {
                LogUtils.printLog(tag," ON OPEN");
                sendEvent("onWebsocketConnect", "");
                updateNotification(true);
                isConnected = true;
                reconnect = true;
                requestHeartBit = false;
                failedHeartBit = 0;
                isEnableHearbitCheck = true;
                sendToListner("onWebsocketConnect", "");
            }

            @Override
            public void onTextReceived(String message) {

                sendToListner("onWebsocketMessage", message);
                //sendEvent("onWebsocketMessage", message);

                JSONObject jobj = null;

                try {
                    try {
                        jobj = new JSONObject(message);
                    } catch (Exception e) {
                        if (message.equals("not logged")) {
                            jobj = new JSONObject();
                            jobj.put("message", message);
                            jobj.put("event", message);
                            FileUtils.writeToFile("websocketserviceuri", "", mContext);
                            closeSocket();
                        }
                    }
                    if (jobj != null) {
                        String evt = jobj.getString("event");
                        if ("heartbit".equals(evt)) {
                            LogUtils.printLog(tag, "isheartbit...");
                            requestHeartBit = false;
                            failedHeartBit = 0;
                            LogUtils.printLog(tag, "isheartbit... " + failedHeartBit + " " + requestHeartBit);
                        } else if("forcelogout".equals(evt)) {
                            LogUtils.printLog(tag, "FORCELOGOUT!!!");
                            FileUtils.writeToFile("websocketserviceuri", "", mContext);
                            uri = null;
                            closeSocket();
                            stopHeartbitService();
                            sendToListner("onWebsocketForceLogout", "");
                            sendEvent("onWebsocketForceLogout", "");
                        }
                    }
                } catch (JSONException ex) {
                    LogUtils.printLog(tag, "Errore " + ex.toString());
                }

                updateNotification(true);
            }



            @Override
            public void onBinaryReceived(byte[] data) {
                sendEvent("onWebsocketMessage", data.toString());
                sendToListner("onWebsocketMessage", data.toString());
                updateNotification(true);
            }

            @Override
            public void onPingReceived(byte[] data) {

            }

            @Override
            public void onPongReceived(byte[] data) {

            }


            @Override
            public void onException(Exception e) {
                LogUtils.printLog(tag,"WEBSOCKET ON EXCEPTION");
                LogUtils.printLog(tag,"WEBSOCKET ON EXCEPTION " + e.getMessage());
                e.printStackTrace();
                sendToListner("onWebsocketException", "");
                // SE reconnect è false, è perchè sto effettuando l'apertura della socket;
                // pertanto se ho un'eccezione è, ad esempio, perchè sto riavviando il server;
                // quindi in questo caso devo rimettere reconnect a true in modo da effettuare un nuovo tentativo
                if( reconnect == false) {
                    reconnect = true;
                    isConnected = false;
                    _sock=null;
                }
                //e.printStackTrace();
                //isConnected = false;
                if(plugin!=null) {
                    plugin.sendEvent("onWebsocketException", "");
                    //updateNotification(false);
                    LogUtils.printLog(tag,"WEBSOCKET ON EXCEPTION INVIO FALSE ALL'UPDATE NOTIFICATION");
                }
            }

            @Override
            public void onCloseReceived() {
                LogUtils.printLog(tag,"WEBSOCKET ON CLOSE");

                isConnected = false;
                isEnableHearbitCheck = false;
                _sock=null;
                reconnect = true;
                sendToListner("onWebsocketClose", "");
                if(plugin!=null) {
                    plugin.sendEvent("onWebsocketClose", "");
                    updateNotification(false);
                }


            }
        };
        try{
            _sock.connect();
        }
        catch(Exception e){
            LogUtils.printLog(tag,"EXCEPTION " + e.getMessage());
            // DA EVIDENZIARE CON UNA FINESTRA DI DIALOGO CHE C'E' UN COFLITTO DI SOCKET
        }
    }


    private void sendToListner(String event, String data) {
        for(WebsocketListnerInterface listner: listners) {
            listner.onEvent(event, data);
        }
        if(plugin != null) {
            plugin.sendEvent(event, data);
        }
    }


    private void updateNotification(boolean b) {
        Class mainClass = null;
        if(plugin != null) {
            mainClass = plugin.getMainActivityClass();
        }
        NotificationService.instance().updateNotification(b, this, mainClass);
    }

    private void sendEvent(String event, String data) {
        if(plugin != null)
            plugin.sendEvent(event, data);
    }

    private void startHeartbitService() {
        isEnableHearbitCheck = false;
        if (m_handler == null) {
            m_handler = new Handler(Looper.getMainLooper());
            m_handlerTask = new Runnable() {
                @Override
                public void run() {
                    checkAndSendHeartBit();
                }
            };
            m_handlerTask.run();
        }
    }

    private void stopHeartbitService() {
        isEnableHearbitCheck = false;
        if (m_handler != null && m_handlerTask != null) {
            m_handler.removeCallbacks(m_handlerTask);
            m_handler = null;
            m_handlerTask = null;
        }
    }

    public static int cntisEnableHearbitCheck =0;

    public int getFailedHeartBit() {
        return failedHeartBit;
    }

    public boolean getIsConnected() {
        return this.isConnected;
    }

    private void checkAndSendHeartBit() {
        //Log.d("WEBSOCKETSERVICE ahihi", "send heartbit");
        LogUtils.printLog(tag," checkAndSendHeartBit " + isEnableHearbitCheck);

        if (isEnableHearbitCheck) {
            if (requestHeartBit) {
                failedHeartBit += 1;
                if (failedHeartBit >= 3) {
                    LogUtils.printLog(tag," ON SOCKET TIMEOUT " + failedHeartBit + " " + uri);
                    sendEvent("onSocketStatusTimeout", "ok");
                    updateNotification(false);
                    isEnableHearbitCheck=false;
                    closeConnectionIfNeeded();
                }
            }

            LogUtils.printLog(tag," sock: " + _sock + " isconnected " + isConnected);
            if (_sock != null && isConnected) {
                _sock.send("{\"event\":\"heartbit\",\"data\":{\"timestamp\":0}}");
            } else {
                LogUtils.printLog(tag,"socket is null " + isConnected);
                if(this.uri != null)
                    this._connect();
            }
            requestHeartBit = true;

            LogUtils.printLog(tag," checkAndSendHeartBit " + failedHeartBit + " requestHeartBit " + requestHeartBit);
        }
        else{
            cntisEnableHearbitCheck=cntisEnableHearbitCheck+1;
            // NEL CASO IN CUI PER QUALCHE STRANO MOTIVO isEnableHearbitCheck fa in flase ma la connessione resta in unso stao spurio non viene più richiamato il failedHeartBit e se ne va in loop
            if (cntisEnableHearbitCheck>=3) {
                isEnableHearbitCheck = true;
                cntisEnableHearbitCheck = 0;
            }
        }
        if (m_handler != null) {
            m_handler.postDelayed(m_handlerTask, failedHeartBit > 0 ? 5000 : 20000);
        }

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogUtils.printLog(tag," onTaskRemoved " + reconnect);
        if(tProcess != null)
        {
            tProcess.interrupt();
            LogUtils.printLog(tag," kill  THREED ");
        }
        updateNotification(false);

        if(reconnect) {
            startMonitorSevice();
        } else{
            LogUtils.printLog(tag,"WEBSOCKETSERVICE stop service ");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.printLog(tag," onDestroy "  + reconnect);
        if(tProcess !=null)
            tProcess.interrupt();

        if(reconnect) {
            startMonitorSevice();
        }
    }

    public void closeSocket() {
        this.reconnect = false;
        LogUtils.printLog(tag," closeSocket ");

        if(_sock != null) {
            _sock.send("{\"event\":\"logout\",\"data\":\"logout\"}");
            _sock.close();
        }
        _sock = null;
        isEnableHearbitCheck = false;
        tProcess.interrupt();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void  stopMonitorSevice(){
        /*Intent servicetIntent = new Intent(mContext, WebsocketService.class);
        stopService(servicetIntent);*/
        stopSelf();
    }

    public void  startMonitorSevice(){
        try {
            LogUtils.printLog(tag, " startMonitorSevice ");
            Intent serviceIntent = new Intent(mContext, WebsocketService.class);
            startService(serviceIntent);
        } catch(Exception e) {
            e.printStackTrace();
            LogUtils.printLog(tag, " startMonitorSevice exception ----> " + e.getMessage());
        }
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

}
