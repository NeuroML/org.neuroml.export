package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;

import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Main;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.xml.sax.SAXException;

public class SVGWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, JAXBException, GenerationException {

    	String exampleFilename = "NML2_FullCell.nml";    

		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+exampleFilename);
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);

    	SVGWriter svgw = new SVGWriter(nmlDocument, exampleFilename);
        String svg = svgw.getMainScript();

        System.out.println(svg);

        File svgFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("nml", "svg"));
        System.out.println("Writing file to: "+svgFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(svg, svgFile);

        assertTrue(svgFile.exists());
        
	}
}
