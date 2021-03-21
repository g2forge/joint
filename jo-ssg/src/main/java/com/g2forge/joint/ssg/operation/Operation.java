package com.g2forge.joint.ssg.operation;

public enum Operation {
	Build {
		@Override
		public IOperation create() {
			return new BuildOperation();
		}
	},
	Serve {
		@Override
		public IOperation create() {
			return new ServeOperation();
		}
	};

	public abstract IOperation create();
}
