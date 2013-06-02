package us.hall.trz.osx;

import static us.hall.trz.osx.LS.*;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * Holds JNI obtained information for Launch Services attributes
 * 
 * @author Michael Hal
 *
 */
public class MacLSInfo {
	public static final int	kLSRequestExtension           = 0x00000001, /* safe to use from threads*/
		                    kLSRequestTypeCreator         = 0x00000002, /* safe to use from threads*/
		                    kLSRequestBasicFlagsOnly      = 0x00000004, /* all but type of application - safe to use from threads*/
		                    kLSRequestAppTypeFlags        = 0x00000008, /* NOT SAFE to use from threads*/
		                    kLSRequestAllFlags            = 0x00000010, /* NOT SAFE to use from threads*/
		                    kLSRequestIconAndKind         = 0x00000020, /* NOT SAFE to use from threads*/
		                    kLSRequestAllInfo             = 0xFFFFFFFF; /* NOT SAFE to use from threads*/
	
	public static final int kLSItemInfoIsPlainFile        = 0x00000001, /* none of the following applies*/
		                    kLSItemInfoIsPackage          = 0x00000002, /* app, doc, or bundle package*/
		                    kLSItemInfoIsApplication      = 0x00000004, /* single-file or packaged*/
		                    kLSItemInfoIsContainer        = 0x00000008, /* folder or volume*/
		                    kLSItemInfoIsAliasFile        = 0x00000010, /* 'real' alias*/
		                    kLSItemInfoIsSymlink          = 0x00000020, /* UNIX symbolic link only*/
		                    kLSItemInfoIsInvisible        = 0x00000040, /* does not include '.' files or '.hidden' entries*/
		                    kLSItemInfoIsNativeApp        = 0x00000080, /* Carbon or Cocoa native app*/
		                    kLSItemInfoIsClassicApp       = 0x00000100, /* CFM Classic app*/
		                    kLSItemInfoAppPrefersNative   = 0x00000200, /* Carbon app that prefers to be launched natively*/
		                    kLSItemInfoAppPrefersClassic  = 0x00000400, /* Carbon app that prefers to be launched in Classic*/
		                    kLSItemInfoAppIsScriptable    = 0x00000800, /* App can be scripted*/
		                    kLSItemInfoIsVolume           = 0x00001000; /* item is a volume*/
	
	private int flags,creator,type;
	private String defApp,kind;
	private Object[] apps;
	
	@SuppressWarnings("unused")
	private void setCreator(int creator) { this.creator = creator; }
	public String getCreator() { 
		byte[] b = new byte[4];
		pokeInt(b,0,creator);
		try {
			return new String(b,"UTF-8");
		}
		catch (java.io.UnsupportedEncodingException uee) { uee.printStackTrace(); }
		return null;
	}

	@SuppressWarnings("unused")
	private void setType(int type) { this.type = type; }
	public String getType() { 
		byte[] b = new byte[4];
		pokeInt(b,0,type);
		try {
			return new String(b,"UTF-8");
		}
		catch (java.io.UnsupportedEncodingException uee) { uee.printStackTrace(); }
		return null;
	}
	
	@SuppressWarnings("unused")
	private void setAppDefault(String app) { this.defApp = app; }
	public Path getAppDefault() { 
		return Paths.get(defApp); 
	}
	
	@SuppressWarnings("unused")
	private void setKind(String kind) { this.kind = kind; }
	public String getKind() { return kind; }
	
	@SuppressWarnings("unused")
	private void setApplications(Object[] apps) { this.apps = apps; }
	public Path[] getApplications() {
		Path[] papps = new Path[apps.length];
		for (int i=0; i<apps.length; i++) {
			papps[i] = Paths.get((String)apps[i]);
		}
		return papps;
	}
	
	@SuppressWarnings("unused")
	private void setFlags(int flags) { this.flags = flags; }
	public boolean isPlainFile() { return (flags & kLSItemInfoIsPlainFile) != 0; }
	public boolean isPackage() { return (flags & kLSItemInfoIsPackage) != 0; }
	public boolean isApplication() { return (flags & kLSItemInfoIsApplication) != 0; }
	public boolean isContainer() { return (flags & kLSItemInfoIsContainer) != 0; }
	public boolean isAliasFile() { return (flags & kLSItemInfoIsAliasFile) != 0; }
	public boolean isSymlink() { return (flags & kLSItemInfoIsSymlink) != 0; }
	public boolean isInvisible() { return (flags & kLSItemInfoIsInvisible) != 0; }
	public boolean isVolume() { return (flags & kLSItemInfoIsVolume) != 0; }

	public Object getAttribute(String attr) {
		if (attr.equals(CREATOR)) return getCreator();
		if (attr.equals(TYPE)) return getType();
		if (attr.equals(PLAIN)) return isPlainFile();
		if (attr.equals(PACKAGE)) return isPackage();
		if (attr.equals(APPLICATION)) return isApplication();
		if (attr.equals(CONTAINER)) return isContainer();
		if (attr.equals(ALIAS)) return isAliasFile();
		if (attr.equals(SYMLINK)) return isSymlink();
		if (attr.equals(INVISIBLE)) return isInvisible();
		if (attr.equals(VOLUME)) return isVolume();
		if (attr.equals(DEFAULT_APP)) return getAppDefault();
		if (attr.equals(KIND)) return getKind();
		if (attr.equals(APPLICATIONS)) return getApplications();
		return null;
	}
	
	private final void pokeInt(byte [] a, int index, int i)
	{
		a[index] = (byte)(i >> 24);
		a[index +1] = (byte)(i >> 16);
		a[index + 2] = (byte)(i >> 8);
		a[index + 3] = (byte)(i);
	}
}
