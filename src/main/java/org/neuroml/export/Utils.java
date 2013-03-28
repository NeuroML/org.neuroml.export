package org.neuroml.export;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.reader.FileInclusionReader;
import org.lemsml.jlems.core.logging.E;
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

	public static Sim loadLemsFileToSim(File lemsFile) throws ContentError, ParseError, ParseException, BuildException, XMLException {
		FileInclusionReader fir = new FileInclusionReader(lemsFile);
        fir.addSearchPaths(getNeuroML2CompTypeDefDir().getAbsolutePath());
        Sim sim = new Sim(fir.read());
            
        sim.readModel();
		return sim;
		
	}
	
	public static Lems loadLemsFile(File lemsFile) throws ContentError, ParseError, ParseException, BuildException, XMLException {
		Sim sim = loadLemsFileToSim(lemsFile);
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
	
    public static String convertNeuroML2ToLems(String nml2string)
    {
        if (nml2string.startsWith("<?xml")) {
            int index = nml2string.indexOf(">");
            nml2string = nml2string.substring(index + 1).trim();
        }
        
        if (nml2string.startsWith("<neuroml")) {

            int index = nml2string.indexOf(">");
            nml2string = nml2string.substring(index + 1);
            // Assume </neuroml> at end...
            nml2string = nml2string.replace("</neuroml>", "");

            nml2string = "<Lems>\n\n"
                    + "    <Include file=\"NeuroML2CoreTypes/NeuroMLCoreDimensions.xml\"/>\n"
                    + "    <Include file=\"NeuroML2CoreTypes/Cells.xml\"/>\n"
                    + "    <Include file=\"NeuroML2CoreTypes/Networks.xml\"/>\n"
                    + "    <Include file=\"NeuroML2CoreTypes/Simulation.xml\"/>\n\n"
                    + nml2string + "\n"
                    + "</Lems>";


        }
        return nml2string;
    }
    

    public static String replaceInFunction(String expr, String oldVar, String newVar) {
        String orig = new String(expr);

        if (expr.trim().equals(oldVar)) {
            return newVar;
        }

        //String new_ = toReplace.get(old);
        String[] pres = new String[]{"\\(", "\\+", "-", "\\*", "/", "\\^", " ", "<", ">"};
        String[] posts = new String[]{"\\)", "\\+", "-", "\\*", "/", "\\^", " ", "<", ">"};

        for (String pre : pres) {
            for (String post : posts) {

                String o = pre + oldVar + post;
                String n = pre + " " + newVar + " " + post;
                //E.info("Replacing "+o+" with "+n+": "+expr);
                expr = expr.replaceAll(o, n);
            }
        }
        expr = expr.trim();

        for (String pre : pres) {
            String o = pre + oldVar;
            String n = pre + " " + newVar;
            if (expr.endsWith(o)) {
                expr = expr.substring(0, expr.length() - o.length()) + n;
            }
        }
        for (String post : posts) {
            String o = oldVar + post;
            String n = newVar + " " + post;
            if (expr.startsWith(o)) {
                expr = n + expr.substring(o.length());
            }
        }

        expr = expr.replaceAll("  ", " ");

        if (!expr.equals(orig)) {
            //E.info("----------  Changed "+orig+" to "+ expr);
        }
        return expr;
    }

}
