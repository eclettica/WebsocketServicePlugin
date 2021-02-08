package it.linup.cordova.plugin.services;

import android.os.AsyncTask;

import it.linup.cordova.plugin.services.WebsocketService;


public class SendOperation extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... params) {
        if(params != null && params.length == 1) {
            WebsocketService.instance().send(params);
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