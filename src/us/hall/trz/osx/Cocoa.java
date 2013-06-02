package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cocoa implements MacFileAttributeView {
	static final String NSFileType = "NSFileType";
	static final String NSFileTypeDirectory = "NSFileTypeDirectory";
	static final String NSFileTypeRegular = "NSFileTypeRegular";
	static final String NSFileTypeSymbolicLink = "NSFileTypeSymbolicLink";
	static final String NSFileTypeSocket = "NSFileTypeSocket";
	static final String NSFileTypeCharacterSpecial = "NSFileTypeCharacterSpecial";
	static final String NSFileTypeBlockSpecial = "NSFileTypeBlockSpecial";
	static final String NSFileTypeUnknown = "NSFileTypeUnknown";
	static final String NSFileSize = "NSFileSize";
	static final String NSFileModificationDate = "NSFileModificationDate";
	static final String NSFileReferenceCount = "NSFileReferenceCount";
	static final String NSFileDeviceIdentifier = "NSFileDeviceIdentifier";
	static final String NSFileOwnerAccountName = "NSFileOwnerAccountName";
	static final String NSFileGroupOwnerAccountName = "NSFileGroupOwnerAccountName";
	static final String NSFilePosixPermissions = "NSFilePosixPermissions";
	static final String NSFileSystemNumber = "NSFileSystemNumber";
	static final String NSFileSystemFileNumber = "NSFileSystemFileNumber";
	static final String NSFileExtensionHidden = "NSFileExtensionHidden";
	static final String NSFileHFSCreatorCode = "NSFileHFSCreatorCode";
	static final String NSFileHFSTypeCode = "NSFileHFSTypeCode";
	static final String NSFileImmutable = "NSFileImmutable";
	static final String NSFileAppendOnly = "NSFileAppendOnly";
	static final String NSFileCreationDate = "NSFileCreationDate";
	static final String NSFileOwnerAccountID = "NSFileOwnerAccountID";
	static final String NSFileGroupOwnerAccountID = "NSFileGroupOwnerAccountID";
	static final String NSFileBusy = "NSFileBusy";
	
	  Path file;
	  MacCocoaInfo info;
	  
	  Cocoa(Path file,boolean followLinks) {
		   this.file = file;
		   info = MacAttrUtils.getCocoaInfo(file);
	   }
	   
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
				    if (!request.substring(0, colon).equals("mac_cocoa"))
				    	throw new IllegalArgumentException("Invalid view " + view + " not mac_cocoa");
					name = request.substring(colon+1, request.length());
				 }
				 if ("*".equals(name)) {
					Set<String> s = info.getMap().keySet();
					for (String attrName : s) {
						map.put("mac_cocoa:"+attrName, info.getMap().get(attrName));
					}
				 }
				 else {
				    map.put(name, info.getAttribute(name));
			     }
			  }	
		      return map;
	   }
	   
	   public void setAttribute(String attribute, Object value,LinkOption... options) throws IOException {
			  throw new ReadOnlyException("Cocoa setting of attributes is currently unsupported");
	   }
}
