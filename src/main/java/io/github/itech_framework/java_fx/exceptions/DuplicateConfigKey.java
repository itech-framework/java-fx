package io.github.itech_framework.java_fx.exceptions;

public class DuplicateConfigKey extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4859057573969376075L;

	public DuplicateConfigKey(String key) {
		super(String.format("The config key [%s] already defined!", key));
	}

}
