#import <Foundation/Foundation.h>
#include <sys/event.h>
#import <JavaVM/jni.h>
#import "KQueueWatchKey.h"

// -----------------------------------------------------------------------------
//  Constants:
// -----------------------------------------------------------------------------
 
// Flags for notifyingAbout:
#define UKKQueueNotifyAboutRename					NOTE_RENAME		// Item was renamed.
#define UKKQueueNotifyAboutWrite					NOTE_WRITE		// Item contents changed (also folder contents changed).
#define UKKQueueNotifyAboutDelete					NOTE_DELETE		// item was removed.
#define UKKQueueNotifyAboutAttributeChange			NOTE_ATTRIB		// Item attributes changed.
#define UKKQueueNotifyAboutSizeIncrease				NOTE_EXTEND		// Item size increased.
#define UKKQueueNotifyAboutLinkCountChanged			NOTE_LINK		// Item's link count changed.
#define UKKQueueNotifyAboutAccessRevocation			NOTE_REVOKE		// Access to item was revoked.

#define UKKQueueFileRenamedNotification				UKFileWatcherRenameNotification
#define UKKQueueFileWrittenToNotification			UKFileWatcherWriteNotification
#define UKKQueueFileDeletedNotification				UKFileWatcherDeleteNotification
#define UKKQueueFileAttributesChangedNotification   UKFileWatcherAttributeChangeNotification
#define UKKQueueFileSizeIncreasedNotification		UKFileWatcherSizeIncreaseNotification
#define UKKQueueFileLinkCountChangedNotification	UKFileWatcherLinkCountChangeNotification
#define UKKQueueFileAccessRevocationNotification	UKFileWatcherAccessRevocationNotification

typedef union _USER_DATA {
    struct {
        u_int32_t key;				// Allows us to determine correct KQueueWatchKey
        u_int32_t fd;				// Allows us to determine event path from KQueueWatchKey
    };
    u_int64_t udata;
} USER_DATA;

@interface KQueueWatcher : NSObject
{
	int				queueFD;			// The actual queue ID (Unix file descriptor).
	NSMutableDictionary* watchKeys;      // java WatchKey hashCode keyed dictionary of KQueueWatchKey objc objects
	BOOL			keepThreadRunning;	// Termination criterion of our thread.
}

//-(void) addPathToQueue: (NSString*)path notifyingAbout: (u_int)fflags;
-(void) addPathToQueue: (NSString*)path notifyingAbout: (u_int)fflags withWatchKey:(jobject)watchKey;
//-(void) addPath: (NSString*)path;
+(KQueueWatcher*)    sharedQueue;
-(void) postNotification: (NSString*)nm forFile: (NSString*)fp;
-(void) addChildren: (NSURL*)pathURL notifyingAbout: (u_int)fflags withWatchKey:(KQueueWatchKey*)watchKey;
-(void) kqueue: (NSString*)path notifyingAbout: (u_int)fflags withWatchKey:(KQueueWatchKey*)watchKey;
-(void) createdInDir:(NSString *)dir notifyingAbout:(u_int)fflags withWatchKey:(KQueueWatchKey*)watchKey;
-(void) postJava:(NSString*)paths about:(int)event withWatchKey:(KQueueWatchKey*)watchKey;
-(void) cancel: (jobject)jwatchKey;
@end