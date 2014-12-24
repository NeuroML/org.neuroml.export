package org.lemsml.export.vhdl.metadata;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.edlems.EDComponent;
//import org.lemsml.export.vhdl.edlems.EDExposure;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class MetadataWriter {

	public static void writeJSONDefaultParameters(EDSimulation sim, StringBuilder sb) throws IOException
	{
		EDComponent neuron = sim.neuronComponents.get(0);

		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
	
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		g.writeArrayFieldStart("default_parameters");
		writeJSONParameters(g,neuron,"");
		g.writeEndArray();
		g.writeEndObject();
		g.close();

		sb.append(sw.toString());
	}
	
	
	private static void writeJSONParameters(JsonGenerator g, EDComponent neuron, String name) throws JsonGenerationException, IOException
	{
		for(Iterator<EDParameter> i = neuron.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_param_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("value", item.value);
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
		}

		for(Iterator<EDComponent> i = neuron.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeJSONParameters(g,item,newName);
		}
	}
	
	

	public static void writeJSONDefaultReadback(EDSimulation sim, StringBuilder sb) throws IOException
	{
		EDComponent neuron = sim.neuronComponents.get(0);

		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		g.writeArrayFieldStart("default_readback");
		writeJSONReadback(g,neuron,"");
		g.writeEndArray();
		g.writeEndObject();
		g.close();

		sb.append(sw.toString());
	}
	
	
	private static void writeJSONReadback(JsonGenerator g, EDComponent neuron, String name) throws JsonGenerationException, IOException
	{
		for(Iterator<EDState> i = neuron.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_stateCURRENT_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
		}

		for(Iterator<EDComponent> i = neuron.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeJSONReadback(g,item,newName);
		}
	}
}
