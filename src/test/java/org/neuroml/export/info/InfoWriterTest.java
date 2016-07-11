
package org.neuroml.export.info;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.info.model.InfoNode;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.Cell;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;

/**
 * @author pgleeson
 *
 */
public class InfoWriterTest extends TestCase
{

	public void testAbstract() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		generateBasicInfo("NML2_AbstractCells.nml");
	}

	public void testIonChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		generateBasicInfo("NML2_SimpleIonChannel.nml");
	}

	public void testNetwork() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		generateBasicInfo("NML2_InstanceBasedNetwork.nml");
	}

	public void testHHCell() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		generateBasicInfo("NML2_SingleCompHHCell.nml");
	}
	

	public void generateBasicInfo(String nmlFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception {  

		String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/"+nmlFilename);
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);

        InfoWriter infow = new InfoWriter(nmlDocument);
        
        String info = infow.getMainScript();

        System.out.println("---------------\nInfo for "+nmlFilename+":\n"+info);
        
        assertTrue(info.length()>10);
        
        if (!nmlDocument.getCell().isEmpty()) 
        {
            Cell cell = nmlDocument.getCell().get(0);
            InfoNode infon = InfoTreeCreator.createInfoTreeFromStandalone(cell);
            System.out.println("==============\nInfo for "+cell.getId()+":\n"+infon);
        }
        
        Lems lems = Utils.readNeuroMLFile(content).getLems();
        for (Component comp: lems.getComponents())
        {
            System.out.println(" >>>>>\nInfo for "+comp.getID()+" in 'LEMS' file: "+nmlFilename+"");
            InfoNode infon = InfoTreeCreator.createInfoTreeFromComponent(comp);
            System.out.println(infon);
            
        }
		
	}
		
	
			
	


}
