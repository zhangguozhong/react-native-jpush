#import <UIKit/UIKit.h>
#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#else
#import "RCTBridgeModule.h"
#endif

@interface TMJPushModule : NSObject <RCTBridgeModule>

+ (void)registerWithlaunchOptions:(NSDictionary *)launchOptions appKey:(NSString *)appKey appChannel:(NSString *)appChannel withAppDelegate:(id)delegate;
+ (void)application:(UIApplication *)application didRegisterDeviceToken:(NSData *)deviceToken;
+ (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo;
+ (void)didReceiveRemoteNotificationWhenFirstLaunchApp:(NSDictionary *)launchOptions;
+ (void)setBadgeNumber:(int)badge;

@end
  
