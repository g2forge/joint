package com.g2forge.joint.md.mylyncommonmark;

import org.junit.Test;

import com.g2forge.joint.md.ATestMDConverter;
import com.g2forge.joint.md.IMDConverter;

public class TestMylynCommonmarkConverter extends ATestMDConverter {
	@Override
	protected IMDConverter getConverter() {
		return new MylynCommonmarkConverter();
	}

	@Test(/* Ensure junit finds this class. */)
	public void doc0() {
		super.doc0();
	}
}
