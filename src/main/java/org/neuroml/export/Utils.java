package org.neuroml.export;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.sim.ParseException;
import org.lemsml.jlems.sim.Sim;
import org.lemsml.jlems.type.BuildException;
import org.lemsml.jlems.type.Lems;
import org.lemsml.jlems.xml.XMLException;
import org.lemsml.jlemsio.reader.FileInclusionReader;
import org.lemsml.jlems.logging.E;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.*;

public class Utils {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

    public static File getNeuroML2CompTypeDefDir()
    {
    	// TODO: get this from jar
    	return new File("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes");
    }
	
	public static Lems loadLemsFile(File lemsFile) throws ContentError, ParseError, ParseException, BuildException, XMLException {
		FileInclusionReader fir = new FileInclusionReader(lemsFile);
        fir.addSearchPaths(getNeuroML2CompTypeDefDir().getAbsolutePath());
        Sim sim = new Sim(fir.read());
            
        sim.readModel();
		return sim.getLems();
		
	}
	
	public static void testValidity(File xmlFile, String xsdFile) throws SAXException, IOException {
        E.info("Testing validity of: "+ xmlFile.getAbsolutePath()+" against: "+ xsdFile);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);


        Source schemaFileSource = new StreamSource(xsdFile);
        Schema schema = factory.newSchema(schemaFileSource);

        Validator validator = schema.newValidator();

        Source xmlFileSource = new StreamSource(xmlFile);

        validator.validate(xmlFileSource);

        E.info("File: "+ xmlFile.getAbsolutePath()+" is valid!!");
	}

}
