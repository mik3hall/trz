package us.hall.osx.eio;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
	   // mac_finder
	   public static final String CREATOR = "creator";
	   public static final String TYPE = "type";
	   public static final String INVISIBLE = "invisible";
	   public static final String NAME_LOCKED = "name_locked";
	   public static final String STATIONERY = "stationery";
	   public static final String ALIAS = "alias";
	   public static final String CUSTOM_ICON = "custom_icon";
	   public static final String LOCKED = "locked";
	   public static final String LABEL = "label";
	   // mac_ls
	   // CREATOR, TYPE, INVISIBLE, ALIAS also included as in mac_finder
	   public static final String PLAIN = "plain";
	   public static final String PACKAGE = "package";
	   public static final String APPLICATION = "application";
	   public static final String CONTAINER = "container";
	   public static final String SYMLINK = "symlink";		
	   public static final String VOLUME = "volume";
	   public static final String DEFAULT_APP = "default_application";
	   public static final String KIND = "kind";
	   public static final String APPLICATIONS = "applications";
	   // mac_cocoa
	   public static final String NSFileType = "NSFileType";
	   public static final String NSFileTypeDirectory = "NSFileTypeDirectory";
	   public static final String NSFileTypeRegular = "NSFileTypeRegular";
	   public static final String NSFileTypeSymbolicLink = "NSFileTypeSymbolicLink";
	   public static final String NSFileTypeSocket = "NSFileTypeSocket";
	   public static final String NSFileTypeCharacterSpecial = "NSFileTypeCharacterSpecial";
	   public static final String NSFileTypeBlockSpecial = "NSFileTypeBlockSpecial";
	   public static final String NSFileTypeUnknown = "NSFileTypeUnknown";
	   public static final String NSFileSize = "NSFileSize";
	   public static final String NSFileModificationDate = "NSFileModificationDate";
	   public static final String NSFileReferenceCount = "NSFileReferenceCount";
	   public static final String NSFileDeviceIdentifier = "NSFileDeviceIdentifier";
	   public static final String NSFileOwnerAccountName = "NSFileOwnerAccountName";
	   public static final String NSFileGroupOwnerAccountName = "NSFileGroupOwnerAccountName";
	   public static final String NSFilePosixPermissions = "NSFilePosixPermissions";
	   public static final String NSFileSystemNumber = "NSFileSystemNumber";
	   public static final String NSFileSystemFileNumber = "NSFileSystemFileNumber";
	   public static final String NSFileExtensionHidden = "NSFileExtensionHidden";
	   public static final String NSFileHFSCreatorCode = "NSFileHFSCreatorCode";
	   public static final String NSFileHFSTypeCode = "NSFileHFSTypeCode";
	   public static final String NSFileImmutable = "NSFileImmutable";
	   public static final String NSFileAppendOnly = "NSFileAppendOnly";
	   public static final String NSFileCreationDate = "NSFileCreationDate";
	   public static final String NSFileOwnerAccountID = "NSFileOwnerAccountID";
	   public static final String NSFileGroupOwnerAccountID = "NSFileGroupOwnerAccountID";
	   public static final String NSFileBusy = "NSFileBusy";		
	   
	   private static native String mimeType(String filePath);
	   
	   private static final HashMap<String,ArrayList<String>> trzViews = new HashMap<String,ArrayList<String>>();
	   
	   static { 
		   trzViews.put("mac_finder",new ArrayList<String>(Arrays.asList(new String[] {CREATOR,TYPE,INVISIBLE,NAME_LOCKED,STATIONERY,ALIAS,CUSTOM_ICON,LOCKED})));
		   trzViews.put("mac_ls",new ArrayList<String>(Arrays.asList(new String[] { CREATOR,TYPE,PLAIN,PACKAGE,APPLICATION,CONTAINER,ALIAS,SYMLINK,INVISIBLE,VOLUME,DEFAULT_APP,KIND,APPLICATIONS})));
		   trzViews.put("mac_cocoa", new ArrayList<String>(Arrays.asList(new String[] {NSFileType,NSFileTypeDirectory,NSFileTypeRegular,NSFileTypeSymbolicLink,NSFileTypeSocket,NSFileTypeCharacterSpecial,
				        NSFileTypeUnknown,NSFileSize,NSFileModificationDate,NSFileReferenceCount,NSFileDeviceIdentifier,NSFileOwnerAccountName,NSFileGroupOwnerAccountName,NSFilePosixPermissions,
				        NSFileSystemNumber,NSFileSystemFileNumber,NSFileExtensionHidden,NSFileHFSCreatorCode,NSFileHFSTypeCode,NSFileImmutable,NSFileAppendOnly,NSFileCreationDate,
				        NSFileGroupOwnerAccountID,NSFileBusy}))); 
	   }
	   
	   /**
	     * Return the attribute for the file
	     * The trz views are sequentially checked for handling the attr
	     * The attr is returned from the first view that supports it.
	     *
	     * @param f file to check for the attribute
	     * @param attr the attribute name
	     * @return The attribute object
	     */
	   public static Object getAttribute(File f,String attr) throws IOException {
		   if (trzViews.get("mac_finder").contains(attr))
			   return Files.getAttribute(f.toPath(),"mac_finder"+attr);
		   else if (trzViews.get("mac_ls").contains(attr))
			   return Files.getAttribute(f.toPath(),"mac_ls"+attr);
		   else if (trzViews.get("mac_cocoa").contains(attr))
			   return Files.getAttribute(f.toPath(),"mac_cocoa"+attr);
		   throw new IllegalArgumentException("No known file meta attribute " + attr);
	   }
	   
	   /*
	    * List supported trz file attribute views
	    * 
	    * @return array of attribute views
	    */
	   public static String[] getTRZViews() {
		   return trzViews.keySet().toArray(new String[0]);
	   }
	   
	   /*
	    * List attributes supported for the view
	    * 
	    * @param view to list trz attributes for
	    * @return list of view supported attributes
	    */
	   public static ArrayList<String> listAttributes(String view) {
		   return trzViews.get(view);
	   }
	   
	   /*
	    * Applications that handle the file
	    * 
	    * @param f the file
	    * @return array of applications supporting the file
	    */
	   public static File[] getApplications(File f) throws IOException {
		   Path[] apps = (Path[])Files.getAttribute(f.toPath(),"mac_ls:applications");
		   File[] appsList = new File[apps.length];
		   for (int i=0;i<apps.length;i++)
			   appsList[i] = apps[i].toFile();
		   return appsList;
	   }
	   
	   /*
	    * The default application supporting the file
	    * 
	    * @param f the file
	    * @return default application
	    */
	   public static File getDefaultApplication(File f) throws IOException {
		   Path app = (Path)Files.getAttribute(f.toPath(),"mac_ls:default_application");
		   return app.toFile();
	   }
	   
	   /*
	    * Open file with default application
	    */
	   public static void open(File f) {
		   	rtexec(new String[] { "open",f.getPath() });
	   }

	   /*
	    * Open file with application
	    * 
	    * @param f the file
	    * @param app the application
	    */
	   public static void open(File f,File app) {
		   rtexec(new String[] {"open","-a",app.getPath(),f.getPath()});
	   }

	   /*
	    * mime type for file
	    * 
	    * @param filePath path to file
	    * @return mime type
	    */
	   public static String getMimeType(String filePath) { return mimeType(filePath); }
	   
	   private static String rtexec(String[] args) {
	        try {
	            StringBuffer execout = new StringBuffer();
	            Process proc = Runtime.getRuntime().exec(args);
	            proc.waitFor();
	            InputStream inout = proc.getInputStream();
	            InputStream inerr = proc.getErrorStream();
	            byte []buffer = new byte[256];
	            while (true) {
	                int stderrLen = inerr.read(buffer, 0, buffer.length);
	                if (stderrLen > 0) {
	                    execout.append(new String(buffer,0,stderrLen));                   
	                }
	                int stdoutLen = inout.read(buffer, 0, buffer.length);
	                if (stdoutLen > 0) {
	                    execout.append(new String(buffer,0,stdoutLen));
	                }
	                if (stderrLen < 0 && stdoutLen < 0)
	                    break;
	            }   
	            return execout.toString();
	        }
	        catch(Throwable tossed) { tossed.printStackTrace(); }
	        return "-";
	    }
}
