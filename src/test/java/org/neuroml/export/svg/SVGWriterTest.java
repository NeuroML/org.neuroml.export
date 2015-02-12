package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;


import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.utils.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class SVGWriterTest extends TestCase {

	public void testGetMainScript() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException {

    	String exampleFilename = "NML2_FullCell.nml";    

		String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/"+exampleFilename);
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
