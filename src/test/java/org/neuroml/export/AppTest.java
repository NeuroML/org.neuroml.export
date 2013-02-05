package org.neuroml.export;


import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.lemsml.jlems.type.Dimension;
import org.neuroml.model.IzhikevichCell;
import org.neuroml.model.Neuroml;

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

    public void testApp()
    {
    	Neuroml nml2 = new Neuroml();
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
    
    
    /*
     * There probably is a better place for these...
     */    

    public static File getLemsExamplesDir()
    {
        return new File("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes");
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
