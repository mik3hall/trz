/* * Copyright (C) 2011 Michael Hall * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * *     http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */package us.hall.trz.osx;import java.io.Closeable;import java.nio.file.spi.FileSystemProvider;import java.nio.file.attribute.*;import java.nio.file.*;import java.net.URI;import java.net.URISyntaxException;import java.util.*;import java.util.concurrent.locks.ReadWriteLock;import java.util.concurrent.locks.ReentrantReadWriteLock;import java.io.IOException;import us.hall.trz.osx.ws.AbstractWatchService;/** * Mac attribute implementation of FileSystem<br> * Initially based on nio zip file provider demo class * * @author Michael Hall * @version $Revision: 3cd81623675f $ * @since  */public class MacFileSystem extends FileSystem {				   	        private final FileSystemProvider provider, priorProvider;        private final ReadWriteLock closeLock = new ReentrantReadWriteLock();        private Set<Closeable> closeableObjects = new HashSet<Closeable>();        private static URI rootURI;        static {        	try {         	   rootURI = new URI("file",null,"/",null);        	}        	catch (URISyntaxException use) { rootURI = null; }        }		// Shouldn't be invoked		protected MacFileSystem() { throw new UnsupportedOperationException(); }			MacFileSystem(FileSystemProvider provider,FileSystemProvider priorProvider) {			this.provider = provider;			this.priorProvider = priorProvider;		}		        MacFileSystem(FileSystemProvider provider, Path fref) {            this(provider, fref.toString(), "/");        }        MacFileSystem(FileSystemProvider provider, String path, String defaultDir) {            this.provider = provider;            this.priorProvider = null;        }        @Override        public FileSystemProvider provider() {            return provider;        }        @Override        public boolean isOpen() {        	return priorProvider.getFileSystem(rootURI).isOpen();        }        @Override        public boolean isReadOnly() {            return priorProvider.getFileSystem(rootURI).isReadOnly();        }        @Override        public String getSeparator() {            return "/";        }                @Override        public void close() throws IOException {        	priorProvider.getFileSystem(rootURI).close();        }            final void begin() {            closeLock.readLock().lock();            if (!isOpen()) {                throw new ClosedFileSystemException();            }        }        final void end() {            closeLock.readLock().unlock();        }                boolean addCloseableObjects(Closeable obj) {            return closeableObjects.add(obj);        }                @Override        public Iterable<Path> getRootDirectories() {        	return priorProvider.getFileSystem(rootURI).getRootDirectories();        }        @Override        public Path getPath(String first,String... more) {            try {            	MacPath mp = new MacPath(priorProvider.getFileSystem(rootURI).getPath(first,more));            	return mp;            }            catch (Throwable tossed) { tossed.printStackTrace(); }            return null;        }  		        @Override        public Iterable<FileStore> getFileStores() {        	return priorProvider.getFileSystem(rootURI).getFileStores();        }        		private static final Set<String> supportedFileAttributeViews =			Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("basic", "mac_finder", "mac_ls", "mac_xattr")));        @Override        public Set<String> supportedFileAttributeViews() {            return supportedFileAttributeViews;        }        @Override        public PathMatcher getPathMatcher(String syntaxAndPattern) {        	return priorProvider.getFileSystem(rootURI).getPathMatcher(syntaxAndPattern);        }        @Override        public UserPrincipalLookupService getUserPrincipalLookupService() {        	return priorProvider.getFileSystem(rootURI).getUserPrincipalLookupService();        }        @Override        public WatchService newWatchService() throws IOException {        	if (System.getProperty("mac.watchservice","fsevent").equals("platform"))        		return priorProvider.getFileSystem(rootURI).newWatchService();        	else return AbstractWatchService.getImpl();        }}