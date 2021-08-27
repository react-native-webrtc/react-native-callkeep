//
//  RNCallKeep_Swift.m
//  RNCallKeep
//
//  Created by N L on 27.8.2021.
//  Copyright © 2021 react-native-webrtc. All rights reserved.
//

#import <React/RCTBridgeModule.h>
@interface RCT_EXTERN_MODULE(EYRCallKeep, NSObject)
RCT_EXTERN_METHOD(setMutedCall:(NSString *)uuidString :(BOOL)muted)
RCT_EXTERN_METHOD(endCall:(NSString*)uuidString)
RCT_EXTERN_METHOD(reportEndCall:(NSString*)uuidString :(int)reason)
RCT_EXTERN_METHOD(getAudioRoutes:(RCTPromiseResolveBlock)resolve :(RCTPromiseRejectBlock)reject)
@end
