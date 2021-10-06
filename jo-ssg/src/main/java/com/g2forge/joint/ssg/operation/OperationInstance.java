package com.g2forge.joint.ssg.operation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.io.HIO;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.core.IComponent;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class OperationInstance implements ICloseable {
	public static class OperationInstanceBuilder {
		protected Set<AutoCloseable> closeables;

		public OperationInstanceBuilder closeable(final AutoCloseable closeable) {
			if (closeable != null) {
				if (closeables == null) closeables = new LinkedHashSet<>();
				closeables.add(closeable);
			}
			return this;
		}

		protected OperationInstanceBuilder component(IComponent component) {
			if (this.inputs == null) this.inputs = new ArrayList<>();
			inputs.addAll(component.getInputs());

			if (component instanceof AutoCloseable) closeable((AutoCloseable) component);
			component.map(conversion -> {
				final ConversionInstance conversionInstance = new ConversionInstance(component, conversion);

				if (conversion.getInputs().isEmpty()) {
					conversionsWithoutInput(conversionInstance);
					conversion(conversionInstance);
				} else {
					if (conversionsByInput$key == null) conversionsByInput$key = new ArrayList<>();
					if (conversionsByInput$value == null) conversionsByInput$value = new ArrayList<>();
					boolean changed = false;
					for (Path input : conversion.getInputs()) {
						int index = conversionsByInput$key.indexOf(input);
						if (index < 0) {
							index = conversionsByInput$value.size();
							conversionsByInput$key.add(input.normalize());
							conversionsByInput$value.add(new HashSet<>());
						}
						changed |= conversionsByInput$value.get(index).add(conversionInstance);
					}
					if (changed) conversion(conversionInstance);
				}
			});

			return this;
		}

		public OperationInstanceBuilder configuration(Configuration configuration) {
			configuration.getComponents().forEach(this::component);
			return this;
		}
	}

	/**
	 * Input files and directories for all components.
	 */
	@Singular
	protected final Set<Path> inputs;

	/**
	 * Map from input files and directories to the conversion instances that use them.
	 */
	@Singular("conversionByInput")
	protected final Map<Path, Set<ConversionInstance>> conversionsByInput;

	/**
	 * The set of conversions instances which depend on no inputs (e.g. the ones to build once at the start)
	 */
	@Singular("conversionsWithoutInput")
	protected final Set<ConversionInstance> conversionsWithoutInput;

	@Singular
	protected final Set<ConversionInstance> conversions;

	protected final Set<AutoCloseable> closeables;

	@Override
	public void close() {
		final Set<AutoCloseable> closeables = getCloseables();
		if (closeables != null) HIO.closeAll(closeables);
	}
}
