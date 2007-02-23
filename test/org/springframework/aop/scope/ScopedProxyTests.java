/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.scope;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.NoOpScope;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ScopedProxyTests extends TestCase {

	/* SPR-2108 */
	public void testProxyAssignable() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("scopedMap.xml", getClass()));
		Object baseMap = bf.getBean("singletonMap");
		assertTrue(baseMap instanceof Map);
	}

	public void testSimpleProxy() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("scopedMap.xml", getClass()));
		Object simpleMap = bf.getBean("simpleMap");
		assertTrue(simpleMap instanceof Map);
		assertTrue(simpleMap instanceof HashMap);
	}

	public void testCreateJdkScopedProxy() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("scopedTestBean.xml", getClass()));
		ITestBean bean = (ITestBean) bf.getBean("testBean");
		assertNotNull(bean);
		assertTrue(AopUtils.isJdkDynamicProxy(bean));
	}

	public void testScopedList() {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("scopedList.xml", getClass()));
		bf.registerScope("request", new NoOpScope());
		TestBean tb = (TestBean) bf.getBean("testBean");
		assertTrue(tb.getFriends() instanceof ScopedObject);
	}

}
