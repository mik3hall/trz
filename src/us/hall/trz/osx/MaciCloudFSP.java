package us.hall.trz.osx;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

public class MaciCloudFSP extends FileSystemProvider {
	MacFileSystemProvider macFSP = new MacFileSystemProvider();
	
    private static final String scheme = "icloud";

    @Override
    public String getScheme() {
       return scheme;
    }
    
    @Override
    public Path getPath(URI uri) {
		return macFSP.getPath(uri);
    }    
    
    @Override
    public FileSystem getFileSystem(URI uri) {
    	return null;
    }
    
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
        throws IOException
    {
       return null;
    }

    @Override
    public boolean isSameFile(Path path, Path other) throws IOException {
        return macFSP.isSameFile(path,other);
    }
    
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
    	macFSP.checkAccess(path,modes);
    }
    
    @Override
    public void copy(Path src, Path target, CopyOption... options)
        throws IOException
    {
    	macFSP.copy(src,target,options);
    }
    
    @Override
    public void createDirectory(Path path, FileAttribute<?>... attrs)
        throws IOException
    {
    	macFSP.createDirectory(path,attrs);
    }
    
    @Override
    public final void delete(Path path) throws IOException {
    	macFSP.delete(path);
    }
    
    @Override
    public <V extends FileAttributeView> V
        getFileAttributeView(Path path, Class<V> type, LinkOption... options)
    {
    	return macFSP.getFileAttributeView(path, type, options);
    }
    
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return macFSP.getFileStore(path);
    }
    
    @Override
    public boolean isHidden(Path path) throws IOException {
        return macFSP.isHidden(path);
    }
    
    @Override
    public void move(Path src, Path target, CopyOption... options)
        throws IOException
    {
    	macFSP.move(src,target,options);
    }
    
    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
        throws IOException
    {
        return macFSP.newByteChannel(path,options,attrs);
    }
    
    @Override
    public DirectoryStream<Path> newDirectoryStream(
        Path path, Filter<? super Path> filter) throws IOException
    {
        return macFSP.newDirectoryStream(path,filter);
    }
    
    @Override
    public <A extends BasicFileAttributes> A
        readAttributes(Path path, Class<A> type, LinkOption... options)
        throws IOException
    {
        return macFSP.readAttributes(path,type,options);
    }
 
    @Override
    public Map<String, Object>
        readAttributes(Path path, String attributes, LinkOption... options)
        throws IOException
    {
    	return macFSP.readAttributes(path,attributes,options);
    }
    
    @Override
    public void setAttribute(Path path, String attribute,
                             Object value, LinkOption... options)
        throws IOException
    {
    	macFSP.setAttribute(path,attribute,value,options);
    }
}
