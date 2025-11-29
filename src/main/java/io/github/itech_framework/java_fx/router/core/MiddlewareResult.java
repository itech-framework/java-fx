package io.github.itech_framework.java_fx.router.core;

public class MiddlewareResult {
	private final boolean shouldProceed;
	private final String redirectTo;
	private final Object redirectArguments;
	private final Runnable onFailureCallback;

	private MiddlewareResult(boolean shouldProceed, String redirectTo, Object redirectArguments,
			Runnable onFailureCallback) {
		this.shouldProceed = shouldProceed;
		this.redirectTo = redirectTo;
		this.redirectArguments = redirectArguments;
		this.onFailureCallback = onFailureCallback;
	}

	public static MiddlewareResult proceed() {
		return new MiddlewareResult(true, null, null, null);
	}

	public static MiddlewareResult abort() {
		return new MiddlewareResult(false, null, null, null);
	}

	public static MiddlewareResult redirect(String route) {
		return new MiddlewareResult(false, route, null, null);
	}

	public static MiddlewareResult redirect(String route, Object arguments) {
		return new MiddlewareResult(false, route, arguments, null);
	}

	public static MiddlewareResult abortWithCallback(Runnable callback) {
		return new MiddlewareResult(false, null, null, callback);
	}

	public boolean shouldProceed() {
		return shouldProceed;
	}

	public String getRedirectTo() {
		return redirectTo;
	}

	public Object getRedirectArguments() {
		return redirectArguments;
	}

	public Runnable getOnFailureCallback() {
		return onFailureCallback;
	}
}