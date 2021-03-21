package com.g2forge.joint.ssg.debug;

import java.nio.file.Path;

import com.g2forge.alexandria.java.close.ICloseable;
import com.g2forge.alexandria.java.close.ICloseableSupplier;
import com.g2forge.alexandria.java.core.resource.Resource;
import com.g2forge.alexandria.java.project.HProject;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.core.Configuration.ConfigurationBuilder;
import com.g2forge.joint.ssg.AJointCommand;
import com.g2forge.joint.ssg.operation.Operation;
import com.g2forge.joint.ssg.staticcontent.StaticContentComponent;
import com.g2forge.joint.ui.UIBuildComponent;
import com.g2forge.joint.ui.UIFrameworkComponent;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class DebugUI extends AJointCommand {
	protected static class DebugContentComponent extends StaticContentComponent implements ICloseable {
		@Getter(AccessLevel.PROTECTED)
		@EqualsAndHashCode.Exclude
		@ToString.Exclude
		protected final ICloseableSupplier<Path> inputCloseable = new Resource(DebugUI.class, "content").getPath();

		public DebugContentComponent(Path output, Path working) {
			super(null, output, working);
		}

		@Override
		public void close() {
			getInputCloseable().close();
		}

		@Override
		public Path getInput() {
			return getInputCloseable().get();
		}
	}

	public static void main(String... args) throws Exception {
		System.exit(new DebugUI().call());
	}

	protected Configuration configure() {
		final Path root = HProject.getLocation(UIFrameworkComponent.class).getProject().getRoot();
		final ConfigurationBuilder retVal = Configuration.builder();
		retVal.component(new DebugContentComponent(root.resolve("src/assets"), root));
		retVal.component(UIBuildComponent.builder().working(root).output(null).initialize(false).build());
		return retVal.build();
	}

	@Override
	protected Operation getOperation() {
		return Operation.Serve;
	}
}
