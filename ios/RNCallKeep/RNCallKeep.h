//
//  RNCallKeep.h
//  RNCallKeep
//
//  Created by Ian Yu-Hsun Lin on 12/22/16.
//  Copyright © 2016 Ian Yu-Hsun Lin. All rights reserved.
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

@end
