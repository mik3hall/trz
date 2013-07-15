/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#import <Cocoa/Cocoa.h>
#import <JavaNativeFoundation/JavaNativeFoundation.h>
#import "MacFileManager.h"


/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    _findFolder
 * Signature: (SIZ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_us_hall_osx_eio_FileManager__1findFolder__SIZ
(JNIEnv *env, jclass clz, jshort domain, jint folderType, jboolean createIfNeeded)
{
    jstring filename = nil;
JNF_COCOA_ENTER(env);

    FSRef foundRef;
    createIfNeeded = createIfNeeded || (folderType == kTemporaryFolderType) || (folderType == kChewableItemsFolderType);
    if (FSFindFolder((SInt16)domain, (OSType)folderType, (Boolean)createIfNeeded, &foundRef) == noErr) {
        char path[PATH_MAX];
        if (FSRefMakePath(&foundRef, (UInt8 *)path, sizeof(path)) == noErr) {
            NSString *filenameString = [[NSFileManager defaultManager] stringWithFileSystemRepresentation:path length:strlen(path)];
            filename = JNFNormalizedJavaStringForPath(env, filenameString);
        }
    }

JNF_COCOA_EXIT(env);
    return filename;
}


/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    _openURL
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_us_hall_osx_eio_FileManager__1openURL
(JNIEnv *env, jclass clz, jstring urlString)
{
JNF_COCOA_ENTER(env);

    NSURL *url = [NSURL URLWithString:JNFNormalizedNSStringForPath(env, urlString)];

        // Radar 3208005: Run this on the main thread; file:// style URLs will hang otherwise.
    [JNFRunLoop performOnMainThreadWaiting:NO withBlock:^(){
        [[NSWorkspace sharedWorkspace] openURL:url];
    }];

JNF_COCOA_EXIT(env);
}


/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    getNativeResourceFromBundle
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_us_hall_osx_eio_FileManager_getNativeResourceFromBundle
(JNIEnv *env, jclass clz, jstring javaResourceName, jstring javaSubDirName, jstring javaTypeName)
{
    jstring filename = NULL;
JNF_COCOA_ENTER(env);

    NSString *resourceName = JNFNormalizedNSStringForPath(env, javaResourceName);
        NSString *subDirectory = JNFNormalizedNSStringForPath(env, javaSubDirName);
        NSString *typeName = JNFNormalizedNSStringForPath(env, javaTypeName);

    NSString *path = [[NSBundle mainBundle] pathForResource:resourceName
                                                     ofType:typeName
                                                inDirectory:subDirectory];

    filename = JNFNormalizedJavaStringForPath(env, path);

JNF_COCOA_EXIT(env);
    return filename;
}


/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    getNativePathToApplicationBundle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_us_hall_osx_eio_FileManager_getNativePathToApplicationBundle
(JNIEnv *env, jclass clazz)
{
        jstring filename = nil;
JNF_COCOA_ENTER(env);

        NSBundle *mainBundle = [NSBundle mainBundle];
        filename = JNFNormalizedJavaStringForPath(env, [mainBundle bundlePath]);

JNF_COCOA_EXIT(env);
        return filename;
}


/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    __moveToTrash
 * Signature: (Ljava/lang/String;)V
 */

JNIEXPORT jboolean JNICALL Java_us_hall_osx_eio_FileManager__1moveToTrash
(JNIEnv *env, jclass clz, jstring url)
{
        __block jboolean returnValue = JNI_FALSE;
JNF_COCOA_ENTER(env);

    NSString *path = JNFNormalizedNSStringForPath(env, url);
    [JNFRunLoop performOnMainThreadWaiting:YES withBlock:^(){
        NSInteger res = 0;
        [[NSWorkspace sharedWorkspace] performFileOperation:NSWorkspaceRecycleOperation
                                                     source:[path stringByDeletingLastPathComponent]
                                                destination:nil
                                                      files:[NSArray arrayWithObject:[path lastPathComponent]]
                                                        tag:&res];
        returnValue = (res == 0);
    }];

JNF_COCOA_EXIT(env);

        return returnValue;
}

/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    __revealInFinder
 * Signature: (Ljava/lang/String;)V
 */

JNIEXPORT jboolean JNICALL Java_us_hall_osx_eio_FileManager__1revealInFinder
(JNIEnv *env, jclass clz, jstring url)
{
        __block jboolean returnValue = JNI_FALSE;
JNF_COCOA_ENTER(env);

    NSString *path = JNFNormalizedNSStringForPath(env, url);
    [JNFRunLoop performOnMainThreadWaiting:YES withBlock:^(){
        returnValue = [[NSWorkspace sharedWorkspace] selectFile:path inFileViewerRootedAtPath:@""];
    }];

JNF_COCOA_EXIT(env);

        return returnValue;
}

/*
 * Class:     us_hall_osx_eio_FileManager
 * Method:    getMimeType
 * Signature: (Ljava/lang/String;)Ljava/lang/String;       
 */
JNIEXPORT jstring JNICALL Java_us_hall_osx_eio_MacAttrUtils_mimeType(JNIEnv *env, jclass clazz, jstring jfilePath) {
	jobject jmimeType;
    JNF_COCOA_ENTER(env);
	NSString * file = [JNFNormalizedNSStringForPath(env,jfilePath) stringByResolvingSymlinksInPath];
	CFStringRef UTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (CFStringRef)[file pathExtension], NULL);
	CFStringRef MIMEType = UTTypeCopyPreferredTagWithClass (UTI, kUTTagClassMIMEType);
	CFRelease(UTI);
	jmimeType = JNFNSToJavaString(env,(NSString*)MIMEType);
	CFRelease(MIMEType);
	JNF_COCOA_EXIT(env);
	return jmimeType;	
}