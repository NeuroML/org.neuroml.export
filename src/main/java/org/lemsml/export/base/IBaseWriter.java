package org.lemsml.export.base;

import java.io.File;
import java.util.List;

public interface IBaseWriter
{

	List<File> convert();

	Boolean isConversionSupported();

}
