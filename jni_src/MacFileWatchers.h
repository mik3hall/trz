static int FILE_CREATED   = 0x00000001;
static int FILE_MODIFIED  = 0x00000002;
static int FILE_DELETED   = 0x00000008;

static void throwIOException(JNIEnv *env, const char *msg);
JNIEnv *GetJEnv(JavaVM *vm,bool *wasAttached);
JavaVM *jvm(void);
