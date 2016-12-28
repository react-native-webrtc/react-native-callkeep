//
//  RNCallKit.h
//  RNCallKit
//
//  Created by Ian Yu-Hsun Lin on 12/22/16.
//  Copyright © 2016 Ian Yu-Hsun Lin. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CallKit/CallKit.h>
#import <Intents/Intents.h>
//#import <AVFoundation/AVAudioSession.h>

#import "RCTEventEmitter.h"

@interface RNCallKit : RCTEventEmitter <CXProviderDelegate>

@property (nonatomic, strong) CXCallController *callKitCallController;
@property (nonatomic, strong) CXProvider *callKitProvider;

+ (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options NS_AVAILABLE_IOS(9_0);

+ (BOOL)application:(UIApplication *)application
continueUserActivity:(NSUserActivity *)userActivity
  restorationHandler:(void(^)(NSArray * __nullable restorableObjects))restorationHandler;

@end
