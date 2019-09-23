//
//  RNCallKeep.h
//  RNCallKeep
//
//  Copyright 2016-2019 The CallKeep Authors (see the AUTHORS file)
//  SPDX-License-Identifier: ISC, MIT
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CallKit/CallKit.h>
#import <Intents/Intents.h>
//#import <AVFoundation/AVAudioSession.h>

#import <React/RCTEventEmitter.h>

@interface RNCallKeep : RCTEventEmitter <CXProviderDelegate>

@property (nonatomic, strong) CXCallController *callKeepCallController;
@property (nonatomic, strong) CXProvider *callKeepProvider;

+ (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options NS_AVAILABLE_IOS(9_0);

+ (BOOL)application:(UIApplication *)application
continueUserActivity:(NSUserActivity *)userActivity
  restorationHandler:(void(^)(NSArray * __nullable restorableObjects))restorationHandler;

+ (void)reportNewIncomingCall:(NSString *)uuidString
                       handle:(NSString *)handle
                   handleType:(NSString *)handleType
                     hasVideo:(BOOL)hasVideo
          localizedCallerName:(NSString * _Nullable)localizedCallerName
                  fromPushKit:(BOOL)fromPushKit;
@end
