#import <JavaNativeFoundation/JavaNativeFoundation.h>
#import "MacFileWatchers.h"
#import "KQueueWatcher.h"
#include <sys/event.h>

static JavaVM *JVM = NULL;

// Simple JNI_OnLoad api
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JVM = vm;
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    printf("JNI_OnUnload\n");
}

/**
 * register invocation for fsevents
 **/
JNIEXPORT void JNICALL Java_us_hall_trz_osx_MacWatchUtils_fsregister(JNIEnv *env, jclass clazz, jstring jfilePath) {
}

/**
 * register invocation for kqueue
 **/
JNIEXPORT void JNICALL Java_us_hall_trz_osx_MacWatchUtils_kqregister(JNIEnv *env, jclass clazz, jobject watchKey,jstring jfilePath,jint jevents) {
	u_int notifications = 0;
	BOOL isDir;
    JNF_COCOA_ENTER(env);
	NSString * path;
	path = [JNFNormalizedNSStringForPath(env,jfilePath) retain];	// Needs to be retained for use in kevent udata field
	if (!([[NSFileManager  defaultManager] fileExistsAtPath:path isDirectory:&isDir] && isDir)) {
		[path release];
		char tempstr[1024];
		const char* filePath;
		filePath = JNFGetStringUTF8Chars(env, jfilePath);
	    sprintf(tempstr,"kqregister: Invalid file: %s doesn't exist or is not a directory",filePath);
		throwIOException(env,tempstr);
	}
	jobject globalWatchKey = JNFNewGlobalRef(env,watchKey);
	// Facilitate directory to NSURL by ensuring ends with a slash
	if (![path hasSuffix:@"/"])
		path = [path stringByAppendingString:@"/"];
    if ((jevents & FILE_CREATED) != 0) 
    	notifications |= NOTE_WRITE;
    if ((jevents & FILE_DELETED) != 0)
    	notifications |= NOTE_DELETE;
    if ((jevents & FILE_MODIFIED) != 0)
    	notifications |= (NOTE_WRITE + NOTE_ATTRIB + NOTE_EXTEND);
	[[KQueueWatcher sharedQueue] addPathToQueue:path notifyingAbout:notifications withWatchKey:globalWatchKey];
	JNF_COCOA_EXIT(env);
}
	
JNIEXPORT void JNICALL Java_us_hall_trz_osx_MacWatchUtils_kqcancel(JNIEnv *env, jclass clazz, jobject watchKey) {
	[[KQueueWatcher sharedQueue] cancel:watchKey];
}

// Throw an IOException with the given message.
static void throwIOException(JNIEnv *env, const char *msg)
{
    static jclass exceptionClass = NULL;
    jclass c;

    if (exceptionClass) {
        c = exceptionClass;
    } else {
        c = (*env)->FindClass(env, "java/io/IOException");
        if ((*env)->ExceptionOccurred(env)) return;
        exceptionClass = (*env)->NewGlobalRef(env, c);
    }

    (*env)->ThrowNew(env, c, msg);
}

JavaVM *jvm() { 
	return JVM;
}

JNIEnv *GetJEnv(JavaVM *vm,bool *wasAttached){
	JNIEnv *env = NULL;
	if(vm == NULL) return env;
	*wasAttached = false;
	
	jint errGetEnv = (*vm)->GetEnv(vm,(void **)&env, JNI_VERSION_1_4);
	if(errGetEnv == JNI_ERR) return NULL;
	if(errGetEnv == JNI_EDETACHED){
		jint rc = (*vm)->AttachCurrentThreadAsDaemon(vm,(void **)&env,(void *)NULL);
		if (rc != 0)
			fprintf(stderr,"MacFileWatchers: attach got %i\n",(int)rc);
		if (env != (void*)NULL) 
			*wasAttached = true;
	}else if(errGetEnv != JNI_OK) return NULL;
	return env;
}
