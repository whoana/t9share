package rose.mary.trace.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.channel.mom.MOMChannelExceptionHandler;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.channel.Channel;
import rose.mary.trace.core.channel.ChannelExceptionHandler;
import rose.mary.trace.core.config.ChannelManagerConfig;
import rose.mary.trace.core.config.LoaderManagerConfig;
import rose.mary.trace.core.config.OldStateCheckHandlerConfig;
import rose.mary.trace.core.data.channel.ChannelConfig;

import rose.mary.trace.core.data.common.State;

import rose.mary.trace.core.helper.module.mte.MsgHandler;
import rose.mary.trace.core.monitor.ThroughputMonitor;
import rose.mary.trace.core.parser.BytesMessageParser;
import rose.mary.trace.core.parser.MQMessageParser;
import rose.mary.trace.core.parser.Parser;
import rose.mary.trace.database.service.BotService;
import rose.mary.trace.database.service.TraceService;
import rose.mary.trace.loader.StateHandler;
import rose.mary.trace.loader.T9Loader;

/**
 * <pre>
* rose.mary.trace.manager
* DirectChannelManager.java
 * </pre>
 * 
 * @author whoana
 * @since Dec 4, 2022
 */
public class DirectLoaderManager {

    public static final int STATE_LOADER_NOTHING = 0;
    public static final int STATE_LOADER_STARTING = 10;
    public static final int STATE_LOADER_STARTTED = 11;
    public static final int STATE_LOADER_ERROR_ON_START = 12;
    public static final int STATE_LOADER_STOPPING = 20;
    public static final int STATE_LOADER_STOPPED = 21;
    public static final int STATE_LOADER_ERROR_ON_STOP = 22;

    Logger logger = LoggerFactory.getLogger(DirectLoaderManager.class);
    // Logger logger = LoggerFactory.getLogger("rose.mary.trace.SystemLogger");

    ChannelManagerConfig channelManagerConfig;

    List<T9Loader> loaders = new ArrayList<T9Loader>();

    CacheManager cacheManager = null;

    int state = STATE_LOADER_NOTHING;

    long delayOnException = 5000;

    ThroughputMonitor tpm;

    ChannelExceptionHandler momChannelExceptionHandler = null;

    LoaderManagerConfig loaderManagerConfig;
    TraceService traceService;
    BotService botService;

    StateHandler stateHandler;

    public DirectLoaderManager(
            ChannelManagerConfig channelManagerConfig,
            CacheManager cacheManager,
            ThroughputMonitor tpm,
            TraceService traceService,
            BotService botService,
            LoaderManagerConfig loaderManagerConfig,
            StateHandler stateHandler) {

        this.channelManagerConfig = channelManagerConfig;
        this.cacheManager = cacheManager;
        this.tpm = tpm;
        this.momChannelExceptionHandler = new MOMChannelExceptionHandler(
                channelManagerConfig.isTranslateMsgOnException());
        this.delayOnException = channelManagerConfig.getDelayOnException();
        this.traceService = traceService;
        this.botService = botService;
        this.loaderManagerConfig = loaderManagerConfig;
        this.stateHandler = stateHandler;

    }

    boolean loadersStarted = false;

    /**
     * @throws Exception
     */
    public synchronized void startLoaders() throws Exception {

        try {

            stopLoaders();

            state = STATE_LOADER_STARTING;

            loaders = new ArrayList<T9Loader>();
            List<ChannelConfig> channelConfigs = channelManagerConfig.getChannelConfigs();

            logger.info("loaders starting");
            for (ChannelConfig cc : channelConfigs) {

                if (cc.isDisable())
                    continue;

                switch (cc.getType()) {
                    case Channel.TYPE_WMQ:
                        startMOMChannel(cc, momChannelExceptionHandler,
                                channelManagerConfig.getOldStateCheckHandlerConfig(), stateHandler);
                        break;
                    case Channel.TYPE_ILINK:
                        startMOMChannel(cc, momChannelExceptionHandler,
                                channelManagerConfig.getOldStateCheckHandlerConfig(), stateHandler);
                        break;
                    case Channel.TYPE_REST:
                        logger.info("skip RestChannel ....");
                        break;
                    default:
                        throw new Exception(Util.join("Unknown Channel type:", cc.getType()));
                    // break;
                }
            }
            loadersStarted = true;
            state = STATE_LOADER_STARTTED;
            logger.info("loaders started");
        } catch (Exception e) {
            state = STATE_LOADER_ERROR_ON_START;
            throw e;
        } finally {

        }
    }

    /**
     * @param config
     * @param cache
     * @return
     * @throws Exception
     */
    private void startMOMChannel(
            ChannelConfig config,
            ChannelExceptionHandler channelExceptionHandler,
            OldStateCheckHandlerConfig oschc,
            StateHandler stateHandler) throws Exception {
        String name = config.getName();
        String hostName = config.getHostName();
        String qmgrName = config.getQmgrName();
        int port = config.getPort();
        String channelName = config.getChannelName();
        String queueName = config.getQueueName();
        int waitTime = config.getWaitTime();
        String module = config.getModule();
        String userId = config.getUserId();
        String password = config.getPassword();
        boolean healthCheck = config.isHealthCheck();

        int ccsid = config.getCcsid();
        int characterSet = config.getCharacterSet();
        boolean autoCommit = config.isAutoCommit();
        boolean bindMode = config.isBindMode();

        Map<String, Integer> nodeMap = oschc.getNodeMap();

        Parser parser = null;
        if (MsgHandler.MODULE_MQ.equalsIgnoreCase(module)) {
            parser = new MQMessageParser(nodeMap);
        } else if (MsgHandler.MODULE_ILINK.equalsIgnoreCase(module)) {
            parser = new BytesMessageParser(nodeMap);
        } else {
            throw new Exception("NotFoundParserException");
        }

        long maxCommitWait = config.getMaxCommitWait();
        long delayForNoMessage = config.getDelayForNoMessage();

        int commitCount = config.getCommitCount();

        int maxCacheSize = config.getMaxCacheSize();
        long delayForMaxCache = config.getDelayForMaxCache();
        boolean loadError = loaderManagerConfig.isLoadError();
        boolean loadContents = loaderManagerConfig.isLoadContents();

        CacheProxy<String, State> finCache = cacheManager.getFinCache();

        int threadCount = config.getThreadCount();
        for (int i = 0; i < threadCount; i++) {

            String loaderName = Util.join(name, ".DirectLoader", i + 1);
            T9Loader loader = new T9Loader(
                    loaderName,
                    module,
                    qmgrName,
                    hostName,
                    port,
                    channelName,
                    queueName,
                    waitTime,
                    userId,
                    password,
                    ccsid,
                    characterSet,
                    bindMode,
                    autoCommit,
                    commitCount,
                    maxCommitWait,
                    delayForNoMessage,
                    delayOnException,
                    maxCacheSize,
                    delayForMaxCache,
                    finCache,
                    traceService,
                    loadError,
                    loadContents,
                    botService,
                    stateHandler);
            if (tpm != null)
                loader.setThroughputMonitor(tpm);
            loader.setParser(parser);
            loaders.add(loader);
            loader.start();
            logger.info(Util.join("DirectLoader(", loader.getName(), ") started"));
        }

    }

    /**
     * 
     */
    public synchronized void stopLoaders() {
        try {
            state = STATE_LOADER_STOPPING;
            if (loaders != null && loaders.size() > 0) {
                logger.info("loaders stopping");
                for (T9Loader loader : loaders) {
                    loader.stop();
                    logger.info(Util.join("loader(", loader.getName(), ") stop"));
                }

                logger.info("loaders stopped");
            }
            loadersStarted = false;
            state = STATE_LOADER_STOPPED;
        } catch (Exception e) {
            state = STATE_LOADER_ERROR_ON_STOP;
            logger.error("", e);
        } finally {

        }
    }

}
