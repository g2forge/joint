package com.g2forge.joint.ssg.operation;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversion;
import com.g2forge.joint.core.IConversionContext;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class ConversionInstance {
	protected final IComponent component;

	protected final IConversion conversion;

	public void invoke(OperationInstance operationInstance, IConversionContext.Mode mode) {
		getConversion().invoke(new IConversionContext() {
			@Override
			public Set<IConversion> getConversions(Path input) {
				final Set<ConversionInstance> conversions = operationInstance.getConversionsByInput().get(input);
				if (conversions == null) return HCollection.emptySet();
				return conversions.stream().filter(c -> c.getComponent() == getComponent()).map(ConversionInstance::getConversion).collect(Collectors.toSet());
			}

			@Override
			public Mode getMode() {
				return mode;
			}

			@Override
			public void register(AutoCloseable closeable) {
				operationInstance.getCloseables().add(closeable);
			}
		});
	}
}