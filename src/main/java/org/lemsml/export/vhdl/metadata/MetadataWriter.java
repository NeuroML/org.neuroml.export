package org.lemsml.export.vhdl.metadata;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
//import org.lemsml.export.vhdl.edlems.EDExposure;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;
import org.lemsml.jlems.core.type.dynamics.ConditionalDerivedVariable;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MetadataWriter {


	static int itemNumber = 1;


	private static void writeIMetaRegimes(Document doc, Element rootElement,
			String name, int length,
			int inputBusPosition, int outputBusPosition)
	{
		writeIMetaItem(doc,rootElement,name,true,false,0,
				length,inputBusPosition,outputBusPosition);	
	}
	
	private static void writeIMetaVar(Document doc, Element rootElement,
			String name, int integerPart, int length,
			int inputBusPosition, int outputBusPosition)
	{
		writeIMetaItem(doc,rootElement,name,true,true,integerPart,
				length,inputBusPosition,outputBusPosition);	
	}


	private static void writeIMetaPar(Document doc, Element rootElement,
			String name, int integerPart, int length,
			int inputBusPosition)
	{
		writeIMetaItem(doc,rootElement,name,false,true,integerPart,
				length,inputBusPosition,0);	
	}
	
	private static void writeIMetaItem(Document doc, Element rootElement,
			String name, boolean isVar, boolean isFP, int integerPart, int length,
			int inputBusPosition, int outputBusPosition)
	{
		Element item = doc.createElement("item" + itemNumber);
		if (isFP)
		{
		Element itemIntegerPart = doc.createElement("itemIntegerPart");
		itemIntegerPart.appendChild(doc.createTextNode(integerPart+""));
		item.appendChild(itemIntegerPart);		
		}
		
		int roundedLength = (length % 8) == 0 ? length : ((length / 8) + 1) *8;
		int dataType = 0;
		if (roundedLength > 32 )
			roundedLength = 64;
		switch (roundedLength)
		{
		case 8:
			dataType =  (isFP) ? 1 : 16;
			break;
		case 16:
			dataType =  (isFP) ? 2 : 32;
			break;
		case 24:
			dataType =  (isFP) ? 3 : 64;
			break;
		case 32:
			dataType =  (isFP) ? 4 : 128;
			break;
		case 64:
			dataType =  (isFP) ? 5 : 256;
			break;
		default:
			dataType = 99;
			break;
		}
		Element itemDataType = doc.createElement("datatype");
		itemDataType.appendChild(doc.createTextNode(dataType+""));
		item.appendChild(itemDataType);

		Element itemName = doc.createElement("name");
		itemName.appendChild(doc.createTextNode(name+""));
		item.appendChild(itemName);

		Element type = doc.createElement("type");
		if (isVar){
			type.appendChild(doc.createTextNode("v"));
		}else{
			type.appendChild(doc.createTextNode("p"));			
		}
		item.appendChild(type);

		Element direction = doc.createElement("direction");
		if (isVar){
			direction.appendChild(doc.createTextNode("io"));
		}else{
			direction.appendChild(doc.createTextNode("i"));			
		}
		item.appendChild(direction);

		Element inputBusAddress = doc.createElement("inputBusAddress");
		inputBusAddress.appendChild(doc.createTextNode(inputBusPosition+""));
		item.appendChild(inputBusAddress);

		if (isVar){
			Element outputBusAddress = doc.createElement("outputBusAddress");
			outputBusAddress.appendChild(doc.createTextNode(outputBusPosition+""));
			item.appendChild(outputBusAddress);
		}

		rootElement.appendChild(item);
		itemNumber++;
	}
	

	private static void writeConnectivityMap(Document doc, Element rootElement,
			EDComponent comp,String name,String parentName)
	{
		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next();
			writeIMetaPar(doc, rootElement, parentName + "_param_" + item.type +  "_"+name + item.name, 
					item.integer, item.busLength, item.inputBusPosition);
		}
		
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			writeIMetaVar(doc, rootElement, parentName + "_state_" + state.type +  "_"+name + state.name , 
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
			writeIMetaVar(doc, rootElement, parentName + "_state_" + state.type +  "_"+name + state.name , 
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
			writeIMetaVar(doc, rootElement, parentName + "_state_" + state.type +  "_"+name + state.name , 
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
			}
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeConnectivityMap(doc,rootElement,item, newName,parentName);
		}
	
	}

	public static void writeSiIMeta(EDSimulation sim, StringBuilder sb,  String neuronName) throws ParserConfigurationException, TransformerException
	{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
		// root elements
		Document doc = docBuilder.newDocument();
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				EDComponent neuron = sim.neuronComponents.get(i);
				Element rootElement = doc.createElement("neuron");
				doc.appendChild(rootElement);

				Element firstname = doc.createElement("synapticInputs");
				firstname.appendChild(doc.createTextNode("1"));
				rootElement.appendChild(firstname);
				itemNumber = 1;
				
				writeIMetaPar(doc, rootElement, "sysparam_time_timestep", -6, 17, 576);
						
				
				if (neuron.regimes.size() > 0)
				{
					writeIMetaRegimes(doc,rootElement, neuron.name + "_current_regime",
							neuron.regimes.get(0).busLength,neuron.regimes.get(0).inputBusPosition,
							neuron.regimes.get(0).outputBusPosition);
				}
				
				writeConnectivityMap(doc,rootElement,neuron,"",neuron.name);
				
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);

				StringWriter outWriter = new StringWriter();
				StreamResult result = new StreamResult( outWriter );
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.transform(source, result);
				
				sb.append(outWriter.toString());
				
				break;
			}
		}
	}
	

	private static void writeInitMetaRegimes(Document doc, Element rootElement,
			String value, int length,
			int inputBusPosition, int outputBusPosition)
	{
		writeInitMetaItem(doc,rootElement,value,true,false,0,
				length,inputBusPosition,outputBusPosition);	
	}
	
	private static void writeInitMetaVar(Document doc, Element rootElement,
			String value, int integerPart, int length,
			int inputBusPosition, int outputBusPosition)
	{
		writeInitMetaItem(doc,rootElement,value,true,true,integerPart,
				length,inputBusPosition,outputBusPosition);	
	}


	private static void writeInitMetaPar(Document doc, Element rootElement,
			String value, int integerPart, int length,
			int inputBusPosition)
	{
		writeInitMetaItem(doc,rootElement,value,false,true,integerPart,
				length,inputBusPosition,0);	
	}
	
	private static void writeInitMetaItem(Document doc, Element rootElement,
			String value, boolean isVar, boolean isFP, int integerPart, int length,
			int inputBusPosition, int outputBusPosition)
	{
		Element item = doc.createElement("command"+itemNumber);

		Element command = doc.createElement("command");
		command.appendChild(doc.createTextNode("24"));
		item.appendChild(command);

		Element device = doc.createElement("device");
		device.appendChild(doc.createTextNode("1"));
		item.appendChild(device);

		Element timestep = doc.createElement("timestep");
		timestep.appendChild(doc.createTextNode("0"));
		item.appendChild(timestep);

		Element modelID = doc.createElement("modelID");
		modelID.appendChild(doc.createTextNode("0"));
		item.appendChild(modelID);

		Element itemID = doc.createElement("itemID");
		itemID.appendChild(doc.createTextNode(itemNumber+""));
		item.appendChild(itemID);
		
		
		
		if (isFP)
		{
			Element itemIntegerPart = doc.createElement("itemIntegerPart");
			itemIntegerPart.appendChild(doc.createTextNode(integerPart+""));
			item.appendChild(itemIntegerPart);		
		}
		
		int roundedLength = (length % 8) == 0 ? length : ((length / 8) + 1) *8;
		int dataType = 0;
		if (roundedLength > 32 )
			roundedLength = 64;
		switch (roundedLength)
		{
		case 8:
			dataType =  (isFP) ? 1 : 16;
			break;
		case 16:
			dataType =  (isFP) ? 2 : 32;
			break;
		case 24:
			dataType =  (isFP) ? 3 : 64;
			break;
		case 32:
			dataType =  (isFP) ? 4 : 128;
			break;
		case 64:
			dataType =  (isFP) ? 5 : 256;
			break;
		default:
			dataType = 99;
			break;
		}
		Element itemDataType = doc.createElement("itemDatatype");
		itemDataType.appendChild(doc.createTextNode(dataType+""));
		item.appendChild(itemDataType);

		Element itemName = doc.createElement("itemValue");
		itemName.appendChild(doc.createTextNode(value+""));
		item.appendChild(itemName);


		rootElement.appendChild(item);
		itemNumber++;
	}

	private static void writeInitialisationMap(Document doc, Element rootElement,
			EDComponent comp,String name,String parentName,Map<String,Float> initialStateValues)
	{
		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next();
			writeInitMetaPar(doc, rootElement, item.value, 
					item.integer, item.busLength, item.inputBusPosition);
		}
		
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			writeInitMetaVar(doc, rootElement, 
					initialStateValues.get("neuron_model_stateCURRENT_" + state.type + "_" + name + 
							state.name).toString(),					
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
			writeInitMetaVar(doc, rootElement,
					initialStateValues.get("neuron_model_stateCURRENT_" + state.type + "_" + name + 
							state.name).toString(), 
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
			writeInitMetaVar(doc, rootElement, initialStateValues.get("neuron_model_stateCURRENT_" + state.type + "_" + name + 
					state.name).toString(),
					state.integer, state.busLength, state.inputBusPosition,state.outputBusPosition);
			}
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeInitialisationMap(doc,rootElement,item, newName,parentName,initialStateValues);
		}
	
	}

	public static void writeSiInitMeta(EDSimulation sim, 
			StringBuilder sb,  String neuronName, float timestep,
			Map<String,Float> initialStateValues) throws ParserConfigurationException, TransformerException
	{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
		// root elements
		Document doc = docBuilder.newDocument();
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				EDComponent neuron = sim.neuronComponents.get(i);
				Element rootElement = doc.createElement("commands");
				doc.appendChild(rootElement);

				itemNumber = 1;
				
				writeInitMetaPar(doc, rootElement, timestep+"", -6, 17, 576);
						
				
				if (neuron.regimes.size() > 0)
				{
					writeInitMetaRegimes(doc,rootElement, "0",
							neuron.regimes.get(0).busLength,neuron.regimes.get(0).inputBusPosition,
							neuron.regimes.get(0).outputBusPosition);
				}
				
				writeInitialisationMap(doc,rootElement,neuron,"",
						neuron.name, initialStateValues);
				
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);

				StringWriter outWriter = new StringWriter();
				StreamResult result = new StreamResult( outWriter );
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.transform(source, result);
				
				sb.append(outWriter.toString());
				
				break;
			}
		}
	}
	
	public static void writeJSONDefaultState(EDSimulation sim, StringBuilder sb, 
			Map<String,Float> initialStateValues, String neuronName) throws IOException
	{
		EDComponent neuron = sim.neuronComponents.get(0);
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				neuron = sim.neuronComponents.get(i);
				break;
			}
		}

		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
	
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		g.writeArrayFieldStart("default_states");
		writeJSONDefaultStates(g,neuron,"",initialStateValues);
		g.writeEndArray();
		g.writeEndObject();
		g.close();

		sb.append(sw.toString());
	}
	
	private static void writeJSONDefaultStates(JsonGenerator g, EDComponent neuron,
			String name,Map<String,Float> initialStateValues) throws JsonGenerationException, IOException
	{
		for(Iterator<EDState> i = neuron.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("value", 
					initialStateValues.get("neuron_model_stateCURRENT_" + item.type + "_" + name + 
							item.name));
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
		}

		for (EDDerivedVariable item : neuron.derivedvariables) {
			if (item.ExposureIsUsed || item.IsUsedForOtherDerivedVariables) {
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("value", 
					initialStateValues.get("neuron_model_stateCURRENT_" + item.type + "_" + name + 
							item.name));
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
			}
		}
		
		for (EDConditionalDerivedVariable item : neuron.conditionalderivedvariables) {
			if (item.ExposureIsUsed || item.IsUsedForOtherDerivedVariables) {
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("value", 
					initialStateValues.get("neuron_model_stateCURRENT_" + item.type + "_" + name + 
							item.name));
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
			}
		}

		for(Iterator<EDComponent> i = neuron.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeJSONDefaultStates(g,item,newName,initialStateValues);
		}
	}

	
	public static void writeJSONDefaultParameters(EDSimulation sim, StringBuilder sb, String neuronName) throws IOException
	{
		EDComponent neuron = sim.neuronComponents.get(0);
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				neuron = sim.neuronComponents.get(i);
				break;
			}
		}

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
	
	

	public static void writeJSONDefaultReadback(EDSimulation sim, StringBuilder sb, String neuronName) throws IOException
	{
		EDComponent neuron = sim.neuronComponents.get(0);
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				neuron = sim.neuronComponents.get(i);
				break;
			}
		}

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
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
		}

		for (EDDerivedVariable item : neuron.derivedvariables) {
			if (item.ExposureIsUsed || item.IsUsedForOtherDerivedVariables) {
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
			}
		}
		
		for (EDConditionalDerivedVariable item : neuron.conditionalderivedvariables) {
			if (item.ExposureIsUsed || item.IsUsedForOtherDerivedVariables) {
			g.writeStartObject();
			g.writeObjectField("name","neuron_model_state_" + item.type + "_" + name + item.name);
			g.writeObjectField("type", item.type);
			g.writeObjectField("integer", item.integer);
			g.writeObjectField("fraction", item.fraction);
			g.writeEndObject();
			}
		}

		for(Iterator<EDComponent> i = neuron.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeJSONReadback(g,item,newName);
		}
	}
}
