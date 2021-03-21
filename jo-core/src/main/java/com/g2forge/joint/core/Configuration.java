package com.g2forge.joint.core;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class Configuration {
	@Singular
	protected final List<IComponent> components;
}
