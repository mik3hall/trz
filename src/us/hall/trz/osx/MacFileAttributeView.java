package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.Map;

public interface MacFileAttributeView {
	public Map<String,Object> readAttributes(String[] attributes, LinkOption... options) throws IOException;
	public void setAttribute(String attribute,Object value,LinkOption... options) throws IOException;
}
