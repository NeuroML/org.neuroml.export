package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.sim.ParseException;
import org.lemsml.jlems.type.BuildException;
import org.lemsml.jlems.type.Lems;
import org.lemsml.jlems.xml.XMLException;
import org.lemsml.jlemsio.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

import junit.framework.TestCase;

public class XppWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException {
        //Note: only works with this example at the moment!!

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	
        File exampleFile = new File(AppTest.getLemsExamplesDir(), exampleFilename);
        
		Lems lems = Utils.loadLemsFile(exampleFile);

		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        //System.out.println(ode);

        File odeFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}

}
