/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * Convenient abstract superclass for dynamic method matchers,
 * which do care about arguments at runtime.
 */
public abstract class DynamicMethodMatcher implements MethodMatcher {

	public final boolean isRuntime() {
		return true;
	}

	/**
	 * Can override to add preconditions for dynamic matching. This implementation
	 * always returns true.
	 */
	public boolean matches(Method m, Class targetClass) {
		return true;
	}

}
