/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ServerSession;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * @author Juergen Hoeller
 * @since 1.3
 */
public class CommonsPoolServerSessionFactory extends AbstractPoolingServerSessionFactory {

	private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

	private long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private final Map serverSessionPools = Collections.synchronizedMap(new HashMap(1));


	/**
	 * Create a CommonsPoolServerSessionFactory with default settings.
	 * Default maximum size of the pool is 8.
	 * @see #setMaxSize
	 * @see GenericObjectPool#setMaxActive
	 */
	public CommonsPoolServerSessionFactory() {
		setMaxSize(GenericObjectPool.DEFAULT_MAX_ACTIVE);
	}

	/**
	 * Set the maximum number of idle ServerSessions in the pool.
	 * Default is 8.
	 * @see GenericObjectPool#setMaxIdle
	 */
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * Return the maximum number of idle ServerSessions in the pool.
	 */
	public int getMaxIdle() {
		return maxIdle;
	}

	/**
	 * Set the minimum number of idle ServerSessions in the pool.
	 * Default is 0.
	 * @see GenericObjectPool#setMinIdle
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * Return the minimum number of idle ServerSessions in the pool.
	 */
	public int getMinIdle() {
		return minIdle;
	}

	/**
	 * Set the maximum waiting time for fetching an ServerSession from the pool.
	 * Default is -1, waiting forever.
	 * @see GenericObjectPool#setMaxWait
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * Return the maximum waiting time for fetching a ServerSession from the pool.
	 */
	public long getMaxWait() {
		return maxWait;
	}

	/**
	 * Set the time between eviction runs that check idle ServerSessions
	 * whether they have been idle for too long or have become invalid.
	 * Default is -1, not performing any eviction.
	 * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * Return the time between eviction runs that check idle ServerSessions.
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return timeBetweenEvictionRunsMillis;
	}

	/**
	 * Set the minimum time that an idle ServerSession can sit in the pool
	 * before it becomes subject to eviction. Default is 1800000 (30 minutes).
	 * <p>Note that eviction runs need to be performed to take this
	 * setting into effect.
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @see GenericObjectPool#setMinEvictableIdleTimeMillis
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Return the minimum time that an idle ServerSession can sit in the pool.
	 */
	public long getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}


	public ServerSession getServerSession(ListenerSessionManager sessionManager) throws JMSException {
		ObjectPool pool = null;
		synchronized (this.serverSessionPools) {
			pool = (ObjectPool) this.serverSessionPools.get(sessionManager);
			if (pool == null) {
				pool = createObjectPool(sessionManager);
				this.serverSessionPools.put(sessionManager, pool);
			}
		}
		try {
			return (ServerSession) pool.borrowObject();
		}
		catch (Exception ex) {
			JMSException jmsEx = new JMSException("Failed to borrow ServerSession from pool");
			jmsEx.setLinkedException(ex);
			throw jmsEx;
		}
	}

	protected ObjectPool createObjectPool(ListenerSessionManager sessionManager) {
		GenericObjectPool pool = new GenericObjectPool(createPoolableObjectFactory(sessionManager));
		pool.setMaxActive(getMaxSize());
		pool.setMaxIdle(getMaxIdle());
		pool.setMinIdle(getMinIdle());
		pool.setMaxWait(getMaxWait());
		pool.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		pool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		return pool;
	}

	protected PoolableObjectFactory createPoolableObjectFactory(final ListenerSessionManager sessionManager) {
		return new PoolableObjectFactory() {
			public Object makeObject() throws JMSException {
				return createServerSession(sessionManager);
			}
			public void destroyObject(Object obj) {
				destroyServerSession((ServerSession) obj);
			}
			public boolean validateObject(Object obj) {
				return true;
			}
			public void activateObject(Object obj) {
			}
			public void passivateObject(Object obj) {
			}
		};
	}

	protected void serverSessionFinished(ServerSession serverSession, ListenerSessionManager sessionManager) {
		ObjectPool pool = (ObjectPool) this.serverSessionPools.get(sessionManager);
		try {
			pool.returnObject(serverSession);
		}
		catch (Exception ex) {
			logger.error("Failed to return ServerSession to pool", ex);
		}
	}

	public void close(ListenerSessionManager sessionManager) {
		synchronized (this.serverSessionPools) {
			for (Iterator it = this.serverSessionPools.values().iterator(); it.hasNext();) {
				ObjectPool pool = (ObjectPool) it.next();
				try {
					pool.close();
				}
				catch (Exception ex) {
					logger.error("Failed to close ServerSession pool", ex);
				}
			}
		}
	}

}
