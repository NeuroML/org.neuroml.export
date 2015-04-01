package org.neuroml.export.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.NullLogChute;

public class VelocityUtils
{
	public static final String neuronCellTemplateFile = "/neuron/cell.vm";
	
	public static final String modelicaClassTemplateFile = "/modelica/main_class.vm";
	public static final String modelicaRunTemplateFile = "/modelica/run.vm";
	
	// public static final String sbmlTemplateFile = "/sbml/template.sbml";
	
	public static final String nestRunTemplateFile = "/nest/run.vm";
	public static final String nestCellTemplateFile = "/nest/cell.vm";
	
	public static final String pynnRunTemplateFile = "/pynn/run.vm";
	public static final String pynnCellTemplateFile = "/pynn/cell.vm";
	
	public static final String cTemplateFile = "/cvode/cvode.vm";
	public static final String makeFile = "cvode/Makefile";
	
	public static final String matlabOdeFile = "/matlab/matlab_ode.vm";
	public static final String matlabEulerFile = "/matlab/matlab_euler.vm";
	
	public static final String dnsimMainFile = "/dnsim/dnsim.m.vm";
	public static final String dnsimModuleFile = "/dnsim/dnsim.txt.vm";
	
	public static final String xppTemplateFile = "/xpp/xpp.vm";
	
	
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
	
	public static Reader getTemplateAsReader(String path){
		InputStream inputStream = VelocityUtils.class.getResourceAsStream(path);
        Reader reader = new InputStreamReader(inputStream);
        return reader;
        
	}
}
