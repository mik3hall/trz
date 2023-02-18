package us.hall.trz.osx;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cocoa implements MacFileAttributeView {
	private static final String NSFileType = "NSFileType";
	private static final String NSFileTypeDirectory = "NSFileTypeDirectory";
	private static final String NSFileTypeRegular = "NSFileTypeRegular";
	private static final String NSFileTypeSymbolicLink = "NSFileTypeSymbolicLink";
	private static final String NSFileTypeSocket = "NSFileTypeSocket";
	private static final String NSFileTypeCharacterSpecial = "NSFileTypeCharacterSpecial";
	private static final String NSFileTypeBlockSpecial = "NSFileTypeBlockSpecial";
	private static final String NSFileTypeUnknown = "NSFileTypeUnknown";
	private static final String NSFileSize = "NSFileSize";
	private static final String NSFileModificationDate = "NSFileModificationDate";
	private static final String NSFileReferenceCount = "NSFileReferenceCount";
	private static final String NSFileDeviceIdentifier = "NSFileDeviceIdentifier";
	private static final String NSFileOwnerAccountName = "NSFileOwnerAccountName";
	private static final String NSFileGroupOwnerAccountName = "NSFileGroupOwnerAccountName";
	private static final String NSFilePosixPermissions = "NSFilePosixPermissions";
	private static final String NSFileSystemNumber = "NSFileSystemNumber";
	private static final String NSFileSystemFileNumber = "NSFileSystemFileNumber";
	private static final String NSFileExtensionHidden = "NSFileExtensionHidden";
	private static final String NSFileHFSCreatorCode = "NSFileHFSCreatorCode";
	private static final String NSFileHFSTypeCode = "NSFileHFSTypeCode";
	private static final String NSFileImmutable = "NSFileImmutable";
	private static final String NSFileAppendOnly = "NSFileAppendOnly";
	private static final String NSFileCreationDate = "NSFileCreationDate";
	private static final String NSFileOwnerAccountID = "NSFileOwnerAccountID";
	private static final String NSFileGroupOwnerAccountID = "NSFileGroupOwnerAccountID";
	private static final String NSFileBusy = "NSFileBusy";
	
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
