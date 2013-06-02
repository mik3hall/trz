package us.hall.trz.osx.ws.impl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import us.hall.trz.osx.MacAttrUtils;
import us.hall.trz.osx.MacFileSystem;
import us.hall.trz.osx.MacPath;
import us.hall.trz.osx.MacWatchUtils;
import us.hall.trz.osx.ws.AbstractWatchKey;
import us.hall.trz.osx.ws.AbstractWatchService;
import us.hall.trz.osx.ws.MacFileKey;


public class FSEventWatchService extends AbstractWatchService {
	
	private static final Object EVENT_LOCK = new Object();

    // background thread to read change events
    private final Poller poller;
    
	public FSEventWatchService() throws IOException {
    	this((MacFileSystem)FileSystems.getDefault());		
	}

    public FSEventWatchService(MacFileSystem fs) throws IOException {
        this.poller = new Poller(fs, this);
        this.poller.start();
    }
    
    public WatchKey register(Path dir,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
         throws IOException
    {
    	return null;
    }
 
	@Override
    protected final void implClose() throws IOException {
        // delegate to poller
//        poller.close();
    }
	/**
	 * WatchKey implementation
	 */
	private class FSEventWatchKey extends AbstractWatchKey
	{
		private final MacFileKey fileKey;

        // events (may be changed). set to null when watch key is invalid
        private volatile Set<? extends WatchEvent.Kind<?>> events;

	    FSEventWatchKey(FSEventWatchService watcher,
	            Path dir,
	            MacFileKey fileKey,
	            Set<? extends WatchEvent.Kind<?>> events)
	    {
	    	super(dir, watcher);
	    	this.fileKey = fileKey;
	    	this.events = events;
	    }
	    
	    @SuppressWarnings("unused")
	    private void postNativeEvent(String context, int eventType) {
	    	WatchEvent.Kind<?> kind;
	    	if (eventType == FILE_CREATED) kind = StandardWatchEventKinds.ENTRY_CREATE;
	    	else if (eventType == FILE_DELETED) kind = StandardWatchEventKinds.ENTRY_DELETE;
	    	else kind = StandardWatchEventKinds.ENTRY_MODIFY;
	    	synchronized(this) {
	    		if (isValid())
	    			signalEvent(kind,Paths.get(context));
	    		if (eventType == FILE_DELETED && context.equals(""))
	    			cancel();
	    	}
	    }
	
	    @SuppressWarnings("unused")
	    MacPath getDirectory() {
	    	return (MacPath)watchable();
	    }
	
	    MacFileKey getFileKey() {
	    	return fileKey;
	    }
	
	    void invalidate() {
	    	events = null;
	    }
	
	    @SuppressWarnings("unused")
	    Set<? extends WatchEvent.Kind<?>> events() {
	    	return events;
	    }
	
	    @SuppressWarnings("unused")
	    void setEvents(Set<? extends WatchEvent.Kind<?>> events) {
	    	this.events = events;
	    }
	
	    @Override
	    public boolean isValid() {
	    	return events != null;
	    }
	
	    @Override
	    public void cancel() {
	    	if (isValid()) {
	    		// delegate to poller
	    		poller.cancel(this);
	    	}
	    }

	}
    
	/**
     * Background thread to read from port
     */
    private class Poller extends AbstractPoller {
        private final FSEventWatchService watcher;

        // maps file key (dev/inode) to WatchKey
        private final Map<MacFileKey,FSEventWatchKey> fileKey2WatchKey;
        
        /**
         * Create a new instance
         */
        Poller(MacFileSystem fs, FSEventWatchService watcher) {
            this.watcher = watcher;
            this.fileKey2WatchKey = new HashMap<MacFileKey,FSEventWatchKey>();
        }	    	

        @Override 
        String getName() { 
        	return "kqueue polling thread"; 
        }
        
        @Override
        void wakeup() throws IOException {
        	synchronized(EVENT_LOCK) {
        		EVENT_LOCK.notifyAll();
        	}
        }
        
        @Override
        Object implRegister(Path dir,
                            Set<? extends WatchEvent.Kind<?>> events,
                            WatchEvent.Modifier... modifiers)
        {
            // no modifiers supported at this time
            if (modifiers != null && modifiers.length > 0) {
            	return new UnsupportedOperationException("Modifier not supported");
            }
            
            if (!MacAttrUtils.isDirectory(dir))
            	return new NotDirectoryException(dir.toString());

            // return existing watch key after updating events if already
            // registered
            MacFileKey fileKey = MacAttrUtils.fileKey(dir);
            FSEventWatchKey watchKey = fileKey2WatchKey.get(fileKey);
            if (watchKey == null) {
//                updateEvents(watchKey, events);
//                return watchKey;
	            // create watch key and insert it into maps
	            watchKey = new FSEventWatchKey(watcher, dir, fileKey, events);
	            fileKey2WatchKey.put(fileKey, watchKey);
            }
            
            // register directory
            int kqevents = 0;
            if (events.contains(StandardWatchEventKinds.ENTRY_CREATE))
            	kqevents |= FILE_CREATED;
            if (events.contains(StandardWatchEventKinds.ENTRY_DELETE))
            	kqevents |= FILE_DELETED;
            if (events.contains(StandardWatchEventKinds.ENTRY_MODIFY))
            	kqevents |= FILE_MODIFIED;
            try {
            	registerImpl(watchKey,dir,kqevents);
            } catch (Throwable tossed) {
                return new IOException(tossed.getMessage());
            }

            // register all entries in directory
//            registerChildren(dir, watchKey, false);

            return watchKey;
        }

	    /**
         * Calls port_associate to register the given path.
         * Returns pointer to fileobj structure that is allocated for
         * the registration.
         */
        void registerImpl(FSEventWatchKey watchKey,Path dir,int fsevents)
        {
        	MacWatchUtils.kqregister(watchKey,dir,fsevents);
        }
        
        // cancel single key
        @Override
        void implCancelKey(WatchKey obj) {
           FSEventWatchKey key = (FSEventWatchKey)obj;
           if (key.isValid()) {
               fileKey2WatchKey.remove(key.getFileKey());
               // TODO call native to free resources related to this WatchKey
               key.invalidate();
           }
        }

        // close watch service
        @Override
        synchronized void implCloseAll() {
        	try {
                // release all native resources

                // invalidate all keys
                for (Map.Entry<MacFileKey,FSEventWatchKey> entry: fileKey2WatchKey.entrySet()) {
                    entry.getValue().invalidate();
                }

                // clean-up
                fileKey2WatchKey.clear();

                // free global resources
        	}
        	catch (Throwable tossed) {
        		tossed.printStackTrace();
        	}
        }
    	
        /**
         * Poller main loop. Blocks on port_getn waiting for events and then
         * processes them.
         */
        @Override
        public void run() {
            try {
                for (;;) {
                	// Process any outstanding requests
//                	System.out.println("run before processRequests");
                	boolean shutdown = processRequests();
//                	System.out.println("run after processRequests");
                	if (shutdown) break; 
                	synchronized(EVENT_LOCK) {
                		try {
//                			System.out.println("KQueueWatchService before EVENT_LOCK wait");
                			EVENT_LOCK.wait();
//                			System.out.println("KQueueWatchService after EVENT_LOCK wait");
                		}
                		catch(InterruptedException iex) { iex.printStackTrace(); }
                	}
                }
            } catch (Throwable tossed) {
            	System.out.println("KQueueWatchService exception");
                tossed.printStackTrace();
            }
//	        System.out.println("KQueueWatchService exiting run loop");
        }
    }
}