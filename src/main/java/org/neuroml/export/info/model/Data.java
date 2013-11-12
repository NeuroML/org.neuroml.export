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
package org.neuroml.export.info.model;

import java.util.List;

/**
 * @author matteocantarelli
 *
 */
public class Data
{

	@Override
	public String toString()
	{
		return "Data [X=" + _xData + ", Y=" + _yData + ", Label=" + _label + "]";
	}

	private List<Float> _xData;
	private List<Float> _yData;
	private String _label;
	
	/**
	 * @param x
	 * @param y
	 * @param label
	 */
	public Data(List<Float> x, List<Float> y, String label)
	{
		_xData=x;
		_yData=y;
		_label=label;
	}

	/**
	 * @return the _xData
	 */
	public List<Float> getXData()
	{
		return _xData;
	}

	/**
	 * @return the _yData
	 */
	public List<Float> getYData()
	{
		return _yData;
	}

	/**
	 * @return the _label
	 */
	public String getLabel()
	{
		return _label;
	}


}
