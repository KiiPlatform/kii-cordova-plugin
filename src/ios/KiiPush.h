//
//  KiiPush.h
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <Cordova/CDVPlugin.h>

@interface KiiPush : CDVPlugin

@property (nonatomic, copy) NSString *receivedCallback;
@property (nonatomic, copy) NSString *registerCallback;

@property (nonatomic, strong) NSDictionary *notificationMessage;
@property BOOL                          isInline;

@property (nonatomic, copy) NSString* appId;
@property (nonatomic, copy) NSString* appKey;
@property (nonatomic, copy) NSString* accessToken;
@property (nonatomic, copy) NSString* baseUrl;
@property BOOL development;


- (void)register:(CDVInvokedUrlCommand*)command;

- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
- (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;

- (void)setNotificationMessage:(NSDictionary *)notification;
- (void)notificationReceived;

@end
