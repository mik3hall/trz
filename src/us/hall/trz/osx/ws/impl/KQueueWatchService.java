package us.hall.trz.osx.ws.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.NotDirectoryException;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import us.hall.trz.osx.MacPath;
import us.hall.trz.osx.MacAttrUtils;
import us.hall.trz.osx.MacWatchUtils;
import us.hall.trz.osx.MacFileSystem;
import us.hall.trz.osx.ws.AbstractWatchKey;
import us.hall.trz.osx.ws.AbstractWatchService;
import us.hall.trz.osx.ws.MacFileKey;

public class KQueueWatchService extends AbstractWatchService implements WatchService {

	private static final Object EVENT_LOCK = new Object();

    // special key to indicate overflow condition
    private final WatchKey OVERFLOW_KEY =
        new AbstractWatchKey(null, null) {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public void cancel() {
            }
        };
        
    // background thread to read change events
    private final Poller poller;
    
    // Holds kqueue posted WatchEvent's
    ConcurrentLinkedQueue<WatchEvent<?>> postedEvents = new ConcurrentLinkedQueue<WatchEvent<?>>();
    
    public KQueueWatchService() throws IOException {
    	this((MacFileSystem)FileSystems.getDefault());
    }
    
    public KQueueWatchService(MacFileSystem fs) throws IOException {
        this.poller = new Poller(fs, this);
        this.poller.start();
    }
    
    @Override
    public WatchKey register(Path dir,
    				  WatchEvent.Kind<?>[] events,
    				  WatchEvent.Modifier... modifiers)
    	throws IOException
    {
    	for (WatchEvent.Modifier modifier : modifiers)
    		if (modifier == null)
    			throw new NullPointerException();
    	return poller.register(dir,events,modifiers);
    }
	    
	@Override
    protected final void implClose() throws IOException {
        // delegate to poller
        poller.close();
    }

    @Override
    public final WatchKey poll(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        WatchKey wk = super.poll(timeout,unit);
/*
        if (wk == null && (postedEvents.size() > 0 || poller.haveRequests())) {
        	wk = OVERFLOW_KEY;
        	wk.signalEvent(StandardWatchEventKinds.OVERFLOW, null);        	
        }
*/
        System.out.println("poll returning " + wk + " post events " + postedEvents.size() + " " + poller.haveRequests());
        return wk;
    }
    
	/**
	 * WatchKey implementation
	 */
//	private class KQueueWatchKey extends AbstractWatchKey
	public class KQueueWatchKey extends AbstractWatchKey
	{
		private final MacFileKey fileKey;

        // events (may be changed). set to null when watch key is invalid
        private volatile Set<? extends WatchEvent.Kind<?>> events;

        // For clone 
        KQueueWatchService watcher;

        KQueueWatchKey(KQueueWatchService watcher,
                        Path dir,
                        MacFileKey fileKey,
                        Set<? extends WatchEvent.Kind<?>> events)
        {
            super(dir, watcher);
            this.watcher = watcher;
            this.fileKey = fileKey;
            this.events = events;
        }
        
        @SuppressWarnings({"unchecked","unused"}) 
        private void postNativeEvent(String context, int eventType) {
        	System.out.println("postNativeEvent: " + context + " " + eventType + " " + Thread.currentThread());
    		WatchEvent.Kind<?> kind;
    		if (eventType == FILE_CREATED) kind = StandardWatchEventKinds.ENTRY_CREATE;
    		else if (eventType == FILE_DELETED) kind = StandardWatchEventKinds.ENTRY_DELETE;
    		else kind = StandardWatchEventKinds.ENTRY_MODIFY;
			if (kind == StandardWatchEventKinds.ENTRY_DELETE && context.equals("")) {
				cancel();
				poller.wakeup();
				signalEvent(kind,Paths.get(context));		// Signal event to exit any 'take'
			}
			else {
				postedEvents.add(new PostedEvent<Object>((WatchEvent.Kind<Object>)kind,context,this));       
				poller.wakeup();
			}
        }
 
    	public void processEvent(WatchEvent<?> evt) {
//    		new Exception("processEvent " + evt + " " + Thread.currentThread()).printStackTrace();
    		WatchEvent.Kind<?> kind = evt.kind();
    		String context = (String)evt.context();
			if (isValid())
				signalEvent(kind,Paths.get(context));
//			if (kind == StandardWatchEventKinds.ENTRY_DELETE && context.equals(""))
//				cancel();
    	}
    	
    	@SuppressWarnings("unused")
        MacPath getDirectory() {
            return (MacPath)watchable();
        }

        MacFileKey getFileKey() {
            return fileKey;
        }

        void invalidate() {
 //           MacWatchUtils.kqcancel(this);
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
        public synchronized void cancel() {
//        	System.out.println("KQueueWatchKey cancel " + Thread.currentThread());
            if (isValid()) {
                // delegate to poller
//            	System.out.println("KQueueWatchKey cancel passing off to poller...");
            	poller.cancel(this);            	
            }
        }

        public String toString() {
        	return new StringBuilder("KQueueWatchKey: ").append(watchable()).append(" events: ").append(events).toString();
        }
	}

	    /**
	     * Background thread to read from port
	     */
	    private class Poller extends AbstractPoller {
	        private final KQueueWatchService watcher;

	        // maps file key (dev/inode) to WatchKey
	        private final Map<MacFileKey,KQueueWatchKey> fileKey2WatchKey;
	        
	        /**
	         * Create a new instance
	         */
	        Poller(MacFileSystem fs, KQueueWatchService watcher) {
	            this.watcher = watcher;
	            this.fileKey2WatchKey = new HashMap<MacFileKey,KQueueWatchKey>();
	        }	    	

	        @Override 
	        String getName() { 
	        	return "kqueue polling thread"; 
	        }
	        
	        @Override
	        void wakeup() {		// throws IOException {
//	        	System.out.println("Poller wakeup " + Thread.currentThread() + " " + Thread.holdsLock(EVENT_LOCK));
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
	            KQueueWatchKey watchKey = fileKey2WatchKey.get(fileKey);
	            if (watchKey == null) {
//	                updateEvents(watchKey, events);
//	                return watchKey;
		            // create watch key and insert it into maps
		            watchKey = new KQueueWatchKey(watcher, dir, fileKey, events);
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
//	            registerChildren(dir, watchKey, false);

	            return watchKey;
	        }

		    /**
	         * Calls port_associate to register the given path.
	         * Returns pointer to fileobj structure that is allocated for
	         * the registration.
	         */
	        void registerImpl(KQueueWatchKey watchKey,Path dir,int kqevents)
	        {
	        	MacWatchUtils.kqregister(watchKey,dir,kqevents);
	        }
	        
	        // cancel single key
	        @Override
	        void implCancelKey(WatchKey obj) {
	           KQueueWatchKey key = (KQueueWatchKey)obj;
//	           System.out.println("Poller implCancelKey " + obj);
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
	                for (Map.Entry<MacFileKey,KQueueWatchKey> entry: fileKey2WatchKey.entrySet()) {
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
	                	synchronized(EVENT_LOCK) {
	                		try {
	    	                	// Process any outstanding requests
//	    	                	System.out.println("run before processRequests");
	    	                	boolean shutdown = processRequests();
//	    	                	System.out.println("run after processRequests");
	    	                	if (shutdown) break; 
//	    	                	System.out.println("run before polling events");
	    	                	WatchEvent<?> evt = postedEvents.poll();
	    	                	while (evt != null) {
	    	                		((PostedEvent<?>)evt).key().processEvent(evt);
	    	                		evt = postedEvents.poll();
	    	                	}
//	                			System.out.println("KQueueWatchService before EVENT_LOCK wait");
	                			if (!haveRequests() && postedEvents.size() == 0)
	                				EVENT_LOCK.wait();
//	                			System.out.println("KQueueWatchService after EVENT_LOCK wait");
	                		}
	                		catch(InterruptedException iex) { iex.printStackTrace(); }
	                	}
	                }
	            } catch (Throwable tossed) {
	            	System.out.println("KQueueWatchService exception");
	                tossed.printStackTrace();
	            }
//		        System.out.println("KQueueWatchService exiting run loop");
	        }
	    }
	    
	    /**
	     * simple WatchEvent implementation tu queue up kqueue notifications
	     */
	    private static class PostedEvent<T> implements WatchEvent<T> {
	        private final WatchEvent.Kind<T> kind;
	        private final T context;
	        private final KQueueWatchKey watchKey;
	        
	        PostedEvent(WatchEvent.Kind<T> type, T context, KQueueWatchKey watchKey) {
	            this.kind = type;
	            this.context = context;
	            this.watchKey = watchKey;
	        }

	        @Override
	        public WatchEvent.Kind<T> kind() {
	            return kind;
	        }

	        @Override
	        public T context() {
	            return context;
	        }
	        
	        @Override 
	        public int count() { return 1; }
	        
	        public KQueueWatchKey key() { return watchKey; }
	        
	        public String toString() {
	        	StringBuilder b = new StringBuilder("PostedEvent ");
	        	b.append(kind.name());
	        	b.append(" Context: ");
	        	b.append(context);
	        	b.append(" (").append(new Integer(count()).toString()).append(")");
	        	return b.toString();
	        }
	    }
}
