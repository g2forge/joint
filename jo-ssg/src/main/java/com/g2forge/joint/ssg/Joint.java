package com.g2forge.joint.ssg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import com.g2forge.gearbox.command.converter.dumb.DumbCommandConverter;
import com.g2forge.gearbox.command.process.ProcessBuilderRunner;
import com.g2forge.gearbox.command.proxy.CommandProxyFactory;
import com.g2forge.gearbox.command.proxy.ICommandProxyFactory;
import com.g2forge.joint.codeowners.GHCodeOwnersComponent;
import com.g2forge.joint.core.Configuration;
import com.g2forge.joint.core.Configuration.ConfigurationBuilder;
import com.g2forge.joint.ssg.operation.Operation;
import com.g2forge.joint.ssg.staticcontent.StaticContentComponent;
import com.g2forge.joint.ui.UIBuildComponent;
import com.g2forge.joint.ui.UIFrameworkComponent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "joint", mixinStandardHelpOptions = true)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Joint extends AJointCommand {
	public enum Component {
		StaticContent,
		UI,
		GHCodeOwners;
	}

	public static final Path JOINT = Paths.get(".joint");

	public static void main(String... args) {
		System.exit(new CommandLine(new Joint()).execute(args));
	}

	@Parameters(index = "0", description = "Path to the content input directory")
	protected Path input;

	@Parameters(index = "1", description = "Path to site output directory")
	protected Path output;

	@Option(names = { "-o", "--operation" }, description = "The operation to run (default: ${DEFAULT-VALUE}, one of: ${COMPLETION-CANDIDATES})", required = false)
	@Builder.Default
	protected Operation operation = Operation.Build;

	@Option(names = { "-c", "--components" }, description = "The joint components to invoke (default: ${DEFAULT-VALUE}, zero or more of: ${COMPLETION-CANDIDATES})", required = false)
	@Builder.Default
	protected EnumSet<Component> components = EnumSet.complementOf(EnumSet.of(Component.GHCodeOwners));

	@Option(names = { "-w", "--working" }, description = "The working directory in which to build the UI (default: <INPUT>/.joint)", required = false)
	protected Path working;

	@Option(names = { "--base-href" }, description = "The base HREF to be used for the angular site (default: none)", required = false)
	@Builder.Default
	protected String baseHref = null;

	@Option(names = { "--404" }, description = "Add support for the 404 handling methods appropriate to your webserver (default: none, zero or more of: ${COMPLETION-CANDIDATES})", required = false)
	@Builder.Default
	protected EnumSet<UIBuildComponent.NotFoundHandler> notFoundHandlers = EnumSet.noneOf(UIBuildComponent.NotFoundHandler.class);

	@Option(names = { "--maps" }, description = "Enable source maps during production builds (default: false)")
	@Builder.Default
	protected boolean maps = false;
	
	@Option(names = { "--servePort" }, description = "Set a specific port for serving (ignored during build, default: use the angular default)")
	@Builder.Default
	protected Integer servePort = null;

	@Builder.Default
	protected ICommandProxyFactory commandProxyFactory = new CommandProxyFactory(DumbCommandConverter.create(), new ProcessBuilderRunner());

	protected Configuration configure() {
		this.working = (getWorking() == null) ? getInput().resolve(JOINT) : getWorking();
		final boolean isUI = getComponents().contains(Component.UI);

		final ConfigurationBuilder retVal = Configuration.builder();
		if (isUI) retVal.component(new UIFrameworkComponent(getName(), working));
		if (getComponents().contains(Component.StaticContent)) retVal.component(new StaticContentComponent(getInput(), isUI ? working.resolve("src/assets") : getOutput(), working));
		if (isUI) retVal.component(new UIBuildComponent(commandProxyFactory, working, getOutput(), true, isMaps(), getServePort(), getBaseHref(), getNotFoundHandlers()));
		if (getComponents().contains(Component.GHCodeOwners)) retVal.component(new GHCodeOwnersComponent(getInput()));
		return retVal.build();
	}

	protected String getName() {
		return getInput().getFileName().toString();
	}
}
