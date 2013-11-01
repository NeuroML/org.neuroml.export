/**
 *  
 */
package org.neuroml.export.info.model;

import java.util.LinkedHashMap;

public class InfoNode {
	
private LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();


public void put(String key, Object val) {
	properties.put(key, val);
}

public Object get(String key) {
	return properties.get(key);
}

public LinkedHashMap<String, Object> getHashMap() {
	return properties;
}


}
