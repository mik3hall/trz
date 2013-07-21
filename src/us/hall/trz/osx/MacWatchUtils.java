package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

public class MacWatchUtils {

	static { System.loadLibrary("macattrs"); }

	private native static void kqregister(WatchKey watchKey,String path,int kqevents) throws IOException;
	private native static void fsregister(WatchKey watchKey,String path,int fsevents) throws IOException;
	private native static void kevent();
	
	public native static void kqcancel(WatchKey watchKey);
//	private native static WatchKey ksregister(String filePath,int events,int modifiers) throws IOException;
	
	public static void kqregister(WatchKey watchKey,Path p,int kqevents) {
		try {
			kqregister(watchKey,p.toAbsolutePath().toString(),kqevents);
		}
		catch (IOException ioex) { ioex.printStackTrace(); }
	}

	public static void fsregister(WatchKey watchKey,Path p,int fsevents) {
		try {
			fsregister(watchKey,p.toAbsolutePath().toString(),fsevents);
		}
		catch (IOException ioex) { ioex.printStackTrace(); }
	}
	
	public static void keventWait() {
		kevent();
	}
}
