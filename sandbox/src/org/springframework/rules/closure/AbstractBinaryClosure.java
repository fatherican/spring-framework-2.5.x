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
package org.springframework.rules.closure;

import java.io.Serializable;

import org.springframework.rules.BinaryClosure;
import org.springframework.util.Assert;

public abstract class AbstractBinaryClosure implements BinaryClosure, Serializable {

	public Object call(Object argument1) {
		if (argument1 == null || argument1 == NULL_VALUE) {
			argument1 = new Object[0];
		}
		Assert.isTrue(argument1.getClass().isArray(),
				"Binary argument must be an array");
		Object[] arguments = (Object[]) argument1;
		Assert.isTrue(arguments.length == 2,
				"Binary argument must contain 2 elements");
		return call(arguments[0], arguments[1]);
	}

}