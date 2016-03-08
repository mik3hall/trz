#import <JavaVM/jni.h>
#import "KQNotifyListener.h"
#import "main.h"

@implementation KQNotifyListener

@synthesize listener;

-(KQNotifyListener *)initWithListener:(jobject)listenerObj
{
	if (self = [super init]) {
		bool wasAttached = false;
//		JavaVM *JVM = (JavaVM*)jvm();
		JavaVM *JVM = jvm();
		JNIEnv *env = (JNIEnv*)GetJEnv(JVM,&wasAttached);
		jobject gListener = (*env)->NewGlobalRef(env,listenerObj);		// Make sure this is around when we need it
		[self setListener:gListener];
		(*env)->DeleteLocalRef(env,listenerObj);		// so we can let the local ref go
		if (gListener == NULL) {
			NSLog(@"KQNotifyListener: failed to obtain global reference to listener object");
			return nil;
		}
	}
	
	return self;
}

@end