#import <JavaVM/jni.h>
/*
 *  main.h
 *  hp
 *
 *  Created by Michael Hall on 2/12/12.
 *  Copyright 2012 __MyCompanyName__. All rights reserved.
 *
 */
JNIEnv *GetJEnv(JavaVM *vm,bool *wasAttached);
JavaVM *jvm(void);
