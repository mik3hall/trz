package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;

public interface MacFileAttributeView extends FileAttributeView {
	public Map<String,Object> readAttributes(String[] attributes, LinkOption... options) throws IOException;
	public void setAttribute(String attribute,Object value,LinkOption... options) throws IOException;
}
