package us.hall.trz.osx.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Test;

public class MacAttributesTest {
	@Test
	public void testSetup() {
		try {
		   File f = new File("build.xml");
		   Path p = f.toPath();
		   Path pfull = new File(System.getProperty("user.dir"),"build.xml").toPath();
		   // General setup tests
		   assertEquals("correct filesystem class",us.hall.trz.osx.MacFileSystem.class,FileSystems.getDefault().getClass());
		   assertEquals("correct provider class",us.hall.trz.osx.MacFileSystemProvider.class,FileSystems.getDefault().provider().getClass());
		   assertEquals("correct path class",us.hall.trz.osx.MacPath.class,FileSystems.getDefault().getPath("build.xml").getClass());
		   assertTrue("path equality match",p.equals(Paths.get("build.xml")));
		   assertTrue("path ends with string correctly",pfull.endsWith("build.xml"));
		   assertTrue("path ends with path correctly",pfull.endsWith(p));
           BasicFileAttributes attrs =
               Files.readAttributes(p, BasicFileAttributes.class);
           assertTrue("basic attr regular file works",attrs.isRegularFile());
		}
		catch (Throwable tossed) { fail(tossed.toString()); }	
	}
	
	@Test
	public void testFinder() {
		try {
		   File f = new File("build.xml");
		   Path p = f.toPath();
		   // Finder meta tests
		   // Make sure we start from a known state for type/creator
		   Files.setAttribute(p, "mac_finder:type","TEXT");
		   Files.setAttribute(p, "mac_finder:creator","ttxt");
		   assertEquals("ttxt",Files.getAttribute(p,"mac_finder:creator"));
		   assertEquals("TEXT",Files.getAttribute(p,"mac_finder:type"));
		   Files.setAttribute(p, "mac_finder:type","TEXT");
		   Files.setAttribute(p, "mac_finder:creator", "sfri");
		   assertEquals("sfri",Files.getAttribute(p,"mac_finder:creator"));
		   Files.setAttribute(p, "mac_finder:creator", "ttxt");
		   Files.setAttribute(p, "mac_finder:label", "orange");
		   assertEquals("orange",Files.getAttribute(p, "mac_finder:label").toString());
		   Files.setAttribute(p, "mac_finder:label", "default");
		   assertEquals("default",Files.getAttribute(p,"mac_finder:label").toString());
		   Files.setAttribute(p, "mac_finder:stationery","true");
		   assertEquals("true",Files.getAttribute(p,"mac_finder:stationery").toString());
		   Map<String,Object> attrs = Files.readAttributes(p,"mac_finder:*");
		   assertEquals(9,attrs.size());
		   assertEquals("ttxt",attrs.get("creator"));
		   attrs = Files.readAttributes(p,"mac_finder:creator,mac_finder:type,mac_finder:label");
		   assertEquals(3,attrs.size());
		   attrs = Files.readAttributes(p,"mac_xattr:*");
		}
		catch (IOException ioex) { fail(ioex.toString()); }		
	}
	
	@Test
	public void testLS() {
	   try {
	      File f = new File("build.xml");
	      Path p = f.toPath();
		  // Make sure we start from a known state for type/creator
		  Files.setAttribute(p, "mac_finder:type","TEXT");
		  Files.setAttribute(p, "mac_finder:creator","ttxt");
		  // LS meta tests
		  assertEquals("ttxt",Files.getAttribute(p,"mac_ls:creator"));
		  assertEquals("true",Files.getAttribute(p,"mac_ls:plain").toString());
		  if (!((Boolean)Files.getAttribute(p,"mac_ls:application")).booleanValue()) {
		     Path defApp = (Path)Files.getAttribute(p,"mac_ls:default_application");
		     assertEquals("true",Files.getAttribute(defApp,"mac_ls:application").toString());
		  }
		  else fail("build.xml not an application");
		  assertEquals("XML text",Files.getAttribute(p,"mac_ls:kind"));
		  Path[] apps = (Path[])Files.getAttribute(p,"mac_ls:applications");
		  assertTrue(apps.length > 0);
		  assertTrue(apps[0].getFileName().toString().endsWith(".app"));
		  Map<String,Object> attrs = Files.readAttributes(p,"mac_ls:*");
		  assertEquals(13,attrs.size());
		  assertEquals("ttxt",attrs.get("creator"));
		  attrs = Files.readAttributes(p,"mac_ls:creator,mac_ls:plain,mac_ls:kind");
		  assertEquals(3,attrs.size());
	   }
	   catch (IOException ioex) { fail(ioex.toString()); }
	}
	
	@Test
	public void testXAttr() {
		try {
			File f = new File("build.xml");
			Path p = f.toPath();	
			Map<String,Object> attrs = Files.readAttributes(p,"mac_xattr:*");
			assertEquals(2,attrs.size());
			assertTrue(attrs.containsKey("mac_xattr:com.apple.FinderInfo"));
			byte[] finfo = (byte[])attrs.get("mac_xattr:com.apple.FinderInfo");
			assertEquals(32,finfo.length);
		}
		catch (IOException ioex) { fail(ioex.toString()); }
	}
	
	@Test
	public void testCocoa() {
		try {
			File f = new File("build.xml");
			Path p = f.toPath();
			Map<String,Object> attrs = Files.readAttributes(p,"mac_cocoa:*");
			assertEquals(17,attrs.size());
			assertFalse(((Boolean)Files.getAttribute(p,"mac_cocoa:NSFileBusy")).booleanValue());
			assertEquals(Files.size(p),((Long)Files.getAttribute(p,"mac_cocoa:NSFileSize")).longValue());
			attrs = Files.readAttributes(p,"mac_cocoa:NSFileModificationDate,mac_cocoa:NSFilePosixPermissions");
			assertEquals(2,attrs.size());		
		}
		catch (IOException ioex) { fail(ioex.toString()); }
	}
}
