package com.g2forge.joint.core.copy;

import java.io.IOException;
import java.nio.file.Path;

import com.g2forge.joint.core.IConversionContext;

public interface IFileConversionType {
	public Path computeOutputRelative(FileConversion conversion);

	public void convert(IConversionContext context, FileConversion conversion, Path input, Path output) throws IOException;
}
