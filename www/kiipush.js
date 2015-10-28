var KiiPush = function() { };

KiiPush.prototype.register = function(kii, options) {
    var successCallback = options.success;
    var errorCallback = options.failure;
    var receivedCallbackName = options.received;

    options['app_id'] = kii.Kii.getAppID();
    options['app_key'] = kii.Kii.getAppKey();
    options['token'] = kii.KiiUser.getCurrentUser().getAccessToken();
    options['baseURL'] = kii.Kii.getBaseURL();
    options['ecb'] = receivedCallbackName;
    
    cordova.exec(successCallback, errorCallback,
                 'KiiPush', 'register',
                     [options]);
};

KiiPush.prototype.initAndroid = function(senderId, callbackName, options) {
    var successCallback = options.success;
    var errorCallback = options.failure;
    
    options['sender_id'] = senderId;
    options['ecb'] = callbackName;
    cordova.exec(successCallback, errorCallback,
                 'KiiPush', 'init',
                     [options]);
};

if (typeof module != 'undefined' && module.exports) {
  module.exports = new KiiPush();
}
