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
 * @author Dmitriy Kopylenko
 * @since 1.1
 * @version $Id: Perl5RegexpMethodPointcutTests.java,v 1.1 2004-07-28 18:35:27 dkopylenko Exp $
 */
public class Perl5RegexpMethodPointcutTests extends AbstractRegexpMethodPointcutTests {
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        rpc = new Perl5RegexpMethodPointcut();
    }
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        rpc = null;
    }
}
