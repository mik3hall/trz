/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package us.hall.trz.osx.ws.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class AbstractPoller implements Runnable {

    // list of requests pending to the poller thread
    private final LinkedList<Request> requestList;

    // set to true when shutdown
    private boolean shutdown;

    protected AbstractPoller() {
        this.requestList = new LinkedList<Request>();
        this.shutdown = false;
    }
 
    public boolean haveRequests() { return requestList.size() > 0; }
    
    /**
     * Starts the poller thread
     */
    public void start() {
        final Runnable thisRunnable = this;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                Thread thr = new Thread(thisRunnable,getName());
                thr.setDaemon(true);
                thr.start();
                return null;
            }
         });
    }
     
    /**
     *  @return name of WatchService extending this class
     */
    abstract String getName();
    
    /**
     * Wakeup poller thread so that it can service pending requests
     */
    abstract void wakeup() throws IOException;

    /**
     * Executed by poller thread to register directory for changes
     */
    abstract Object implRegister(Path path,
                                 Set<? extends WatchEvent.Kind<?>> events,
                                 WatchEvent.Modifier... modifiers);

    /**
     * Executed by poller thread to cancel key
     */
    abstract void implCancelKey(WatchKey key);

    /**
     * Executed by poller thread to shutdown and cancel all keys
     */
    abstract void implCloseAll();

    /**
     * Shutdown poller thread
     */
    final void close() throws IOException {
        invoke(RequestType.CLOSE);
    }

    /**
     * Types of request that the poller thread must handle
     */
    private static enum RequestType {
        REGISTER,
        CANCEL,
        CLOSE;
    }

    /**
     * Encapsulates a request (command) to the poller thread.
     */
    private static class Request {
        private final RequestType type;
        private final Object[] params;

        private boolean completed = false;
        private Object result = null;

        Request(RequestType type, Object... params) {
            this.type = type;
            this.params = params;
        }

        RequestType type() {
            return type;
        }

        Object[] parameters() {
            return params;
        }

        void release(Object result) {
            synchronized (this) {
                this.completed = true;
                this.result = result;
                notifyAll();
            }
        }

        /**
         * Await completion of the request. The return value is the result of
         * the request.
         */
        Object awaitResult() {
            synchronized (this) {
                while (!completed) {
                    try {
                        wait();
                    } catch (InterruptedException x) {
                        // ignore
                    }
                }
                return result;
            }
        }
    }

	/**
     * Requests, and waits on, poller thread to register given file.
     */
    final WatchKey register(Path dir,
                            WatchEvent.Kind<?>[] events,
                            WatchEvent.Modifier... modifiers)
        throws IOException
    {
        // validate arguments before request to poller
        if (dir == null)
            throw new NullPointerException();
        if (events.length == 0)
            throw new IllegalArgumentException("No events to register");
        Set<WatchEvent.Kind<?>> eventSet = new HashSet<WatchEvent.Kind<?>>(events.length);
        for (WatchEvent.Kind<?> event: events) {
            // standard events
            if (event == ENTRY_CREATE ||
                event == ENTRY_MODIFY ||
                event == ENTRY_DELETE)
            {
                eventSet.add(event);
                continue;
            }

            // OVERFLOW is ignored
            if (event == OVERFLOW) {
                if (events.length == 1)
                    throw new IllegalArgumentException("No events to register");
                continue;
            }

            // null/unsupported
            if (event == null)
                throw new NullPointerException("An element in event set is 'null'");
            throw new UnsupportedOperationException(event.name());
        }
        return (WatchKey)invoke(RequestType.REGISTER, dir, eventSet, modifiers);
    }
 
    /**
     * Cancels, and waits on, poller thread to cancel given key.
     */
    final void cancel(WatchKey key) {
        try {
            invoke(RequestType.CANCEL, key);
        } catch (IOException x) {
            // should not happen
            throw new AssertionError(x.getMessage());
        }
    }
    
    /**
     * Enqueues request to poller thread and waits for result
     */
    private Object invoke(RequestType type, Object... params) throws IOException {
        // submit request
//    	System.out.println("AbstractPoller invoke " + type + " shutdown is " + shutdown + " " + Thread.currentThread());
        Request req = new Request(type, params);
        synchronized (requestList) {
            if (shutdown) {
                throw new ClosedWatchServiceException();
            }
            requestList.add(req);
        }
        
        // wakeup thread
        wakeup();
        // wait for result
//        System.out.println("AP await result " + Thread.currentThread());
        Object result = req.awaitResult();

        if (result instanceof RuntimeException)
            throw (RuntimeException)result;
        if (result instanceof IOException )
            throw (IOException)result;
        return result;
    }

    /**
     * Invoked by poller thread to process all pending requests
     *
     * @return  true if poller thread should shutdown
     */
    @SuppressWarnings("unchecked")
    boolean processRequests() {
//    	System.out.println("AbstractPoller processRequests " + Thread.currentThread());
        synchronized (requestList) {
            Request req;
            while ((req = requestList.poll()) != null) {
//            	System.out.println("req="+req);
                // if in process of shutdown then reject request
                if (shutdown) {
                    req.release(new ClosedWatchServiceException());
                }

                switch (req.type()) {
                    /**
                     * Register directory
                     */
                    case REGISTER: {
                        Object[] params = req.parameters();
                        Path path = (Path)params[0];
                        Set<? extends WatchEvent.Kind<?>> events =
                            (Set<? extends WatchEvent.Kind<?>>)params[1];
                        WatchEvent.Modifier[] modifiers =
                            (WatchEvent.Modifier[])params[2];
                        req.release(implRegister(path, events, modifiers));
                        break;
                    }
                    /**
                     * Cancel existing key
                     */
                    case CANCEL : {
                        Object[] params = req.parameters();
                        WatchKey key = (WatchKey)params[0];
                        implCancelKey(key);
                        req.release(null);
                        break;
                    }
                    /**
                     * Close watch service
                     */
                    case CLOSE: {
                        implCloseAll();
                        req.release(null);
                        shutdown = true;
                        break;
                    }

                    default:
                        req.release(new IOException("request not recognized"));
                }
            }
        }
        return shutdown;
    }
}
