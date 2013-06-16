#import <AppKit/AppKit.h>
#import "KQueueWatcher.h"
#import <JavaVM/jni.h>
#import <JavaNativeFoundation/JavaNativeFoundation.h>
#import "MacFileWatchers.h"
// -----------------------------------------------------------------------------
//  Globals:
// -----------------------------------------------------------------------------

static KQueueWatcher * gKQueueSharedQueueSingleton = nil;
static int count = 0;
static int fires = 0;

static JNF_CLASS_CACHE(jc_KQueueWatchKey,"us/hall/trz/osx/ws/impl/KQueueWatchService$KQueueWatchKey");
static JNF_MEMBER_CACHE(jm_postNativeEvent, jc_KQueueWatchKey, "postNativeEvent","(Ljava/lang/String;I)V");
static JNF_MEMBER_CACHE(jm_cancel, jc_KQueueWatchKey, "cancel","()V");
static JNF_MEMBER_CACHE(jm_hashCode, jc_KQueueWatchKey, "hashCode","()I");	

// From UKFileWatcher.m
static NSString* UKFileWatcherRenameNotification				= @"UKKQueueFileRenamedNotification";
static NSString* UKFileWatcherWriteNotification					= @"UKKQueueFileWrittenToNotification";
static NSString* UKFileWatcherDeleteNotification				= @"UKKQueueFileDeletedNotification";
static NSString* UKFileWatcherAttributeChangeNotification		= @"UKKQueueFileAttributesChangedNotification";
static NSString* UKFileWatcherSizeIncreaseNotification			= @"UKKQueueFileSizeIncreasedNotification";
static NSString* UKFileWatcherLinkCountChangeNotification		= @"UKKQueueFileLinkCountChangedNotification";
static NSString* UKFileWatcherAccessRevocationNotification		= @"UKKQueueFileAccessRevocationNotification";

@interface KQueueWatcher (hidden)
-(NSNumber *) keyForJavaObject:(jobject)object;
@end

@implementation KQueueWatcher (hidden)

-(NSNumber *) keyForJavaObject:(jobject)object
{
	bool wasAttached = false;
	jint key;
	JavaVM *JVM = (JavaVM*)jvm();
	JNIEnv *env = (JNIEnv*)GetJEnv(JVM,&wasAttached);
	JNF_COCOA_ENTER(env);
	key = JNFCallIntMethod(env, object, jm_hashCode);
	JNF_COCOA_EXIT(env);
	return [[NSNumber numberWithInt:key] retain];
}

@end

@implementation KQueueWatcher

// -----------------------------------------------------------------------------
//  sharedQueue:
//		Returns a singleton queue object. In many apps (especially those that
//      subscribe to the notifications) there will only be one kqueue instance,
//      and in that case you can use this.
//
//      For all other cases, feel free to create additional instances to use
//      independently.
//
//	REVISIONS:
//      2005-07-02  UK  Created.
// -----------------------------------------------------------------------------

+(KQueueWatcher*) sharedQueue
{
    @synchronized( self )
    {
        if( !gKQueueSharedQueueSingleton )
            gKQueueSharedQueueSingleton = [[KQueueWatcher alloc] init];	// This is a singleton, and thus an intentional "leak".
    }
    
    return gKQueueSharedQueueSingleton;
}

// -----------------------------------------------------------------------------
//	* CONSTRUCTOR:
//		Creates a new KQueue and starts that thread we use for our
//		notifications.
//
//	REVISIONS:
//
//      2004-11-12  UK  Doesn't pass self as parameter to watcherThread anymore,
//                      because detachNewThreadSelector retains target and args,
//                      which would cause us to never be released.
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------
-(id)   init
{
	self = [super init];
	if(!self) return nil;
	watchKeys = [[NSMutableDictionary alloc] init];
	queueFD = kqueue();
	if( queueFD == -1 )
	{
		[self release];
		return nil;
	}
		
	// Start new thread that fetches and processes our events:
	keepThreadRunning = YES;
	[NSThread detachNewThreadSelector:@selector(watcherThread:) toTarget:self withObject:nil];
	
	return self;
}

// -----------------------------------------------------------------------------
//	release:
//		Since NSThread retains its target, we need this method to terminate the
//      thread when we reach a retain-count of two. The thread is terminated by
//      setting keepThreadRunning to NO.
//
//	REVISIONS:
//		2004-11-12	UK	Created.
// -----------------------------------------------------------------------------

-(oneway void) release
{
    @synchronized(self)
    {
        //NSLog(@"%@ (%d)", self, [self retainCount]);
        if( [self retainCount] == 2 && keepThreadRunning )
            keepThreadRunning = NO;
    }
    
    [super release];
    NSLog(@"keepThreadRunning: %@",keepThreadRunning ? @"YES" : @"NO");
}

// -----------------------------------------------------------------------------
//	* DESTRUCTOR:
//		Releases the kqueue again.
//
//	REVISIONS:
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------

-(void) dealloc
{
	
	if( keepThreadRunning )
		keepThreadRunning = NO;
		
	[watchKeys release];
	watchKeys = nil;
	
	[super dealloc];
    
    //NSLog(@"kqueue released.");
}


// -----------------------------------------------------------------------------
//	queueFD:
//		Returns a Unix file descriptor for the KQueue this uses. The descriptor
//		is owned by this object. Do not close it!
//
//	REVISIONS:
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------

-(int)  queueFD
{
	return queueFD;
}


// -----------------------------------------------------------------------------
//	addPathToQueue:
//		Tell this queue to listen for all interesting notifications sent for
//		the object at the specified path. If you want more control, use the
//		addPathToQueue:notifyingAbout: variant instead.
//
//	REVISIONS:
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------
/*
-(void) addPathToQueue: (NSString*)path
{
	[self addPathToQueue: path notifyingAbout: UKKQueueNotifyAboutRename
												| UKKQueueNotifyAboutWrite
												| UKKQueueNotifyAboutDelete
												| UKKQueueNotifyAboutSizeIncrease	
												| UKKQueueNotifyAboutAttributeChange];
}

-(void) addPath: (NSString*)path
{
	[self addPathToQueue: path notifyingAbout: UKKQueueNotifyAboutRename
												| UKKQueueNotifyAboutWrite
												| UKKQueueNotifyAboutDelete
												| UKKQueueNotifyAboutSizeIncrease	
												| UKKQueueNotifyAboutAttributeChange];
}
*/
// -----------------------------------------------------------------------------
//	addPathToQueue:notfyingAbout:
//		Tell this queue to listen for the specified notifications sent for
//		the object at the specified path.
//
//	REVISIONS:
//      2005-06-29  UK  Files are now opened using O_EVTONLY instead of O_RDONLY
//                      which allows ejecting or deleting watched files/folders.
//                      Thanks to Phil Hargett for finding this flag in the docs.
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------

-(void) addPathToQueue: (NSString*)path notifyingAbout: (u_int)fflags withWatchKey:(jobject)jwatchKey
{
	@synchronized(self) {
		KQueueWatchKey * watchKey;
		watchKey = [watchKeys objectForKey:[self keyForJavaObject:jwatchKey]];
		if (watchKey == nil) {
			watchKey = [[KQueueWatchKey alloc] init];
			[watchKeys setObject:watchKey forKey:[self keyForJavaObject:jwatchKey]];
		}
		else {
			// Clean up any prior monitoring on this re-register
			[watchKey removeAllPathsFromQueue];
		}
		[watchKey setFilterFlags:fflags];
		[watchKey setJavaWatchKey:jwatchKey];	
		[self kqueue:path notifyingAbout:fflags withWatchKey:watchKey];
	
    	[self addChildren:[NSURL fileURLWithPath:path] notifyingAbout:fflags withWatchKey:watchKey];
	}
}

-(void) addChildren: (NSURL*)directoryURL notifyingAbout: (u_int)fflags withWatchKey: (KQueueWatchKey*)watchKey
{
	NSFileManager *fileManager = [[[NSFileManager alloc] init] autorelease];
	NSArray *keys = [[NSArray alloc] initWithObjects:NSURLIsDirectoryKey,NSURLIsHiddenKey,nil];
	
	NSDirectoryEnumerator *enumerator = [fileManager
		enumeratorAtURL:directoryURL
		includingPropertiesForKeys:keys
		options:0
		errorHandler:^(NSURL *url, NSError *error) {
			// Handle the error.
			// Return YES if the enumeration should continue after the error.
			NSLog(@"addChildren error in enumeratorAtURL");
			return YES;
	}];
	
	for (NSURL *url in enumerator) { 
		NSError *error;
		NSString * path = [[url path] retain];
		NSNumber *isDirectory = nil;
		if (! [url getResourceValue:&isDirectory forKey:NSURLIsDirectoryKey error:&error]) {
			// handle error
			NSLog(@"addChildren error in enumerator");
		}
		else if (! [isDirectory boolValue]) {
			// No error and it’s not a directory; do something with the file
			[self kqueue:path notifyingAbout:fflags withWatchKey:watchKey];
		}
		else {
			// Add directory to kqueue
			if (![path hasSuffix:@"/"])
				path = [path stringByAppendingString:@"/"];
			[self kqueue:path notifyingAbout:fflags withWatchKey:watchKey];
			// and recurse it's children
			[self addChildren:url notifyingAbout:fflags withWatchKey:watchKey];
		}
	}
}

-(void) kqueue: (NSString*)path notifyingAbout: (u_int)fflags withWatchKey:(KQueueWatchKey*)watchKey
{
	NSString * resolvedPath = [path stringByResolvingSymlinksInPath];
	if ([path hasSuffix:@"/"] && ![resolvedPath hasSuffix:@"/"])
		resolvedPath = [resolvedPath stringByAppendingString:@"/"];	
/*	
	We should be all right with a duplication as long as fflags are unique?
	TODO Even if some filter overlap we should be able to search 
	all WatchKeys with an interest in the notification and post each?

	if ([watchKey containsPath:resolvedPath]) {
		NSLog(@"KQueueWatcher WARNING attempting to duplicate %@ in kqueue",resolvedPath);
		return;
	}
*/
	[resolvedPath retain];
//	NSLog(@"KQueueWatcher notifying about %@",resolvedPath);
	struct timespec		nullts = { 0, 0 };
	USER_DATA udata;
	
	struct kevent64_s   ev;

	int					fd = open( [resolvedPath fileSystemRepresentation], O_EVTONLY, 0 );
    if( fd >= 0 )
    {
		udata.key = [[self keyForJavaObject:[watchKey javaWatchKey]] intValue];
		udata.fd = fd;
        EV_SET64(&ev, fd, EVFILT_VNODE, 
        		EV_ADD | EV_CLEAR | EV_ENABLE, 
        		[watchKey filterFlags], 0, udata.udata, 
        		0, 0);
        @synchronized( self )
        {
        	[watchKey addPath:resolvedPath withFD:[NSNumber numberWithInt: fd]];
			int rc = kevent64( queueFD, &ev, 1, NULL, 0, 0, &nullts );
			if (ev.flags == EV_ERROR)
				NSLog(@"kevent for %@ resolved %@ error %i",path,resolvedPath,ev.data);
			else if (rc == -1)
				NSLog(@"kevent for %@ resolved %@ error %i",path,resolvedPath,errno); 
        }
    }
}

// -----------------------------------------------------------------------------
//	watcherThread:
//		This method is called by our NSThread to loop and poll for any file
//		changes that our kqueue wants to tell us about. This sends separate
//		notifications for the different kinds of changes that can happen.
//		All messages are sent via the postNotification:forFile: main bottleneck.
//
//		This also calls sharedWorkspace's noteFileSystemChanged.
//
//      To terminate this method (and its thread), set keepThreadRunning to NO.
//
//	REVISIONS:
//		2005-08-27	UK	Changed to use keepThreadRunning instead of kqueueFD
//						being -1 as termination criterion, and to close the
//						queue in this thread so the main thread isn't blocked.
//		2004-11-12	UK	Fixed docs to include termination criterion, added
//                      timeout to make sure the bugger gets disposed.
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------

-(void)		watcherThread: (id)sender
{
	int					n;
	int 				idle_cnt = 0;
	
	struct kevent64_s   ev;
    struct timespec     timeout = { 5, 0 }; // 5 seconds timeout.
	int					theFD = queueFD;	// So we don't have to risk accessing iVars when the thread is terminated.
	
    while( keepThreadRunning )
    {
		NSAutoreleasePool*  pool = [[NSAutoreleasePool alloc] init];
		NS_DURING
			n = kevent64( queueFD, NULL, 0, &ev, 1, 0, &timeout );
			count += n;
//		    NSLog(@"n %i keepThreadRunning: %@",count,keepThreadRunning ? @"YES" : @"NO");
			if (!keepThreadRunning) continue;		// If shutdown in meantime exit now
			if( n > 0)
			{
				if( ev.filter == EVFILT_VNODE )
				{
					if( ev.fflags )
					{
						USER_DATA udata;
						udata = (USER_DATA)ev.udata;
						KQueueWatchKey * watchKey = [watchKeys objectForKey:[NSNumber numberWithInt:(u_int32_t)udata.key]];
						NSNumber * fd = [NSNumber numberWithInt:udata.fd];
						NSString*       context = [watchKey contextForFD:fd];
						NSString*       fpath = [watchKey pathForFD:fd];
						[[NSWorkspace sharedWorkspace] noteFileSystemChanged: fpath];
/*					
						if( (ev.fflags & NOTE_RENAME) == NOTE_RENAME ) {
							[paths addObject:fpath];
							[self postJava:paths about:FILE_RENAMED withWatchKey:watchKey];
//							[self postNotification: UKKQueueFileRenamedNotification forFile: fpath];
						}
*/
						if( (ev.fflags & NOTE_WRITE) == NOTE_WRITE ) {
							if ([fpath hasSuffix:@"/"]) {		// write to directory means file created
								[self createdInDir:fpath notifyingAbout:ev.fflags withWatchKey:watchKey];
							}
							else {
								if ([watchKey accepts:NOTE_WRITE])
									[self postJava:context about:FILE_MODIFIED withWatchKey:watchKey];
							}
//							else [self postNotification: UKKQueueFileWrittenToNotification forFile: fpath];
//							[self postNotification: UKKQueueFileWrittenToNotification forFile: fpath];
						}
						if( (ev.fflags & NOTE_DELETE) == NOTE_DELETE ) {
//							if ([fpath length] > [[watchedPaths objectAtIndex:0] length]) {
								if ([watchKey accepts:NOTE_DELETE])
									[self postJava:context about:FILE_DELETED withWatchKey:watchKey];
								// kqueue will remove the entry so we should clean up our references to match
								if ([watchKey isRoot:fd]) {
									[watchKey removeAllPathsFromQueue];
/*
									bool wasAttached = false;
									JavaVM *JVM = (JavaVM*)jvm();
									JNIEnv *env = (JNIEnv*)GetJEnv(JVM,&wasAttached);
									JNF_COCOA_ENTER(env);
									JNFCallVoidMethod(env,[watchKey javaWatchKey], jm_cancel);
									JNF_COCOA_EXIT(env);
*/
									[watchKeys removeObjectForKey:watchKey];
									[watchKey release];
									watchKey = nil;

								}
								else [watchKey removeFileForPath:fpath];
// TODO this should also get any watched sub-dirs/files
/*
								NSUInteger idx = [watchedPaths indexOfObject:fpath];
								[watchedPaths removeObjectAtIndex:idx];
								int deletedFD = [[watchedFDs objectAtIndex:idx] intValue];
								int rc = close(deletedFD);
								if (rc == -1)
						            NSLog(@"KQueueWatcher: Couldn't close deleted file descriptor (%d)", errno);
								[watchedFDs removeObjectAtIndex:idx];
*/
//							}
//							[self postNotification: UKKQueueFileDeletedNotification forFile: fpath];
						}
						if( (ev.fflags & NOTE_ATTRIB) == NOTE_ATTRIB ) {
							if ([watchKey accepts:NOTE_ATTRIB])
								[self postJava:context about:FILE_MODIFIED withWatchKey:watchKey];
//							[self postNotification: UKKQueueFileAttributesChangedNotification forFile: fpath];
						}
						if( (ev.fflags & NOTE_EXTEND) == NOTE_EXTEND ) {
							if ([watchKey accepts:NOTE_EXTEND])
								[self postJava:context about:FILE_MODIFIED withWatchKey:watchKey];
//							[self postNotification: UKKQueueFileSizeIncreasedNotification forFile: fpath];
						}
/*
						if( (ev.fflags & NOTE_LINK) == NOTE_LINK )
							[self postNotification: UKKQueueFileLinkCountChangedNotification forFile: fpath];
						if( (ev.fflags & NOTE_REVOKE) == NOTE_REVOKE )
							[self postNotification: UKKQueueFileAccessRevocationNotification forFile: fpath];
*/
					}
				}
			}
			else if (idle_cnt++ == 2) keepThreadRunning = false;
		NS_HANDLER
			NSLog(@"Error in UKKQueue watcherThread: %@",localException);
		NS_ENDHANDLER
		
		[pool release];
    }
    
	// Close our kqueue's file descriptor:
	if( close( theFD ) == -1 )
		NSLog(@"release: Couldn't close main kqueue (%d)", errno);
	
//    NSLog(@"exiting kqueue watcher thread.");
}

-(void) createdInDir:(NSString *)dir notifyingAbout:(u_int)fflags withWatchKey:(KQueueWatchKey*)watchKey
{
	BOOL isDir;
	BOOL hasExtension;
	NSArray * fileList = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:dir error:nil];
	for (NSString *fileName in fileList) {
		NSString * fullPath = [dir stringByAppendingPathComponent:fileName];
		hasExtension = [fileName rangeOfString:@"."].length > 0;
		if (![watchKey containsPath:fullPath] && 
			!(!hasExtension && [watchKey containsPath:[fullPath stringByAppendingString:@"/"]])) {
			if (!hasExtension && ([[NSFileManager  defaultManager] fileExistsAtPath:fullPath isDirectory:&isDir] && isDir)) {
				// need to append trailing / if actually a created directory
				fullPath = [fullPath stringByAppendingString:@"/"];
			}
			[self kqueue:fullPath notifyingAbout: fflags withWatchKey:watchKey];
			[self postJava:[watchKey contextForPath:fullPath] about:FILE_CREATED withWatchKey:watchKey];
			return;
		}
	}
}

-(void) postJava:(NSString*)context about:(int)event withWatchKey:(KQueueWatchKey*)watchKey
{
	fires += 1;
	NSLog(@"postJava %i",fires);
	bool wasAttached = false;
	JavaVM *JVM = (JavaVM*)jvm();
	JNIEnv *env = (JNIEnv*)GetJEnv(JVM,&wasAttached);
	JNF_COCOA_ENTER(env);
	JNFCallVoidMethod(env, [watchKey javaWatchKey], jm_postNativeEvent,
						JNFNormalizedJavaStringForPath(env,context),(jint)event);
	JNF_COCOA_EXIT(env);
}

-(void) cancel: (jobject)jwatchKey
{
	@synchronized(self) {
		NSLog(@"KQueueWatcher: cancel");
		NSNumber * key = [self keyForJavaObject:jwatchKey];
		KQueueWatchKey * watchKey = [watchKeys objectForKey:key];
		if (!watchKey) {
			NSLog(@"kqclean WARNING clean for unknown WatchKey");
			return;
		}
		[watchKey removeAllPathsFromQueue];
		[watchKeys removeObjectForKey:key];
		[watchKey release];
		watchKey = nil;
	}
}

// -----------------------------------------------------------------------------
//	postNotification:forFile:
//		This is the main bottleneck for posting notifications. If you don't want
//		the notifications to go through NSWorkspace, override this method and
//		send them elsewhere.
//
//	REVISIONS:
//      2004-02-27  UK  Changed this to send new notification, and the old one
//                      only to objects that respond to it. The old category on
//                      NSObject could cause problems with the proxy itself.
//		2004-10-31	UK	Helloween fun: Make this use a mainThreadProxy and
//						allow sending the notification even if we have a
//						delegate.
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------

-(void) postNotification: (NSString*)nm forFile: (NSString*)fp
{
/*
	if( delegateProxy )
    {
        #if UKKQUEUE_BACKWARDS_COMPATIBLE
        if( ![delegateProxy respondsToSelector: @selector(watcher:receivedNotification:forPath:)] )
            [delegateProxy kqueue: self receivedNotification: nm forFile: fp];
        else
        #endif
            [delegateProxy watcher: self receivedNotification: nm forPath: fp];
    }
	
	if( !delegateProxy || alwaysNotify )
		[[[NSWorkspace sharedWorkspace] notificationCenter] postNotificationName: nm object: fp];
*/
	NSLog(@"Notification: %@ (%@)", nm, fp);
}

// -----------------------------------------------------------------------------
//	description:
//		This method can be used to help in debugging. It provides the value
//      used by NSLog & co. when you request to print this object using the
//      %@ format specifier.
//
//	REVISIONS:
//		2004-11-12	UK	Created.
// -----------------------------------------------------------------------------

-(NSString*)	description
{
//	return [NSString stringWithFormat: @"%@ { watchedPaths = %@}", NSStringFromClass([self class]), watchedPaths];
	return [NSString stringWithFormat: @"%@", NSStringFromClass([self class])];
}

@end
