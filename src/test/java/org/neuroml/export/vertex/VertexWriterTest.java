package org.neuroml.export.vertex;

import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class VertexWriterTest extends TestCase
{

    public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
    {

        String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
        Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

        VertexWriter vw = new VertexWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll(".xml", ".run.m"));

        UtilsTest.checkConvertedFiles(vw.convert());
    }

}
