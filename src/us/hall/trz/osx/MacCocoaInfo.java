package us.hall.trz.osx;

import java.util.Map;

public class MacCocoaInfo {
	private Map<String,Object> map;
	
	@SuppressWarnings("unused")
	private void setMap(Map<String,Object> map) { this.map = map; }
	public Map<String,Object> getMap() { return map; }
	
	public Object getAttribute(String name) { return map.get(name); }
}
