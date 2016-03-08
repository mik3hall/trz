#import <JavaVM/jni.h>
#import <Cocoa/Cocoa.h>

@interface KQNotifyListener : NSObject {

	jobject listener;
}

@property jobject listener;

-(KQNotifyListener *)initWithListener:(jobject)listener;

@end