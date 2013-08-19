/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011, 2013 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.lemsml.export.som;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.export.utils.FileUtils;
import org.lemsml.jlems.core.api.LEMSBuildConfiguration;
import org.lemsml.jlems.core.api.LEMSBuildException;
import org.lemsml.jlems.core.api.LEMSBuildOptions;
import org.lemsml.jlems.core.api.LEMSBuildOptionsEnum;
import org.lemsml.jlems.core.api.LEMSBuilder;
import org.lemsml.jlems.core.api.LEMSDocumentReader;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildOptions;
import org.lemsml.jlems.core.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;

/**
 * @author matteocantarelli
 *
 */
public class SOMWriterTest extends TestCase
{

	public void testIzhikevich() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError
	{
		//generateMainScript("./src/test/resources/izhikevich.xml","./src/test/resources/izhikevich.json");
	}
	
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, LEMSBuildException
	{
		generateMainScript("../org.neuroml.model/src/main/resources/NeuroML2CoreTypes/LEMS_NML2_Ex9_FN.xml","./src/test/resources/LEMS_NML2_Ex9_FN.json");
	}

	public void generateMainScript(String lemsFilename, String somFileName) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, LEMSBuildException {

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
		
		SOMWriter mw = new SOMWriter((Lems)model);
        assertEquals(FileUtils.readFile(somFileName), mw.getMainScript());
	}

}
