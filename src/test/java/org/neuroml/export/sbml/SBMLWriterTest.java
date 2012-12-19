package org.neuroml.export.sbml;

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
import org.lemsml.jlemsio.util.FileUtil;

import junit.framework.TestCase;

public class SBMLWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException {

    	File exampleSrc = new File("/home/padraig/NeuroML2/NeuroML2CoreTypes");
    	File nml2DefSrc = new File("/home/padraig/NeuroML2/NeuroML2CoreTypes");
        //Note: only works with this example at the moment!!
    	
        File xml = new File(exampleSrc, "LEMS_NML2_Ex9_FN.xml");
        String tempDir = System.getProperty("user.dir") + File.separator + "src/test/resources/tmp";
        File tgtDir = new File(tempDir);
        if (!tgtDir.exists())
        	tgtDir.mkdir();
        
        File sbmlFile = new File(tgtDir,"LEMS_NML2_Ex9_FN.sbml");



        FileInclusionReader fir = new FileInclusionReader(xml);
        fir.addSearchPaths(nml2DefSrc.getAbsolutePath());
        Sim sim = new Sim(fir.read());
            
        sim.readModel();
		Lems lems = sim.getLems();

        SBMLWriter sbmlw = new SBMLWriter(lems);
        String sbml = sbmlw.getMainScript();

        System.out.println(sbml);

        System.out.println("Writing to: "+sbmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sbml, sbmlFile);

        
        assertTrue(sbmlFile.exists());
	}

}
