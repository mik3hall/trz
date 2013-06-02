package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class XAttr implements MacFileAttributeView {
	Path file;
	MacXAttrInfo info;
	
	XAttr(Path file,boolean followLinks) {
		this.file = file;
		info = MacAttrUtils.getXAttrInfo(file);
	}

	// should @Override?
	 public final String name() { return "mac_xattr"; }
	 
	  public Map<String,Object> readAttributes(String[] requested,LinkOption... options)
	  {
	      Map<String,Object> map = new HashMap<String,Object>();
	      for (String request : requested) {
	         final int colon = request.indexOf(":");
	      
			 final String name;
	      
			 if (colon == -1) {
				name = request;
			 }
			 else {
				String view = request.substring(0,colon);
			    if (!request.substring(0, colon).equals("mac_xattr"))
			    	throw new IllegalArgumentException("Invalid view " + view + " not mac_xattr");
				name = request.substring(colon+1, request.length());
			 }
			 if ("*".equals(name)) {
				Set<String> s = info.getXAttrMap().keySet();
				for (String attrName : s) {
					map.put("mac_xattr:"+attrName, info.getXAttrMap().get(attrName));
				}
			 }
			 else {
			    map.put(name, info.getAttribute(name));
		     }
		  }	
	      return map;
	  }
	   
	   public void setAttribute(String attribute, Object value,LinkOption... options) throws IOException {
		   throw new ReadOnlyException("setAttribute currently unsupported for XAttr");
	   }
}
