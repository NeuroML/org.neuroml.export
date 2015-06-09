package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class SVGWriterTest extends TestCase
{

    public void testRS() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException
    {
        convertExample("L23PyrRS.nml");
    }
    public void testL5() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException
    {
        convertExample("L5PC.cell.nml");
    }
    public void testSCell() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException
    {
        convertExample("ShapedCell.cell.nml");
    }

    public void convertExample(String exampleFilename) throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException
    {

        String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);
        NeuroMLConverter nmlc = new NeuroMLConverter();
        NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);

        SVGWriter sw = new SVGWriter(nmlDocument, new File(AppTest.getTempDir(), "../"+Utils.NEUROML_EXAMPLES_RESOURCES_DIR), exampleFilename.replaceAll("nml", "svg"));

        UtilsTest.checkConvertedFiles(sw.convert());

    }
}
