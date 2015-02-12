
package org.lemsml.export.dlems;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;

import junit.framework.TestCase;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.exception.GenerationException;
import org.neuroml.export.exception.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author matteocantarelli
 *
 */
public class DLemsWriterTest extends TestCase
{

	public void testIzhikevich() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException 
	{
		generateMainScript("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes/LEMS_NML2_Ex2_Izh.xml",
				         "./src/test/resources/tmp/izhikevich.json");
	}

	public void testHH() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException
	{
		generateMainScript("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes/LEMS_NML2_Ex1_HH.xml",
				           "./src/test/resources/tmp/LEMS_NML2_Ex1_HH.json");
	}

	public void testFN() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException
	{
		generateMainScript("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes/LEMS_NML2_Ex9_FN.xml",
				           "./src/test/resources/tmp/LEMS_NML2_Ex9_FN.json");
	}
	

	public void generateMainScript(String lemsFilename, String dlemsFileName) throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException {
		String exampleFilename = lemsFilename.substring(lemsFilename.lastIndexOf('/')+1);
        System.out.println("Loading: "+exampleFilename);
        
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
    	
        DLemsWriter sw = new DLemsWriter(lems);

        String dlems = sw.getMainScript();


        File dlemsFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".json"));
        System.out.println(dlems);
        System.out.println("Writing to: "+dlemsFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(dlems, dlemsFile);

        
        assertTrue(dlemsFile.exists());
		
	}
		
	
	public void testVelocity() throws LEMSException, GenerationException, IOException {
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		context.put( "name", new String("VelocityOnOSB") );
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("s1", "v1");
		map.put("s2", "v2");
		map.put("s3", "v3");
		map.put("s4", "v4");
		E.info("Map: "+map);
		context.put( "state", map);
		
		Template template = null;
		
		template = Velocity.getTemplate("./src/test/resources/mytemplate.vm");
	
		StringWriter sw = new StringWriter();
		
		template.merge( context, sw );
		
		System.out.println(sw);
	}
			
	

	public void generateMainScriptAPI(String lemsFilename, String dlemsFileName) throws LEMSException, GenerationException, IOException {
		
		
		/*
		LEMSDocumentReader reader = new LEMSDocumentReader();
		
		ILEMSDocument model = reader.readModel(new File(lemsFilename));
		LEMSBuilder builder=new LEMSBuilder();
		builder.addDocument(model);
		ILEMSBuildOptions options=new LEMSBuildOptions();
		ILEMSBuildConfiguration config = new LEMSBuildConfiguration(null);
		options.addBuildOption(LEMSBuildOptionsEnum.FLATTEN);
		builder.build(config, options);
		config = new LEMSBuildConfiguration(LEMSDocumentReader.getTarget(model));
		builder.build(config, options);
		
		DLemsWriter mw = new DLemsWriter((Lems)model);
        assertEquals(FileUtils.readFile(dlemsFileName), mw.getMainScript());
        */
	}

}
