/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.ejb.support;

import javax.ejb.EnterpriseBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanFactoryLoader;
import org.springframework.beans.factory.support.BootstrapException;

/** 
 * Superclass for all EJBs. Package-visible: not intended for direct
 * subclassing. Provides a logger and a standard way of loading a
 * BeanFactory. Subclasses act as a facade, with the business logic
 * deferred to beans in the BeanFactory.
 *
 * <p>Default is to use an XmlApplicationContextBeanFactoryLoader, which
 * will initialize an XmlApplicationContext from the classpath (based
 * on a JNDI name specified). For a lighter weight implementation when
 * ApplicationContext usage is not required, setBeanFactoryLoader() may
 * be called (<i>before</i> your EJB's ejbCreate() method is invoked, 
 * for example, in setSessionContext()) with a value of XmlBeanFactoryLoader,
 * which will load an XML bean factory from the classpath. Alternately,
 * setBeanFactoryLoader() may be called with a completely custom
 * implementation of the BeanFactoryLoader.
 *
 * <p>Note that we cannot use final for our implementation of
 * EJB lifecycle methods, as this violates the EJB specification.
 *
 * @author Rod Johnson
 * @version $Id: AbstractEnterpriseBean.java,v 1.6 2004-01-18 00:09:10 colins Exp $
 */
abstract class AbstractEnterpriseBean implements EnterpriseBean {

	/**
	 * Logger, available to subclasses. Not final since stateful session beans
	 * will have to remove it and restore it in ejbPassivate/ejbActivate.
	 */
	protected Log logger = LogFactory.getLog(getClass());

	/**
	 * Helper strategy that knows how to load Spring BeanFactory (or
	 * ApplicationContext subclass).
	 */
	private BeanFactoryLoader beanFactoryLoader;

	/** Spring BeanFactory that provides the namespace for this EJB */
	private BeanFactory beanFactory;

	/**
	 * Load a Spring BeanFactory namespace.
	 * Subclasses must invoke this method. Package-visible as it
	 * shouldn't be called directly by user-created subclasses.
	 * @see org.springframework.ejb.support.AbstractStatelessSessionBean#ejbCreate()
	 */
	void loadBeanFactory() throws BootstrapException {
		if (this.beanFactoryLoader == null) {
			this.beanFactoryLoader = new XmlApplicationContextBeanFactoryLoader();
		}
		this.beanFactory = this.beanFactoryLoader.loadBeanFactory();
	}
	
	/**
	 * Unload the Spring BeanFactory instance.
	 * The default ejbRemove method invokes this method, but subclasses which
	 * override ejbRemove must invoke this method themselves.
	 * Package-visible as it shouldn't be called directly by user-created
	 * subclasses.
	 */
	void unloadBeanFactory() throws FatalBeanException {
		if (this.beanFactory != null) {
		  this.beanFactoryLoader.unloadBeanFactory(this.beanFactory);
		  this.beanFactory = null;
		}
	}

	/**
	 * Can be invoked before loadBeanFactory.
	 * Invoke in constructor or setXXXXContext() if you want
	 * to override the default bean factory loader.
	 */
	public void setBeanFactoryLoader(BeanFactoryLoader beanFactoryLoader) {
		this.beanFactoryLoader = beanFactoryLoader;
	}

	/**
	 * May be called after ejbCreate().
	 * @return the bean Factory
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Useful EJB lifecycle method. Override if necessary.
	 */
	public void ejbRemove() {
		// Empty
	}

}
