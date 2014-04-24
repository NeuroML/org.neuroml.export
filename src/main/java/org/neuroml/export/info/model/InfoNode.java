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
    public static String INDENT = "    ";
    
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
	 * @return
	 */
	public Map<String, Object> getProperties()
	{
		return _properties;
		
	}
	
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
				main.append(((InfoNode) obj).toTreeString(indent + INDENT));
			}
            else if(obj instanceof PlotNode)
			{
				main.append(indent + key + ": " + ((PlotNode)obj).toShortString() + "\n");
			}
			else
			{
				main.append(indent + key + ": " + obj + "\n");
			}
		}
		return main.toString();
	}

    @Override
	public String toString()
	{
		return toTreeString("").trim();
	}

}
