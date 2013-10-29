package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

public class XppWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {
        //Note: only works with this example at the moment!!

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	
        
        ///exampleFile = new File("/home/padraig/org.neuroml.import/src/test/resources/Simple3Species_SBML.xml");

    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        //System.out.println(ode);

        File odeFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}

}
