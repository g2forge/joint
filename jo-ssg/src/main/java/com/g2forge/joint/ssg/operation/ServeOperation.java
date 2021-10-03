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
import com.g2forge.joint.core.IConversionContext.Mode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServeOperation implements IOperation {
	protected static ICloseable startScanning(Configuration configuration, OperationInstance operationInstance) {
		final OperationInstance[] context = new OperationInstance[] { operationInstance };
		final FileScanner scanner = new FileScanner(event -> {
			// Invoke the conversions with the correct mode based on the file scanner event
			final Mode mode = event.isScan() ? IConversionContext.Mode.ServeBuild : IConversionContext.Mode.ServeRebuild;
			final Set<ConversionInstance> conversions = event.getPaths().stream().map(context[0].getConversionsByInput()::get).filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet());
			for (ConversionInstance conversion : conversions) {
				conversion.invoke(context[0], mode);
			}

			if (!event.isScan()) for (IComponent component : configuration.getComponents()) {
				// If any of the changed paths starts with any of the components input paths
				final boolean rescan = component.getInputs().stream().filter(i -> event.getPaths().stream().filter(p -> p.startsWith(i)).findAny().isPresent()).findAny().isPresent();
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
		}, throwable -> log.error("Exception while handling changes", throwable), true, false, operationInstance.getInputs());
		scanner.open();
		return () -> {
			HIO.closeAll(() -> {
				scanner.close();
				scanner.waitClosed();
			}, operationInstance);
		};
	}

	@Override
	public ICloseable invoke(Configuration configuration) {
		// Create the operation instance, and close it ONLY ON FAILURE (otherwise the caller will close it through our return value)
		final OperationInstance operationInstance = OperationInstance.builder().configuration(configuration).build();
		for (ConversionInstance conversion : operationInstance.getConversionsWithoutInput()) {
			conversion.invoke(operationInstance, IConversionContext.Mode.Build);
		}
		try {
			return startScanning(configuration, operationInstance);
		} catch (Throwable throwable) {
			operationInstance.close();
			throw throwable;
		}
	}
}
