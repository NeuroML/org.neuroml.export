package org.lemsml.export.base;

import java.io.File;
import java.util.List;

import org.lemsml.jlems.core.type.Lems;

public interface IBaseWriter
{

	List<File> convert(Lems lems);

	Boolean isConversionSupported(Lems lems);

}
