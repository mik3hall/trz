package us.hall.trz.osx.ws;

public class MacFileKey {
	long fileSystemNumber, fileSystemFileNumber;

	public MacFileKey(long fileSystemNumber,long fileSystemFileNumber) {
		this.fileSystemNumber = fileSystemNumber;
		this.fileSystemFileNumber = fileSystemFileNumber;
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
        return (this.fileSystemNumber == other.fileSystemNumber) && (this.fileSystemFileNumber == other.fileSystemFileNumber);
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
