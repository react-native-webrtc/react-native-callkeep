//
//  RNCallKeep_Swift.m
//  RNCallKeep
//
//  Created by N L on 27.8.2021.
//  Copyright © 2021 react-native-webrtc. All rights reserved.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(EYRCallKeep, RCTEventEmitter)
RCT_EXTERN_METHOD(setMutedCall:(NSString *)uuidString muted:(BOOL))
RCT_EXTERN_METHOD(endCall:(NSString*)uuidString)
RCT_EXTERN_METHOD(answerIncomingCall:(NSString*)uuidString)
RCT_EXTERN_METHOD(reportEndCall:(NSString*)uuidString reason:(int))
RCT_EXTERN_METHOD(fulfillAnswerCallAction)
RCT_EXTERN_METHOD(fulfillEndCallAction)
RCT_EXTERN_METHOD(getInitialEvents:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock))
@end
