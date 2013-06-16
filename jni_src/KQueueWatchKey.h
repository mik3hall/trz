#import <Foundation/Foundation.h>
#import <JavaVM/jni.h>

@interface KQueueWatchKey : NSObject
{
	int				queueFD;			// The actual queue ID (Unix file descriptor).
	NSMutableDictionary* watchedFiles;	// monitored files keyed by fd with path values
										// there may be multiple fds with the same path if different filter flags
	jobject			javaWatchKey;		// java KQueueWatchKey
	u_int			filterFlags;		// fflags to use on paths added to kqueue
	NSString*		fpath;				// Path that kqueue is monitoring and notifying about
	NSNumber*		rootFD;				// fd for the root directory entry, used to determine contexts
}

-(u_int) filterFlags;
-(void) setFilterFlags:(u_int)fflags;
-(jobject) javaWatchKey;
-(void) setJavaWatchKey:(jobject)watchKey;
-(BOOL) accepts:(u_int)fflag;
-(BOOL) isRoot:(NSNumber*)fd;
-(BOOL) containsPath:(NSString*)path;
-(void) addPath:(NSString*)path withFD:(NSNumber*)fd;
-(NSString *) pathForFD:(NSNumber*)fd;
-(NSString *) contextForPath:(NSString*)path;
-(NSString *) contextForFD:(NSNumber*)fd;
-(void) removeFileForFD: (NSNumber*)fd;
-(void) removeFileForPath: (NSString*)path;
-(void) removeAllPathsFromQueue;

@end