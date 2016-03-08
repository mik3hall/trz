#import <Cocoa/Cocoa.h>
#import <AppKit/AppKit.h>
#import <JavaVM/jni.h>
#import <JavaNativeFoundation/JavaNativeFoundation.h>
#import "JNISig.h"
#import "KQNotifyListener.h"
#import "KQNotifer.h"
#import "main.h"

static KQNotifier *instance = nil;

void addListener(JNIEnv *env,jobject listener) {
	NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init]; 
	NSMutableArray * listeners = [instance listeners];
	if (listeners == nil) {
		[instance setListeners:[NSMutableArray arrayWithCapacity:1]];
		listeners = [instance listeners];
	}
	KQNotifyListener * notifyListener = [[KQNotifyListener alloc] initWithListener:listener];
	if (notifyListener != nil)
		[listeners addObject:notifyListener];
	[pool release];
}

jobject getEvent(JNIEnv *env,jobject map) {
	jclass eclazz = (*env)->FindClass(env,"us/hall/event/KQNotifyEvent");
	if (eclazz == NULL) {
		NSLog(@"getEvent: failed to obtain Notify class");
		return NULL;
	}
	jmethodID cid = (*env)->GetMethodID(env,eclazz,"<init>",JRISigMethod(JRISigClass("java/util/Map")) JRISigVoid);
	jobject jnievent = (*env)->NewObject(env, eclazz, cid, map);
	return jnievent;
}

@implementation KQNotifier

@synthesize listeners;

/*****************************************************
 * init
 *****************************************************
 * Purpose:  This method will setup the KQNotifer to notify
 * the specified methods.  After this method is called and the application
 * loop run you will see notifications on KQueue file events.  
 *
 * Parameters:
 *  KQNotification     A selector which
 * represents a method with one argument:  a pointer to a NSNotification.
 * Before calling init the YourLaunchNotificationMethod parameter is
 * expected to have a selector pointing to a method which the user desires
 * to be called any time a KQueue file change of interest occurs. 
 *
 * Additional Note: For these notifications to work you must have an active
 * connection with the window server.  This can be done by calling
 * [NSApplication sharedApplication] *before* registering for notifications.
 * Also, here we must run the resulting Application event loop to ensure
 *  we get the notifications properly.
 *
 *****************************************************/

-(id)init: (SEL) KQNotification:
{
    NSWorkspace* CurrentAppsSharedWorkspace;
    NSNotificationCenter* WorkspaceNotificationCenter;

    //Calling init on super as always
    self = [super init];

    /* Before setting up the notifications we first must get access to the
     * workspace notification center for this application.
     * This is because all NSWorkspace notifications are posted to
     * NSWorkspace's own notification center, not the application's default
     * notification center.
     */

    /* Method call: To get access to the notification center for our the
     * workspace we must first get access to the shared workspace.  We
     * call the method sharedWorkspace on NSWorkspace to do this.  This
     * call will return the shared workspace for our application.
     */

    CurrentAppsSharedWorkspace = [NSWorkspace sharedWorkspace];

    /* Method Call: Once we have the workspace for our application we can ask
     * for the workspace's notification center.  We do this by calling the
     * noticationCenter method on the shared workspace for our application.
     * This will return the notification center for our the workspace.
     */

    WorkspaceNotificationCenter =
               [CurrentAppsSharedWorkspace notificationCenter];

    /* Here we are registering for the launch notifications.  This is done
     * by registering for the NSWorkspaceDidLaunchApplicationNotification
     * inside our the NSWorkspace notification center.  We do the registration
     * using the method call addObserver on the workspace's notification center.
     * First Argument: The observor who will receive the notifications.  In
     *     this case we want us to receive the notifications so add 'self'
     *     as the observor.
     * Second Argument: The selector for the method which will be called when
     *     a notification arrives.  In this case it is the selector passed into
     *     this method. Note the method being called must have one argument
     *     which is a NSNotification.
     * Third Argument: The name notification we are registering for.
     *     In this case we want launch notifications.
     *     The name of the notification for launch notifications is represented
     *     by the NSWorkspace constant:
     *     NSWorkspaceDidLaunchApplicationNotification
     * Forth Argument: An object the notification must be associated with for
     *     us to be notified.  Here we don't need or want any extra selectors
     *     since we want all launch notifications.  Thus, we pass nil for this
     *     optional argument so that we receive all application
     *     launch notifications.
     */

    [WorkspaceNotificationCenter addObserver:self
    selector:fileSystemChanged
    name:kFSFileChangedNotification object:nil];

    return self; //as always in init we need to return ourself
}

/*****************************************************
 * dealloc
 *****************************************************
 * Purpose:  This method deallocates the LaunchTerminateNotifier object.
 * Also here we will unregister for the notifications.
 *
 * Parameters: No Parameters
 *
 *****************************************************/
- (void)dealloc
{
    // --- Removing the notifications --- //

    /* Before removing the notifications we first must get access to
     * the workspace notification center for this application.
     * This is because the NSWorkspace is who we have to unregister with
     * for the notifications
     */

    /* Method call: To get access to the notification center for our the
     * workspace we must first get access to the shared workspace.  We
     * call the method sharedWorkspace on NSWorkspace to do this.  This
     * call will return the shared workspace for our application.
     */

    NSWorkspace* CurrentAppsSharedWorkspace = [NSWorkspace sharedWorkspace];

    /* Method Call: Once we have the workspace for our application we can ask
     * for the workspace's notification center.  We do this by calling the
     * noticationCenter method on the shared workspace for our application.
     * This will return the notification center for the workspace.
     */

    NSNotificationCenter* WorkspaceNotificationCenter =
               [CurrentAppsSharedWorkspace notificationCenter];

    /* Here we are unregistering for the launch and terminate notifications.
     * This is done by removing ourself as an observor for all notifications.
     * Since we will only be registered for launch and terminate notifications
     * this effectively removes us in a single method call.
     * First Argument: The observor who will remove from the notification
     *     list.  In this case we need to remove ourselves from the notification
     *     list.  Thus unregister 'self' for all notifications.
     */

    [WorkspaceNotificationCenter removeObserver:self];

    [super dealloc]; //as always we need to call dealloc on super when we
                     //are done.

}

/*****************************************************
 * fileSystemChanged
 *****************************************************
 * Purpose:  This is the method which will become active any time
 * a launch occurs on the system.  This is because it
 * was the method specified in the selector when calling
 * init.
 *
 * Parameters:
 *
 *  fileSystemChanged      A Notification
 * sent which is associated with an application launching.  The
 * notification contains information on the process just launched.
 *
 *****************************************************/

-(void) fileSystemChanged: (NSNotification *)KQNotificationReceived
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    /* Looking up process name based upon the dictionary of
     * process information provided.  Currently this is the
     * only information in the list.  However, more is
     * scheduled to be added in the future.
     */
    NSString *LaunchedApplicationProcessName =
        [[LaunchNotificationReceived userInfo]
                        objectForKey: @"NSApplicationName"];
    bool wasAttached = false;
	JavaVM *JVM = (JavaVM*)jvm();
	JNIEnv *env = (JNIEnv*)GetJEnv(JVM,&wasAttached);
    JNF_COCOA_ENTER(env);
    NSDictionary *dict = [LaunchNotificationReceived userInfo];
    JNFTypeCoercer* coercer = [JNFDefaultCoercions defaultCoercer];
    [JNFDefaultCoercions addMapCoercionTo:coercer]; 
    jobject map = [coercer coerceNSObject:dict withEnv:env];   
    jobject event = getEvent(env,map);
    if (event != NULL) {
    	static JNF_CLASS_CACHE(jc_NotifyEvent, "us/hall/event/KQNotifyEvent");
    	static JNF_CTOR_CACHE(jm_NotifyEvent_ctor, jc_NotifyEvent, JRISigClass("java/util/HashMap") JRISigVoid);
		static JNF_CLASS_CACHE(jc_NotifyEventListener, "us/hall/event/KQNotifyEventListener");
	
		static JNF_MEMBER_CACHE(jm_fileSystemChanged, jc_NotifyEventListener, "fileSystemChanged","(Lus/hall/event/KQNotifyEvent;)V" );
    	for (MonitorListener *listener in listeners) {
			JNFCallObjectMethod(env, [listener listener], jm_fileSystemChanged, event);
    	}
    }
    else {
       NSLog(@"LaunchNotificationReceived: failed to get event object for notification");
    }               
    JNF_COCOA_EXIT(env);
    [pool drain];
}

+(KQNotifier *)sharedInstance
{
    @synchronized (self) {
		/* Registering for the notifications using the LaunchTerminateNotifier
		 * method init.  The two parameters to init are selectors for the launch
		 * and termination methods which will be called on notification.
		 * First Argument: A selector representing the method which
		 *    will be called each time an application is launched.
		 * Second Argument: A selector representing the method which
		 *    will be called each time an application is terminated.
		 */
		instance = [[KQNotifier alloc] init:
					@selector(fileSystemChanged:)];

//      if (instance == nil) {
//         [[self allocWithZone:NULL] init];
//      }
    }
    
    return instance;
}

+ (id)allocWithZone:(NSZone *)zone
{
   @synchronized (self) {
      if (instance == nil) {
         instance = [super allocWithZone:NULL];
         return instance;
      }
   }
   return nil;
}

- (id)copyWithZone:(NSZone *)zone
{
	return self;
}

- (id)retain
{
	return self;
}

- (NSUInteger)retainCount
{
	return NSUIntegerMax;		// denotes an object that cannot be released
}

- (void)release
{
	// do nothing
}

- (id)autorelease
{
	return self;
}

@end

/*****************************************************
 * main
 *****************************************************
 * Purpose:  
 *****************************************************/
int main (int argc, const char * argv[])
{
    //need a auto release pool for allocating objects.
    NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];

    /* For the notifications to work properly we need to have an active
     * connection to the window server.  This is done here by calling
     * sharedApplication on the NSApplication.  In a regular Cocoa application
     * (as opposed to a tool) this will likely be done for you.  However,
     * we call sharedApplication and run the resulting NSApplication to
     * ensure we get the notifications properly
     */
    NSApplication* CurrentApplication = [NSApplication sharedApplication];

    /* Registering for the file system change notifications
     * method init.  The two parameters to init are selectors for the 
     * notification method.
     * Argument: A selector representing the method which
     *    will be called when the file system changes
     */
    [[KQ alloc] init:
                @selector(fileSystemChanged:):

    /* Running the current applications main event loop.  This will allow us
     * to receive the launch/terminate notifications
     */
    [CurrentApplication run];

    // No more need for autorelease pool so release
    [pool release];
    return 0;
}
