package com.g2forge.joint.core;

import java.nio.file.Path;
import java.util.Set;

public interface IConversion {
	/**
	 * Get the paths of the files referenced.
	 * 
	 * @return The paths the files referenced.
	 */
	public Set<Path> getInputs();

	/**
	 * Get the relative path in the output directory of the file generated.
	 * 
	 * @return The relative path in the output directory of the file generated.
	 */
	public Set<Path> getOutputs();

	/**
	 * Convert the inputs to the outputs.
	 * 
	 * @oaram context The context in which the conversion is happening.
	 */
	public void invoke(IConversionContext context);
}
