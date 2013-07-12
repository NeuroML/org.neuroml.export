package org.neuroml.export;


import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.model.IzhikevichCell;
import org.neuroml.model.NeuroMLDocument;

import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLElements;
import org.lemsml.jlems.core.xml.XMLElementReader;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }
    
    public void testReplaceInFunction()
    {
    	assertEquals("x + y - z", replaceExpr("a+b-c"));
    	assertEquals("x ^rr", replaceExpr("a^rr"));
    }
    
    private String replaceExpr(String oldExpr) {

    	String a = "a";
    	String b = "b";
    	String c = "c";

    	String x = "x";
    	String y = "y";
    	String z = "z";

    	String ret = Utils.replaceInExpression(oldExpr, a, x);
    	ret = Utils.replaceInExpression(ret, b, y);
    	ret = Utils.replaceInExpression(ret, c, z);
    	System.out.println("Converted {"+oldExpr+"} to {"+ret+"}");
    	
    	return ret;
    }

    public void testApp()
    {
    	NeuroMLDocument nml2 = new NeuroMLDocument();
        nml2.setId("SomeCells");

        IzhikevichCell iz1 = new IzhikevichCell();
        iz1.setId("Izh0");
        iz1.setV0("-70mV");
        iz1.setThresh("30mV");
        iz1.setA("0.02");
        iz1.setB("0.2");
        iz1.setC("-50");
        iz1.setD("2");
        /*
        iz1.setIamp("2");
        iz1.setIdel("100 ms");
        iz1.setIdur("100 ms");
        */
        nml2.getIzhikevichCell().add(iz1);
        
    	System.out.println("Completed the NeuroML test, created: "+iz1+"...");
        assertTrue( true );
        
		Dimension current = new Dimension("current");
		current.setI(1);
		Dimension current2 = new Dimension("current2");
		current2.setI(1);

    	System.out.println("Completed the LEMS test, created: "+current2+"...");
    	
		/*assertTrue("Dimensions match", current.matches(current2));*/
    }

    public void testVersions() throws IOException
    {
    	System.out.println("Running a test on version usage, making all references to versions are: v"+Main.ORG_NEUROML_EXPORT_VERSION+"...");

    	String jnmlPom = FileUtil.readStringFromFile(new File("pom.xml"));

    	XMLElementReader xer = new XMLElementReader(jnmlPom);
    	assertEquals(Main.ORG_NEUROML_EXPORT_VERSION, xer.getRootElement().getElement("version").getBody());
    	
    }


    public static Lems readLemsFileFromExamples(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError
    {
    	NeuroML2Validator nmlv = new NeuroML2Validator();
    	
		String content = JUtil.getRelativeResource(nmlv.getClass(), Main.getLemsExamplesResourcesDir()+"/"+exampleFilename);
		
		return Utils.readLemsNeuroMLFile(content).getLems();
    }
    public static Lems readNeuroMLFileFromExamples(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError
    {
    	NeuroML2Validator nmlv = new NeuroML2Validator();
    	
		String content = JUtil.getRelativeResource(nmlv.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+exampleFilename);
		
		return Utils.readLemsNeuroMLFile(content).getLems();
    }
    
    public static File getTempDir()
    {
	    String tempDirName = System.getProperty("user.dir") + File.separator + "src/test/resources/tmp";
	    File tempDir = new File(tempDirName);
	    if (!tempDir.exists())
	    	tempDir.mkdir();
	    return tempDir;
    }
    
}
