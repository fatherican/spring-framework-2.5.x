/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.aop.support;

/**
 * This class is retained for backward compatibility.
 * As of Spring 1.1, use Perl5RegexpMethodPointcut for
 * Perl5 pointcuts as in Spring 1.0. This changes allows
 * us to support other pointcut syntaxes in future releases
 * and preserves consistent naming conventions within the framework.
 * @author Rod Johnson
 * @deprecated use Perl5RegexpMethodPointcut
 */
public class RegexpMethodPointcut extends Perl5RegexpMethodPointcut {

}
