package com.g2forge.joint.md.enigma;

import org.junit.Ignore;
import org.junit.Test;

import com.g2forge.joint.md.ATestMDConverter;
import com.g2forge.joint.md.IMDConverter;

public class TestEnigmaMDConverter extends ATestMDConverter {
	@Test
	@Ignore
	public void angular() {
		super.angular();
	}

	@Test
	@Ignore
	public void doc1() {
		super.doc1();
	}

	@Override
	protected IMDConverter getConverter() {
		return new EnigmaMDConverter();
	}
}
