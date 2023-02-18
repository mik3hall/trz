# TRZ
### An OS/X java nio.2 default FileSystemProvider 

Why TRZ? See below.

This was originally mainly written to demonstrate that with nio.2 custom File attribute views would be possible. Specifically for this code, OS/X file related api's. 

### Native WatchService

####TL;DR (just want to use the watch service)

In order to use this you first need a jdk that includes the jdk.incubator.foreign module. You also need the macnio2.jar in classpath, not modular. Finally you need, the native macattr.dylib in java.library.path. 

Invocation along the lines of...
```
java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED -cp .:macnio2.jar -Djava.library.path=libs -Djava.nio.file.spi.DefaultFileSystemProvider=us.hall.trz.osx.MacFileSystemProvider LotsOfEvents
``` 
This code provides the custom default file system provider that uses the native watch service instead of the default polling one. Mostly it otherwise serves as a passthrough to the normal platform file system provider. The rest is as indicated with an additinal parameter seeming necessary for 'foreign'.

I think this is about the third release with the foreign api in incubator. But if you wanted to use an earlier jdk I think it could be possible. You would need to revert the changes shown below to replace the runtime version use of Unsafe to the foreign api. So back to Unsafe. Then if I understood correctly you would need to include the jdk.unsupported module. I didn't try this. Replacing Unsafe with 'foreign' seemed the best choice going forward.

####This implementation

I am revisiting the code because it may at long last be possible to add a native WatchService. The prior code that I included here had I think at least three aborted attempts of my own to have one. That has been deleted, hopefully making things somewhat less messy. 

Recently there was a pull request on openjdk [8293067: (fs) Implement WatchService using system library (macOS) #10140](https://github.com/openjdk/jdk/pull/10140). That provided just this. It comes from a [JetBrains runtime implementation](https://github.com/JetBrains/JetBrainsRuntime). The openjdk effort is still in process. I based what I have here on the JetBrains. 

Some changes were made since this is not integrated into a runtime. The original was able to access the Unix attributes for the fileKey. I make use of my extended Cocoa attributes. 

So instead of...

```
key = new UnixFileKey(st_dev, st_ino);
```
which is in the internal sun.nio.fs package. I have...

```
return new MacFileKey(fileSystemNumber, fileSystemFileNumber);
```

which uses my provider's Cocoa file attributes...

```
long fileSystemNumber = 
			((Long)Files.getAttribute(p,"mac_cocoa:NSFileSystemNumber")).longValue();
long fileSystemFileNumber = 
			((Long)Files.getAttribute(p,"mac_cocoa:NSFileSystemFileNumber")).longValue();
```

The runtime version was able to make use of Unsafe. 

```
import jdk.internal.misc.Unsafe;
...
final int flags = unsafe.getInt(eventFlagsPtr);
```

I chose to use the new foreign memory api's. 

```
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ValueLayout;
...
MemoryAddress flagsArray = MemoryAddress.ofLong(eventFlagsPtr);
long offset = 0L;
...
final int flags = flagsArray.get(ValueLayout.JAVA_INT,offset);
...
offset += SIZEOF_FS_EVENT_STREAM_EVENT_FLAGS;
```

This means that this can only be used with a jdk version that includes the foreign incubator. The module for that must be included and there is an additional launch parm. 

e.g. 

```
java --add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED -cp .:macnio2.jar -Djava.library.path=libs -Djava.nio.file.spi.DefaultFileSystemProvider=us.hall.trz.osx.MacFileSystemProvider LotsOfEvents
```

is how I launch the one and only test I have done so far. It does run fine at this point and is a modification of the jdk nio LotsOfEvents test that does lots more of the lots.

```
./lotsofevents.sh
WARNING: Using incubator modules: jdk.incubator.foreign
Seed from RandomFactory = -6232501242862233876L
watcher us.hall.trz.osx.MacOSXWatchService@7f53c3bf class us.hall.trz.osx.MacOSXWatchService
watcher us.hall.trz.osx.MacOSXWatchService@154be9df class us.hall.trz.osx.MacOSXWatchService
Polling retrieved 1 event(s)
Polling retrieved 1 event(s)
Polling retrieved 1 event(s)
Polling retrieved 1 event(s)
2 overflow elapsed 32295
1 overflow elapsed 32296
1queuing elapsed 24387
2queuing elapsed 24387
```

I think shows that I am correctly using the native watch service implementation and not the OS/X platform polling one. 

I had to include some of the runtime related like...  
jio.c.  
jni_util_md.c  
jni_util.c. 

and associated headers.
There were linkage errors on extern's that I found implementations for in Graal runtime source that I have.

For references to jvm I set it from a JNI_OnLoad. When I was getting 'platform encoding not initialized' errors, I initialized it to UTF-8 here. That is what I show for sun.jnu.encoding. 

```
// Simple JNI_OnLoad api
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jvm = vm;
    JNIEnv *env = NULL;
    jint errGetEnv = (*vm)->GetEnv(vm,(void **)&env, JNI_VERSION_1_4);
    if(errGetEnv == JNI_OK) {
    	InitializeEncoding(env, "UTF-8");
    }
    return JNI_VERSION_1_4;
}
```

That more or less covers how this implementation might differ from the openjdk or JetBrains runtime ones. It should work on any openjdk version that includes the foreign incubator. You replace the default FileSystemProvider with my OS/X custom one - which is mostly pass through to the platform one, and you should automatically get the native watch service.

If you want to try it out before it may be included in openjdk.

### Extended OS/X FileAttributeView's

As previously mentioned I originally came up with a default FileSystemProvider to see how customizable this could be. Particularly, for OS/X. The provider is mostly pass through to the platform one. However, the following FileAttributeView's have been added.

#### Cocoa

e.g. mac_cocoa:NSFileSize

Attributes: 
 
	NSFileType
	NSFileTypeDirectory
	NSFileTypeRegular
	NSFileTypeSymbolicLink
	NSFileTypeSocket
	NSFileTypeCharacterSpecial
	NSFileTypeBlockSpecial
	NSFileTypeUnknown
	NSFileSize
	NSFileModificationDate
	NSFileReferenceCount
	NSFileDeviceIdentifier
	NSFileOwnerAccountName
	NSFileGroupOwnerAccountName
	NSFilePosixPermissions
	NSFileSystemNumber
	NSFileSystemFileNumber
	NSFileExtensionHidden
	NSFileHFSCreatorCode
	NSFileHFSTypeCode
	NSFileImmutable
	NSFileAppendOnly
	NSFileCreationDate
	NSFileOwnerAccountID
	NSFileGroupOwnerAccountID
	NSFileBusy

#### Launch Services

I always thought the Launch Services api was underrated and potentially useful.

e.g. mac_ls:DEFAULT_APP

Attributes:

```
CREATOR
TYPE
PLAIN	
PACKAGE
APPLICATION
CONTAINER
ALIAS
SYMLINK
INVISIBLE
VOLUME
DEFAULT_APP
KIND
APPLICATIONS
```
#### Extended Attributes
When I added some support for extended attributes they were rather new to me. Since then I have occasionally found the need to do things like delete the com.apple.quarantine attribute. I believe that jdk has itself added some support. I am not really familiar with that either.

Not knowing exactly what it did or was for I chose to make my support read only. With no fixed keys. 

e.g. 

```
Map<String,Object> attrs = Files.readAttributes(p,"mac_xattr:*");
byte[] finfo = (byte[])attrs.get("mac_xattr:com.apple.FinderInfo");
```
I am not familiar with many of the attributes the code makes avaiable. Others should undoubtedly be read only. You don't want the code trying to change file size. 

Given interest I could put in more time to make the code more correct and robust.

#### Finder 

If you like Mac api's classical. These attributes are Carbon based and pre-date OS/X. I've been told creator/type are no longer used for anything. This appeared to be true in some quick testing where setting did not work. However, the api and methods are still around. For creator/type even redundant across api's, see Cocoa. 

e.g. Files.setAttribute(p, "mac_finder:label", "orange");

Color as a file attribute, sort of fun?

Attributes:

```
CREATOR
TYPE
INVISIBLE
NAME_LOCKED
STATIONERY
ALIAS
CUSTOM_ICON
LOCKED
LABEL
```
#### Additional API

**FileManager** 

FileManager is part of the old Apple java api's I salvaged during the macport project. I replaced parts with code from this project. Given the elapsed time some parts may not work or be very useful anymore.

**FileHelper**

Some additional file related methods I thought made useful additions to the eio api. The same caveats related to elapsed time. 

Although I did make these about my first attempt at modular. You would probably need
```
--add-modules us.hall.eio
```
 to make use of them.
   
#### Why TRZ?

TRZ stands for 'transparent random zip'. My first attempt at something like a filesystem allowed you to sort of see through 'transparent'ly a RandomAccessFile 'random' file laid out as a 'zip' file. It worked well enough that I could get javac to compile directly to zip based for a homegrown IDE. It relied on violating ClassLoader search path order and some reflection tricks. I had called it 'smoke and mirrors'. ClassLoader changes at Java 6 completely broke it. When Java 7 nio.2 brought the code of interest here I carried on the name of code I put a lot of time into.




