package us.hall.trz.osx;

import java.util.Map;

public class MacXAttrInfo {
	private Map<String,byte[]> map;
	
	@SuppressWarnings("unused")
	private void setXAttrMap(Map<String,byte[]> map) { this.map = map; }
	public Map<String,byte[]> getXAttrMap() { return map; }
	
	public Object getAttribute(String name) { return map.get(name); }
}
