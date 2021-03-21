package com.g2forge.joint.core;

import java.nio.file.Path;
import java.util.Set;

import com.g2forge.alexandria.java.function.IConsumer1;

public interface IComponent {
	/**
	 * Get the paths of the files and directories which are used as input to this component, if any.
	 * 
	 * @return The input paths of this component.
	 */
	public Set<Path> getInputs();

	public Set<Path> getOutputs();

	public void map(IConsumer1<? super IConversion> consumer);
}
