/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.config;

/**
 * <pre>
 * rose.mary.trace.manager
 * InterfaceCacheManagerConfig.java
 * </pre>
 * 
 * @author whoana
 * @date Sep 16, 2019
 */
public class InterfaceCacheManagerConfig {

	int refreshDelay = 60 * 60;

	/**
	 * @return the refreshDelay
	 */
	public int getRefreshDelay() {
		return refreshDelay;
	}

	/**
	 * @param refreshDelay the refreshDelay to set
	 */
	public void setRefreshDelay(int refreshDelay) {
		this.refreshDelay = refreshDelay;
	}

}
