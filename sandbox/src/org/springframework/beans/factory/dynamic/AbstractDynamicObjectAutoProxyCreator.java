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

package org.springframework.beans.factory.dynamic;

import java.util.List;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 
 * @author Rod Johnson
 * @version $Id: AbstractDynamicObjectAutoProxyCreator.java,v 1.1 2004-08-10 14:27:23 johnsonr Exp $
 */
public abstract class AbstractDynamicObjectAutoProxyCreator extends AbstractAutoProxyCreator {
	
	private int expirySeconds;
	
	/**
	 * @param defaultPollIntervalSeconds
	 *            The defaultPollIntervalSeconds to set.
	 */
	public void setExpirySeconds(int defaultPollIntervalSeconds) {
		this.expirySeconds = defaultPollIntervalSeconds;
	}
	
	public int getExpirySeconds() {
		return expirySeconds;
	}


	/**
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#getInterceptorsAndAdvisorsForBean(java.lang.Object, java.lang.String, TargetSource)
	 */
	protected Object[] getInterceptorsAndAdvisorsForBean(Object bean, String beanName, TargetSource targetSource) throws BeansException {
		if (targetSource == null) {
			return DO_NOT_PROXY;
		}
		else if (!(targetSource instanceof AbstractRefreshableTargetSource)) {
			return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
		}
		else {
			
			AbstractRefreshableTargetSource ats = (AbstractRefreshableTargetSource) targetSource;
			// TargetSource must have been created by this class
			return new Object[] { new DefaultIntroductionAdvisor(ats) };
		}
	}
	
	/**
	 * Return null if not managed
	 * @return
	 */
	protected abstract AbstractRefreshableTargetSource createRefreshableTargetSource(Object bean, ConfigurableListableBeanFactory beanFactory, String beanName);
	

	/**
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#getCustomTargetSource(java.lang.Object, java.lang.String)
	 */
	protected TargetSource getCustomTargetSource(Object bean, String beanName) {
		AbstractRefreshableTargetSource ts = createRefreshableTargetSource(bean, (ConfigurableListableBeanFactory) getBeanFactory(), beanName);
		if (ts != null) {
			ts.setExpirySeconds(expirySeconds);
		}
		return ts;
	}
	
	/**
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators(java.util.List)
	 */
	public void setCustomTargetSourceCreators(List targetSourceCreators) {
		throw new UnsupportedOperationException();
	}
}
