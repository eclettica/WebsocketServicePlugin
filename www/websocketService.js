  // Empty constructor
  function WebsocketServicePlugin() {
    //this._isAndroid = device.platform.match(/^android|amazon/i) !== null;
    //this._isAndroid = true;
    
  }
  
  WebsocketServicePlugin.prototype._pluginInitialize = function() {
    console.log('WebsocketServicePlugin _pluginInitialize')
    this._isAndroid = device.platform.match(/^android|amazon/i) !== null;
    console.log('WebsocketServicePlugin _pluginInitialize')
    this._defaults =
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
  }
  
  WebsocketServicePlugin._defaults =
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
    console.log('Configure NotificationService setDefaults ' + JSON.stringify(this._defaults) + " " + JSON.stringify(overrides))
    
    if(!this._defaults) {
      this._defaults =
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
    }
  
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
        console.log('Configure android NotificationService ' + JSON.stringify(defaults))
          cordova.exec(null, null, 'WebsocketServicePlugin', 'configure', [defaults, false]);
      } else {
        console.log('Configure NotificationService ' + JSON.stringify(defaults))
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
  
  WebsocketServicePlugin.prototype.disconnect = function(uri, successCallback, errorCallback) {
    var options = {};
    options.uri = uri;
    cordova.exec(successCallback, errorCallback, 'WebsocketServicePlugin', 'disconnect', [options]);
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
  
  WebsocketServicePlugin.prototype.on = function(event, data) {
    console.log('WebsocketServicePlugin on ', event, data);
  }
  
  WebsocketServicePlugin.prototype.fireEvent = function(event, data) {
    console.log('WebsocketServicePlugin on ', event, data);
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
  