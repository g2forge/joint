package com.g2forge.joint.md.flexmark;

import org.junit.Test;

import com.g2forge.joint.md.ATestMDConverter;
import com.g2forge.joint.md.IMDConverter;

public class TestFlexmarkConverter extends ATestMDConverter {
	@Test(/* Ensure junit finds this class. */)
	public void doc0() {
		super.doc0();
	}

	@Override
	protected IMDConverter getConverter() {
		return new FlexmarkConverter();
	}

	@Test
	public void toc() {
		test();
	}
}
