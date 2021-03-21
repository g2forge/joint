package com.g2forge.joint.codeowners;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.g2forge.alexandria.java.core.helpers.HStream;
import com.g2forge.alexandria.java.io.RuntimeIOException;
import com.g2forge.alexandria.java.io.dataaccess.PathDataSource;
import com.g2forge.gearbox.github.actions.HGHActions;
import com.g2forge.gearbox.github.codeowners.GHCOPattern;
import com.g2forge.gearbox.github.codeowners.IGHCOLine;
import com.g2forge.joint.codeowners.GHCodeOwnersComponent.IGHCOParser;
import com.g2forge.joint.md.flexmark.FlexmarkConverter;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

public class MDFrontMatterGHCOParser implements IGHCOParser {
	@Data
	@Builder(toBuilder = true)
	@RequiredArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FrontMatter {
		@Singular
		protected final List<String> owners;

		protected final String owner;
	}
	
	@Override
	public IGHCOLine parse(Path root, Path relative) {
		final String text = new FlexmarkConverter().parse(new PathDataSource(root.resolve(relative), StandardOpenOption.READ)).getFrontMatter();
		if (text == null) return null;
		final FrontMatter parsed;
		try {
			parsed = HGHActions.getMapper().readValue(text, FrontMatter.class);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		final Set<String> owners = new LinkedHashSet<>();
		if (parsed.getOwner() != null && parsed.getOwner().length() > 0) owners.add(parsed.getOwner());
		if (((parsed.getOwners() != null) && !parsed.getOwners().isEmpty())) owners.addAll(parsed.getOwners());
		if (owners.isEmpty()) return null;
		final String relativeString = HStream.toStream(relative.iterator()).map(Object::toString).collect(Collectors.joining("/"));
		return new GHCOPattern("/" + relativeString, owners.stream().collect(Collectors.toList()));
	}
}