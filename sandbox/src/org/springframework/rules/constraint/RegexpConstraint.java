/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.rules.constraint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.rules.Constraint;
import org.springframework.rules.reporting.TypeResolvableSupport;

/**
 * A constraint based on a regular expression pattern.
 *
 * @author Keith Donald
 */
public class RegexpConstraint extends TypeResolvableSupport implements
		Constraint {

	private Pattern pattern;

	/**
	 * Creates a RegexpConstraint with the provided regular expression pattern
	 * string.
	 *
	 * @param regex
	 *            The regular expression
	 */
	public RegexpConstraint(String regex) {
		pattern = Pattern.compile(regex);
	}

	public boolean test(Object argument) {
		if (argument == null) {
			argument = "";
		}
		Matcher m = pattern.matcher((CharSequence) argument);
		return m.matches();
	}

}