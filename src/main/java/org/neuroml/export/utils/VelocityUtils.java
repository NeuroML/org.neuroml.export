package org.neuroml.export.utils;

import java.util.Properties;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.NullLogChute;

public class VelocityUtils
{

	public static void initializeVelocity(){
		Properties props = new Properties();
		//FIXME: This line removes any log system in velocity. We need this for Geppetto but eventually a proper log system is needed 
		props.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());                        
		Velocity.init(props);
	}
	
	public static VelocityEngine getVelocityEngine(){
		Properties propsEngine = new Properties();
        propsEngine.put("resource.loader", "classpath");
        propsEngine.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		//FIXME: This line removes any log system in velocity. We need this for Geppetto but eventually a proper log system is needed
        propsEngine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());         
        VelocityEngine ve = new VelocityEngine();
        ve.init(propsEngine);
        return ve;
	}
}
