/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.channel.mom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.channel.Channel;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.helper.module.mte.ILinkMsgHandler;
import rose.mary.trace.core.helper.module.mte.MQMsgHandler;
import rose.mary.trace.core.helper.module.mte.MsgHandler;

/**
 * <pre>
 * MQTraceListener
 * 큐로부터 트레이스 메시지를 읽어 들이는 채널
 * </pre>
 * 
 * @author whoana
 * @since Jul 29, 2019
 */
public class MOMChannel extends Channel {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	String hostName;
	String qmgrName;
	int port;
	String userId;
	String password;
	String channelName;
	String queueName;
	String module;
	int waitTime;
	int ccsid;
	int characterSet;
	boolean bindMode = true;

	MsgHandler mh = null;

	public MOMChannel(
			String name,
			String module,
			String qmgrName,
			String hostName,
			int port,
			String channelName,
			String queueName,
			int waitTime,
			String userId,
			String password,
			int ccsid,
			int characterSet,
			boolean bindMode,
			boolean autoCommit,
			int commitCount,
			long maxCommitWait,
			long delayForNoMessage,
			long delayOnException,
			int maxCacheSize,
			long delayForMaxCache,
			CacheProxy<String, Trace> cache) throws Exception {
		super(name, autoCommit, commitCount, maxCommitWait, delayForNoMessage, delayOnException, maxCacheSize,
				delayForMaxCache, cache);
		this.qmgrName = qmgrName;
		this.hostName = hostName;
		this.port = port;
		this.channelName = channelName;
		this.queueName = queueName;
		this.waitTime = waitTime;
		this.userId = userId;
		this.password = password;
		this.module = module;
		this.ccsid = ccsid;
		this.characterSet = characterSet;
		this.bindMode = bindMode;
		this.stateCheckerId = "rose.mary.trace.core.helper.checker.OldStateCheckHandler";

	}

	@Override
	protected void initialize() throws Exception {

		if (MsgHandler.MODULE_MQ.equalsIgnoreCase(module)) {
			mh = new MQMsgHandler(qmgrName, hostName, port, channelName, userId, password, ccsid, characterSet,
					autoCommit, bindMode);
		} else if (MsgHandler.MODULE_ILINK.equalsIgnoreCase(module)) {
			mh = new ILinkMsgHandler(qmgrName, hostName, port, channelName);
		} else {
			throw new Exception("NotFounMode:" + module);
		}

		mh.open(queueName, MsgHandler.Q_QPEN_OPT_GET);
	}

	@Override
	public Object trace() throws Exception {

		return mh.get(waitTime);

	}

	// TransactionManager tm = null;
	@Override
	protected void commit() throws Exception {

		// tm = cache.getTransactionManager();
		// if(tm != null) tm.begin();
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0101");
		cache.put(cacheMap);
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0102");
		cacheMap.clear();
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0103");
		// if(tm != null) tm.commit();
		mh.commit();
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0104");
	}

	@Override
	protected void rollback() throws Exception {
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0105");
		mh.rollback();
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0106");
		cacheMap.clear();
		logger.debug(Thread.currentThread().getName() + "-CNLBLD0107");
		// if(tm != null) mh.rollback();
	}

	// @Override
	// protected void commit() throws Exception {
	//
	// mh.commit();
	//
	// }
	//
	// @Override
	// protected void rollback() throws Exception {
	//
	// //mh.rollback();
	// mh.commit();
	//
	// }

	@Override
	public boolean ping() {
		if (mh != null)
			return mh.ping();
		else
			return false;
	}

}
