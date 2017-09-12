//
//  KiiPush.m
//

#import "KiiPush.h"

@implementation KiiPush

- (void)unregister:(CDVInvokedUrlCommand*)command;
{
    [[UIApplication sharedApplication] unregisterForRemoteNotifications];
    [self successWithMessage:@"unregistered" callbackId:command.callbackId];
}


- (void)registerPushiOS10
{
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    [center requestAuthorizationWithOptions:(UNAuthorizationOptionBadge +
                                             UNAuthorizationOptionAlert +
                                             UNAuthorizationOptionSound)
                          completionHandler:^(BOOL granted, NSError * _Nullable error) {
                              if (!granted || error != nil) {
                                  [self failWithMessage:@"Authorization failed."
                                              withError:error
                                             callbackId:self.registerCallback];
                              } else {
                                  [[UIApplication sharedApplication]registerForRemoteNotifications];
                              }
                          }];
}

- (void)registerPushiOS8
{
    UIApplication *application = [UIApplication sharedApplication];
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    UIUserNotificationSettings* notificationSettings =
    [UIUserNotificationSettings
     settingsForTypes: UIUserNotificationTypeBadge|UIUserNotificationTypeSound|UIUserNotificationTypeAlert
     categories:nil];
    [application registerUserNotificationSettings:notificationSettings];
    [application registerForRemoteNotifications];
#pragma clang diagnostic pop
}

- (void)registerPushiOS7
{
    UIApplication *application = [UIApplication sharedApplication];
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    [application registerForRemoteNotificationTypes:(UIRemoteNotificationTypeBadge |
                                                     UIRemoteNotificationTypeSound |
                                                     UIRemoteNotificationTypeAlert)];
#pragma clang diagnostic pop
}

- (void)register:(CDVInvokedUrlCommand*)command;
{

    NSMutableDictionary* options = [command.arguments objectAtIndex:0];
    self.appId = options[@"app_id"];
    self.appKey = options[@"app_key"];
    self.accessToken = options[@"token"];
    self.baseUrl = options[@"baseURL"];
    self.development = false;
    NSNumber* d = options[@"development"];
    if (d != nil) {
        self.development = [d boolValue];
    }

    self.receivedCallback = [options objectForKey:@"ecb"];
    self.isInline = NO;
    self.registerCallback = command.callbackId;

    if ( floor(NSFoundationVersionNumber) > NSFoundationVersionNumber_iOS_9_x_Max)
    {
        // iOS10 or later
        [self registerPushiOS10];
    } else if (NSFoundationVersionNumber > NSFoundationVersionNumber_iOS_7_1){
        // iOS 8, 9
        [self registerPushiOS8];
    } else {
        [self registerPushiOS7];
    }

    if (self.notificationMessage)			// if there is a pending startup notification
        [self notificationReceived];	// go ahead and process it
}

- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    NSString *token = [[[[deviceToken description] stringByReplacingOccurrencesOfString:@"<"withString:@""]
                        stringByReplacingOccurrencesOfString:@">" withString:@""]
                       stringByReplacingOccurrencesOfString: @" " withString: @""];
    [self installDevice:[NSString stringWithFormat:@"%@", token]
            development:self.development];
}

- (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    [self failWithMessage:@"" withError:error callbackId:self.registerCallback];
}

- (void)notificationReceived {
    if (self.notificationMessage && self.receivedCallback)
    {
        NSMutableString *jsonStr = [NSMutableString stringWithString:@"{"];

        [self parseDictionary:self.notificationMessage intoJSON:jsonStr];

        if (self.isInline)
        {
            [jsonStr appendFormat:@"foreground:\"%d\"", 1];
            self.isInline = NO;
        }
        else
            [jsonStr appendFormat:@"foreground:\"%d\"", 0];

        [jsonStr appendString:@"}"];

        NSString * jsCallBack = [NSString stringWithFormat:@"%@(%@);", self.receivedCallback, jsonStr];
        [(UIWebView*)self.webView stringByEvaluatingJavaScriptFromString:jsCallBack];

        self.notificationMessage = nil;
    }
}

// reentrant method to drill down and surface all sub-dictionaries' key/value pairs into the top level json
-(void)parseDictionary:(NSDictionary *)inDictionary intoJSON:(NSMutableString *)jsonString
{
    NSArray         *keys = [inDictionary allKeys];
    NSString        *key;

    for (key in keys)
    {
        id thisObject = [inDictionary objectForKey:key];

        if ([thisObject isKindOfClass:[NSDictionary class]])
            [self parseDictionary:thisObject intoJSON:jsonString];
        else if ([thisObject isKindOfClass:[NSString class]])
            [jsonString appendFormat:@"\"%@\":\"%@\",",
             key,
             [[[[inDictionary objectForKey:key]
                stringByReplacingOccurrencesOfString:@"\\" withString:@"\\\\"]
               stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""]
              stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"]];
        else {
            [jsonString appendFormat:@"\"%@\":\"%@\",", key, [inDictionary objectForKey:key]];
        }
    }
}

-(void)successWithMessage:(NSString *)message callbackId:(NSString*)callbackId
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackId];
}

-(void)failWithMessage:(NSString *)message withError:(NSError *)error callbackId:(NSString*)callbackId
{
    NSString        *errorMessage = (error) ? [NSString stringWithFormat:@"%@ - %@", message, [error localizedDescription]] : message;
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];

    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackId];
}

#pragma mark - private

- (void)installDevice:(NSString*)pushToken development:(BOOL)development {
    NSURLSession *session = [NSURLSession sharedSession];

    NSURL *url = [NSURL URLWithString:[NSString stringWithFormat:@"%@/apps/%@/installations", self.baseUrl, self.appId]];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    [request setHTTPMethod:@"POST"];
    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    params[@"installationRegistrationID"] = pushToken;
    params[@"deviceType"] = @"IOS";
    params[@"development"] = [NSNumber numberWithBool:self.development];

    if (![NSJSONSerialization isValidJSONObject:params]) {
        return;
    }
    NSError* err;
    NSData* data = [NSJSONSerialization dataWithJSONObject:params options:NSJSONWritingPrettyPrinted error:&err];
    if (err != nil) {
        return;
    }
    [request setHTTPBody:data];
    [request addValue:self.appId forHTTPHeaderField:@"x-kii-appid"];
    [request addValue:self.appKey forHTTPHeaderField:@"x-kii-appkey"];
    [request addValue:[NSString stringWithFormat:@"bearer %@", self.accessToken] forHTTPHeaderField:@"authorization"];
    [request addValue:@"application/vnd.kii.InstallationCreationRequest+json" forHTTPHeaderField:@"content-type"];


    NSURLSessionTask *task = [session dataTaskWithRequest:request
                                        completionHandler:
                              ^(NSData *data, NSURLResponse *response, NSError *error) {
                                  if (error) {
                                      NSLog(@"Error code %ld", (long)error.code);
                                      if (self.registerCallback) {
                                          [self failWithMessage:@"Failed to install device token."
                                                      withError:error
                                                     callbackId:self.registerCallback];
                                      }
                                      return;
                                  }
                                  if (self.registerCallback) {
                                      [self successWithMessage:pushToken
                                                    callbackId:self.registerCallback];
                                  }
                              }
                              ];
    [task resume];
}

@end
