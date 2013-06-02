package us.hall.trz.osx;import java.io.IOException;import java.nio.file.LinkOption;import java.nio.file.Path;import java.util.HashMap;import java.util.Map;/** * nio.2 AttributeView implementor for Mac Launch Services attributes *  * @author Michael Hall * */class LS implements MacLSFileAttributeView {	static final String CREATOR = "creator";	static final String TYPE = "type";	static final String PLAIN = "plain";	static final String PACKAGE = "package";	static final String APPLICATION = "application";	static final String CONTAINER = "container";	static final String ALIAS = "alias";	static final String SYMLINK = "symlink";	static final String INVISIBLE = "invisible";	static final String VOLUME = "volume";	static final String DEFAULT_APP = "default_application";	static final String KIND = "kind";	static final String APPLICATIONS = "applications";		@SuppressWarnings("unused")	private Path file;	private MacLSInfo info;		LS(Path file,boolean followLInks) {		this.file = file;		info = MacAttrUtils.getLSInfo(file);	}		public final String name() { return "mac_ls"; }	public void setAttribute(String attribute, Object value,LinkOption... options) throws IOException {       throw new ReadOnlyException("LS attribute setting currently unsupported");	}	   	   public Map<String,Object> readAttributes(String[] requested,LinkOption... options)	   throws IOException	   {	      Map<String,Object> map = new HashMap<String,Object>();	      for (String request : requested) {	         final int colon = request.indexOf(":");	      			 final String name;	      			 if (colon == -1) {				name = request;			 }			 else {				String view = request.substring(0,colon);			    if (!request.substring(0, colon).equals("mac_ls"))			    	throw new IllegalArgumentException("Invalid view " + view + " not mac_ls");				name = request.substring(colon+1, request.length());			 }			 if ("*".equals(name)) {				map.putAll(readAttributes(new String[] {					"mac_ls:creator",					"mac_ls:type",					"mac_ls:plain",					"mac_ls:package",					"mac_ls:application",					"mac_ls:container",					"mac_ls:alias",					"mac_ls:symlink",					"mac_ls:invisible",					"mac_ls:volume",					"mac_ls:default_application",					"mac_ls:kind",					"mac_ls:applications"				}));			 }			 else {			    map.put(name, info.getAttribute(name));		     }		  }		      return map;	   }}