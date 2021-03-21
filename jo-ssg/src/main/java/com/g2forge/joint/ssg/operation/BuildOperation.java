package com.g2forge.joint.ssg.operation;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.core.IConversionContext;

public class BuildOperation implements IOperation {
	@Override
	public ICloseable invoke(Configuration configuration) {
		try (final OperationInstance operationInstance = OperationInstance.builder().configuration(configuration).build()) {
			for (ConversionInstance conversion : operationInstance.getConversions()) {
				conversion.invoke(operationInstance, IConversionContext.Mode.Build);
			}
		}

		return null;
	}
}
