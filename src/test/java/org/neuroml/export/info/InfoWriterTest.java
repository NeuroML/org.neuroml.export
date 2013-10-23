
package org.neuroml.export.info;

import java.io.IOException;
import java.util.LinkedHashMap;
import javax.xml.bind.JAXBException;

import junit.framework.TestCase;


import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.Main;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;

/**
 * @author pgleeson
 *
 */
public class InfoWriterTest extends TestCase
{

	public void testAbstract() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException
	{
		generateBasicInfo("NML2_AbstractCells.nml");
	}

	public void testCell() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException
	{
		generateBasicInfo("NML2_FullCell.nml");
	}

	public void testIonChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException
	{
		generateBasicInfo("NML2_SimpleIonChannel.nml");
	}

	public void testNetwork() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException
	{
		generateBasicInfo("NML2_InstanceBasedNetwork.nml");
	}
	

	public void generateBasicInfo(String nmlFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {  

		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+nmlFilename);
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);

        InfoWriter infow = new InfoWriter(nmlDocument);
        
        LinkedHashMap<String, Object> list = infow.getProperties();
        
        assertTrue(!list.isEmpty());

        String info = infow.getMainScript();

        System.out.println("---------------\nInfo for "+nmlFilename+":\n"+info);
        
        
        assertTrue(info.length()>10);
		
	}
		
	
			
	


}
