// Empty constructor
function WebsocketServicePlugin() {
  //this._isAndroid = device.platform.match(/^android|amazon/i) !== null;
  //this._isAndroid = true;
}

WebsocketServicePlugin.prototype._pluginInitialize = function() {
  this._isAndroid = device.platform.match(/^android|amazon/i) !== null;
}

var _defaults =
{
    title:   'WebSocketService is running',
    text:    '.......',
    bigText: false,
    resume:  true,
    silent:  false,
    hidden:  true,
    color:   undefined,
    icon:    'ic_launcher'
};

WebsocketServicePlugin.prototype.setDefaults = function(overrides) {

  var defaults = this._defaults;

    for (var key in defaults)
    {
        if (overrides.hasOwnProperty(key))
        {
            defaults[key] = overrides[key];
        }
    }

    if (this._isAndroid)
    {
        cordova.exec(null, null, 'WebsocketServicePlugin', 'configure', [defaults, false]);
    }
}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'
WebsocketServicePlugin.prototype.show = function(message, duration, successCallback, errorCallback) {
  var options = {};
  options.message = message;
  options.duration = duration;
  cordova.exec(successCallback, errorCallback, 'WebsocketServicePlugin', 'show', [options]);
}

WebsocketServicePlugin.prototype.connect = function(uri, successCallback, errorCallback) {
  var options = {};
  options.uri = uri;
  cordova.exec(successCallback, errorCallback, 'WebsocketServicePlugin', 'connect', [options]);
}

WebsocketServicePlugin.prototype.send = function(msg, successCallback, errorCallback) {
  var options = {};
  options.params = msg;
  cordova.exec(successCallback, errorCallback, 'WebsocketServicePlugin', 'send', [options]);
}

WebsocketServicePlugin.prototype.startBackground = function(successCallback, errorCallback) {
  cordova.plugins.backgroundMode.enable();
}

WebsocketServicePlugin.prototype.checkBackground = function(successCallback, errorCallback) {
  let bool = cordova.plugins.backgroundMode.isActive();
  if(successCallback)
    successCallback(bool);
}

// Installation constructor that binds ToastyPlugin to window
WebsocketServicePlugin.install = function() {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.WebsocketServicePlugin = new WebsocketServicePlugin();
  
  return window.plugins.WebsocketServicePlugin;
};
cordova.addConstructor(WebsocketServicePlugin.install);
