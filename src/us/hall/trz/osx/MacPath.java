package us.hall.trz.osx;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import us.hall.trz.osx.ws.AbstractWatchService;

public class MacPath implements Path {
	private final Path proxy;
    // package-private
    MacPath(Path proxy) {
    	this.proxy = proxy;
    }

    /**
     * @return default FileSystem Path that proxy's pass through Path operations for us
     */
    Path getProxy() { return proxy; }
    
    public MacPath getRoot() {
        if (this.isAbsolute()) 
        	return new MacPath(proxy.getFileSystem().getPath("/")); 
        return null;
    }

    @Override
    public Path getFileName() {
    	return proxy.getFileName();
    }
    
    public Path getName(int index) {
    	return proxy.getName(index);
    }

    public Path getParent() {
    	return proxy.getParent();
    }
    
    public int getNameCount() {
    	return proxy.getNameCount();
    }
	
    public Map<String,Object> readAttributes(String attributes,LinkOption... options) throws IOException {
        return readAttributes(attributes.split(","), options);
    }

	protected Map<String,Object> readAttributes(String[] attributes, LinkOption... options) throws IOException {
		return new HashMap<String,Object>();			// FIXME		
/*		
		for (String attr : attributes) {
			final int colon = attr.indexOf(":");
			
			final String view;
			final String name;
			
			if (colon == -1) {
				view = "basic";
				name = attr;
			}
			else {
				view = attr.substring(0, colon);
				name = attr.substring(colon+1, attr.length());
			}
			
			if ("*".equals(name)) {
				if ("basic".equals(view)) {
					map.putAll(readAttributes(new String[] {
						"basic:lastModifiedTime",
						"basic:lastAccessTime",
						"basic:creationTime",
						"basic:size",
						"basic:isRegularFile",
						"basic:isDirectory",
						"basic:isSymbolicLink",
						"basic:isOther",
						"basic:fileKey"
					}));
				}
				else if ("zip".equals(view)) {
					map.putAll(readAttributes(new String[] {
						"zip:comment",
						"zip:compressedSize",
						"zip:crc",
						"zip:extra",
						"zip:method",
						"zip:name",
						"zip:isArchiveFile",
						"zip:versionMadeBy",
						"zip:extAttrs"
					}));
				}
				else if ("jar".equals(view)) {
					map.putAll(readAttributes(new String[] {
						"jar:manifestAttributes",
						"jar:entryAttributes"
					}));
				}
			}
			else {
				map.put(attr, getAttribute(attr));
			}
		}
		
		return map;
*/
	}

	@Override
    public Path subpath(int beginIndex, int endIndex) {
    	return proxy.subpath(beginIndex, endIndex);
    }

	@Override
    public Path toRealPath(LinkOption... options) throws IOException {
    	return new MacPath(proxy.toRealPath(options));
    }

	@Override
    public MacPath toAbsolutePath() {
    	return new MacPath(proxy.toAbsolutePath());
    }

	@Override
    public URI toUri() {
    	return proxy.toUri();
    }
    
	@Override
    public Path relativize(Path other) {
    	return new MacPath(proxy.relativize(other));
    }

    //@Override
    public FileSystem getFileSystem() {
    	return FileSystems.getDefault();
    }

    @Override
    public boolean isAbsolute() {
    	return proxy.isAbsolute();		
    }

    @Override
    public Path resolve(Path other) {
    	if (other instanceof MacPath)
    		return new MacPath(proxy.resolve(((MacPath)other).proxy));
    	return new MacPath(proxy.resolve(other));
    }
    
    @Override
    public Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    @Override
    public boolean startsWith(Path other) {
    	if (other instanceof MacPath)
    		return proxy.startsWith(((MacPath)other).proxy);
    	return proxy.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
    	if (other instanceof MacPath)
    		return proxy.endsWith(((MacPath)other).proxy);
    	return proxy.endsWith(other);
    }
 
    @Override
    public String toString() {
    	return proxy.toString();
    }

    @Override
    public int hashCode() {
    	return proxy.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof MacPath)
    		return proxy.equals(((MacPath)obj).proxy);
    	return proxy.equals(obj);
    }

    @Override
    public int compareTo(Path other) {
    	if (other instanceof MacPath)
    		return proxy.compareTo(((MacPath)other).proxy);
    	return proxy.compareTo(other);
    }

    @Override
    public final WatchKey register(WatchService watcher,
                                   WatchEvent.Kind<?>... events)
        throws IOException
    {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }
    
    @Override
    public WatchKey register(
            WatchService watcher,
            WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) throws IOException 
    {
        if (watcher == null)
            throw new NullPointerException();
        if (!(watcher instanceof AbstractWatchService))
        	return proxy.register(watcher,events,modifiers);       
        checkRead();
        return ((AbstractWatchService)watcher).register(this, events, modifiers);
    }

    void checkRead() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkRead(toString());	// Simplifies sometime path manipulation of UnixPath 
        								// - OK to skip? TODO answer
    }
    
    @Override
    public Iterator<Path> iterator() {
    	return proxy.iterator();
    }
    
    @Override
    public Path normalize() {
    	return new MacPath(proxy.normalize());
    }

    @Override
    public final boolean endsWith(String other) {
        return proxy.endsWith(other);
    }
    
    @Override
    public Path resolveSibling(Path other) {
    	if (other instanceof MacPath)
    		return new MacPath(proxy.resolveSibling(((MacPath)other).proxy));
        return proxy.resolveSibling(other);
    }
    
    @Override
    public final Path resolveSibling(String other) {
    	return proxy.resolveSibling(other);
    }
    
    @Override
    public final boolean startsWith(String other) {
    	return proxy.startsWith(other);
    }
    
    @Override
    public final File toFile() {
    	return proxy.toFile();
    }
}
