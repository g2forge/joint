package com.g2forge.joint.ssg.operation;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.core.helpers.HCollection;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.alexandria.java.io.watch.FileScanner;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.core.IComponent;
import com.g2forge.joint.core.IConversionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServeOperation implements IOperation {
	protected static ICloseable startScanning(Configuration configuration, OperationInstance operationInstance) {
		final OperationInstance[] context = new OperationInstance[] { operationInstance };
		final FileScanner scanner = new FileScanner(changed -> {
			final Set<ConversionInstance> reconvert = changed.stream().map(context[0].getConversionsByInput()::get).filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet());
			for (ConversionInstance conversion : reconvert) {
				conversion.invoke(context[0], IConversionContext.Mode.ServeRebuild);
			}

			for (IComponent component : configuration.getComponents()) {
				// If any of the changed paths starts with any of the components input paths
				final boolean rescan = component.getInputs().stream().filter(i -> changed.stream().filter(p -> p.startsWith(i)).findAny().isPresent()).findAny().isPresent();
				if (rescan) {
					final Set<ConversionInstance> pre = context[0].getConversions();
					// Scan the component to find all the relevant conversions
					context[0] = context[0].toBuilder().component(component).build();
					// Invoke all the newly discovered conversions
					final Set<ConversionInstance> toInvoke = HCollection.difference(context[0].getConversions(), pre);
					for (ConversionInstance conversion : toInvoke) {
						conversion.invoke(operationInstance, IConversionContext.Mode.ServeBuild);
					}
				}
			}
		}, throwable -> log.error("Exception while handling changes", throwable), false, context[0].getInputs());
		scanner.open();
		return () -> {
			HIO.closeAll(scanner, operationInstance);
		};
	}

	@Override
	public ICloseable invoke(Configuration configuration) {
		final OperationInstance operationInstance = OperationInstance.builder().configuration(configuration).build();
		try {
			operationInstance.getConversions().forEach(conversion -> conversion.invoke(operationInstance, IConversionContext.Mode.ServeBuild));
			return startScanning(configuration, operationInstance);
		} catch (Throwable throwable) {
			operationInstance.close();
			throw throwable;
		}
	}
}
