package com.g2forge.joint.ssg.staticcontent;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class DirectoryConfiguration {
	@Singular("include")
	protected final List<String> include;

	@Singular("exclude")
	protected final List<String> exclude;
}
