package us.hall.trz.osx.ws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NSFileSystemNumber
 * The corresponding value is an NSNumber object that specifies the filesystem 
 * number of the file system. The value corresponds to the value of st_dev, 
 * as returned by stat(2).
 *
 * NSFileSystemFileNumber
 * The corresponding value is an NSNumber object containing an unsigned long. 
 * The value corresponds to the value of st_ino, as returned by stat(2).
 */

public class MacFileKey {
	long fileSystemNumber, fileSystemFileNumber;

	public MacFileKey(long fileSystemNumber,long fileSystemFileNumber) {
		this.fileSystemNumber = fileSystemNumber;
		this.fileSystemFileNumber = fileSystemFileNumber;
	}
	
	public static MacFileKey fileKey(Path p) throws IOException {
		long fileSystemNumber = 
			((Long)Files.getAttribute(p,"mac_cocoa:NSFileSystemNumber")).longValue();
		long fileSystemFileNumber = 
			((Long)Files.getAttribute(p,"mac_cocoa:NSFileSystemFileNumber")).longValue();
		return new MacFileKey(fileSystemNumber, fileSystemFileNumber);
	}
	 
    @Override
    public int hashCode() {
        return (int)(fileSystemNumber ^ (fileSystemNumber >>> 32)) +
               (int)(fileSystemFileNumber ^ (fileSystemFileNumber >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MacFileKey))
            return false;
        MacFileKey other = (MacFileKey)obj;
        return (this.fileSystemNumber == other.fileSystemNumber) && 
        	(this.fileSystemFileNumber == other.fileSystemFileNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(fileSystemNumber=")
          .append(Long.toHexString(fileSystemNumber))
          .append(",fileSystemFileNumber=")
          .append(fileSystemFileNumber)
          .append(')');
        return sb.toString();
    }
}
