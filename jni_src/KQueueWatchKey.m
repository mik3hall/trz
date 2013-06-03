#import "KQueueWatchKey.h"

@implementation KQueueWatchKey

-(id)   init
{
	self = [super init];
	if(!self) return nil;
	watchedFiles = [[NSMutableDictionary alloc] init];

	return self;
}

-(u_int) filterFlags 
{
	return filterFlags;
}

-(void) setFilterFlags:(u_int)fflags
{
	filterFlags = fflags;
}

-(jobject) javaWatchKey
{
	return javaWatchKey;
}

-(void) setJavaWatchKey:(jobject)watchKey {
	javaWatchKey = watchKey;
}

/**
 * Check to see if we match the given single filter flag setting
 **/
-(BOOL) accepts:(u_int)fflag
{
	return ((filterFlags & fflag) == fflag );
}

-(BOOL)isRoot:(NSNumber*)fd
{
	return [fd isEqualToNumber:rootFD];
}

-(BOOL) containsPath:(NSString*)path
{
	NSEnumerator * paths = [watchedFiles objectEnumerator];
	for (NSString* watchedPath in paths) 
		if ([path isEqualToString:watchedPath]) return TRUE;
	return FALSE; 
}

-(void) addPath:(NSString*)path withFD:(NSNumber*)fd 
{
	if ([watchedFiles count] == 0) 			// first one should be root
		rootFD = fd;
	[watchedFiles setObject:path forKey:fd];
}

-(NSString *) pathForFD:(NSNumber*)fd
{
	return [watchedFiles objectForKey:fd];
}

-(NSString *) contextForPath:(NSString*)path
{
	return [path substringFromIndex:[[watchedFiles objectForKey:rootFD] length]];
}

-(NSString *) contextForFD:(NSNumber*)fd
{
	return [self contextForPath:[watchedFiles objectForKey:fd]];
}

-(void) removeFileForFD: (NSNumber*)fdnum
{
	if ([watchedFiles objectForKey: fdnum] == nil) return;
	
	int fd = [fdnum intValue];
	close(fd);
	[watchedFiles removeObjectForKey:fdnum];
}

-(void) removeFileForPath: (NSString*)path
{
	NSEnumerator * fds = [[watchedFiles allKeysForObject:path] objectEnumerator];
	for (NSNumber * fdnum in fds)
		[self removeFileForFD:fdnum];
}
/*
-(void) removePath: (NSString*)path
{
    [self removePathFromQueue: path];
}
*/

// -----------------------------------------------------------------------------
//	removePathFromQueue:
//		Stop listening for changes to the specified path. This removes all
//		notifications. Use this to balance both addPathToQueue:notfyingAbout:
//		as well as addPathToQueue:.
//
//	REVISIONS:
//		2004-03-13	UK	Documented.
// -----------------------------------------------------------------------------
/*
-(void) removePathFromQueue: (NSString*)path
{
    int		index = 0;
    int		fd = -1;
    
    @synchronized( self )
    {
    	if ([watchedPaths indexOfObject: path] == NSNotFound) return;
  
        fd = [[watchedFDs objectAtIndex: index] intValue];
        
        [watchedFDs removeObjectAtIndex: index];
        [watchedPaths removeObjectAtIndex: index];
    }
	
//	if( close( fd ) == -1 )
//        NSLog(@"removePathFromQueue: Couldn't close file descriptor (%d)", errno);
}
*/

// -----------------------------------------------------------------------------
//	removeAllPathsFromQueue:
//		Stop listening for changes to all paths. This removes all
//		notifications.
//
//  REVISIONS:
//      2004-12-28  UK  Added as suggested by bbum.
// -----------------------------------------------------------------------------

-(void) removeAllPathsFromQueue;
{
    @synchronized( self )
    {
    	NSEnumerator *  fdEnumerator = [[watchedFiles allKeys] objectEnumerator];
        NSNumber     *  anFD;
        
        while( (anFD = [fdEnumerator nextObject]) != nil )
            close( [anFD intValue] );

		[watchedFiles removeAllObjects];
//        [self release];
    }
}

-(void) dealloc
{
	
	// Close all our file descriptors so the files can be deleted:
	if ([watchedFiles count] != 0) {
   		NSEnumerator *  fdEnumerator = [[watchedFiles allKeys] objectEnumerator];
        NSNumber     *  anFD;
        
        while( (anFD = [fdEnumerator nextObject]) != nil )
            close( [anFD intValue] );

		[watchedFiles removeAllObjects];
        [self release];	
	}
			
	[watchedFiles release];
	watchedFiles = nil;
	
	[super dealloc];
    
    //NSLog(@"kqueue released.");
}

@end