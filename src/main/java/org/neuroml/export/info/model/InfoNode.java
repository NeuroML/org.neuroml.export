/**
 *  
 */
package org.neuroml.export.info.model;

import java.util.LinkedHashMap;
import java.util.Map;



/**
 * @author borismarin
 * 
 */
public class InfoNode
{

	private Map<String, Object> _properties = new LinkedHashMap<String, Object>();

	/**
	 * @param key
	 * @param val
	 */
	public void put(String key, Object val)
	{
		_properties.put(key, val);
	}

	/**
	 * @param key
	 * @return
	 */
	public Object get(String key)
	{
		return _properties.get(key);
	}

	/**
	 * 
	 */
	public boolean isEmpty()
	{
		return _properties.isEmpty();
	}

	/**
	 * @param indent
	 * @return
	 */
	public String toTreeString(String indent)
	{

		StringBuilder main = new StringBuilder();
		for(String key : _properties.keySet())
		{
			Object obj = _properties.get(key);
			if(obj instanceof InfoNode)
			{
				main.append(indent + key + ":\n");
				main.append(((InfoNode) obj).toTreeString(indent + "\t"));
			}
			else
			{
				main.append(indent + key + ": " + obj + "\n");
			}
		}
		return main.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return toTreeString("").trim();
	}

}
