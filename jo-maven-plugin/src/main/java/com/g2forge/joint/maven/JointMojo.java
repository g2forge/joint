package com.g2forge.joint.maven;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.g2forge.joint.ssg.Joint;
import com.g2forge.joint.ssg.operation.Operation;
import com.g2forge.joint.ui.UIBuildComponent;

@Mojo(name = "joint", defaultPhase = LifecyclePhase.COMPILE)
public class JointMojo extends AbstractMojo {
	protected static final String DEFAULT_OUTPUT = "gh-pages";

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(property = "joint.input", required = false, defaultValue = "")
	String input;

	@Parameter(property = "joint.output", required = false, defaultValue = DEFAULT_OUTPUT)
	String output;

	@Parameter(property = "joint.operation", required = false, defaultValue = "Build")
	String operation;

	@Parameter(property = "joint.basehref", required = false)
	String baseHref;

	@Parameter(property = "joint.404", required = false, defaultValue = "")
	String notFoundHandlers;

	@Parameter(property = "joint.maps", required = false, defaultValue = "false")
	boolean maps;

	@Parameter(property = "joint.servePort", required = false, defaultValue = "")
	String servePort;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Path base = project.getBasedir().toPath();
		final Joint.JointBuilder builder = Joint.builder();

		builder.input(input == null ? base : base.resolve(input));
		builder.output(output == null ? base.resolve(DEFAULT_OUTPUT) : base.resolve(output));

		//  Pass in the parameters
		if (operation != null) builder.operation(Operation.valueOf(operation));
		if (baseHref != null) builder.baseHref(baseHref);
		if (notFoundHandlers != null) builder.notFoundHandlers(Stream.of(notFoundHandlers.split(",")).map(String::trim).map(UIBuildComponent.NotFoundHandler::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(UIBuildComponent.NotFoundHandler.class))));
		else builder.notFoundHandlers(EnumSet.allOf(UIBuildComponent.NotFoundHandler.class));
		builder.maps(maps);
		if ((servePort != null) && !servePort.isBlank()) builder.servePort(Integer.parseInt(servePort.trim()));

		// Run joint
		final Joint joint = builder.build();
		try {
			joint.call();
		} catch (Exception exception) {
			throw new MojoFailureException("Joint static site generation failed", exception);
		}
	}
}