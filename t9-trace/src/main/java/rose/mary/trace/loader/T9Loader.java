package rose.mary.trace.loader;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.channel.ChannelExceptionHandler;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.exception.HaveNoTraceInfoException;
import rose.mary.trace.core.exception.NoMoreMessageException;
import rose.mary.trace.core.exception.RequiredFieldException;
import rose.mary.trace.core.exception.ZeroLengthMessageException;
import rose.mary.trace.core.helper.module.mte.ILinkMsgHandler;
import rose.mary.trace.core.helper.module.mte.MQMsgHandler;
import rose.mary.trace.core.helper.module.mte.MsgHandler;
import rose.mary.trace.core.monitor.ThroughputMonitor;
import rose.mary.trace.core.parser.Parser;
import rose.mary.trace.database.service.BotService;
import rose.mary.trace.database.service.TraceService;

public class T9Loader implements Runnable {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    final static int DEFAULT_COMMIT_COUNT = 1000;
    public static final int STATE_INIT = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_SHUTDOWN = 2;

    private int state = STATE_SHUTDOWN;

    private boolean isShutdown = true;
    private ThroughputMonitor tpm;
    private String stateCheckerId;

    private List<Trace> traceList = new ArrayList<Trace>();
    private List<State> stateList = new ArrayList<State>();
    private CacheProxy<String, State> finCache;

    private long commitLapse = System.currentTimeMillis();
    private long maxCommitWait = 100;
    private int totalCommitCount = 0;

    private Thread thread = null;

    private String name;
    private TraceService traceLoadService;
    private boolean loadError = true;
    private boolean loadContents = true;
    private BotService botService;

    private ChannelExceptionHandler channelExceptionHandler;
    private long delayOnException = 5000;
    private long delayForNoMessage = 1000;
    private int commitCount = DEFAULT_COMMIT_COUNT;

    private boolean autoCommit = false;

    private Parser parser = null;

    private String hostName;
    private String qmgrName;
    private int port;
    private String userId;
    private String password;
    private String channelName;
    private String queueName;
    private String module;
    private int waitTime;
    private int ccsid;
    private int characterSet;
    private boolean bindMode = true;

    private MsgHandler mh = null;

    private int maxCacheSize;
    private long delayForMaxCache;

    private StateHandler stateHandler;

    public T9Loader(
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
            CacheProxy<String, State> finCache,
            TraceService traceLoadService,
            boolean loadError,
            boolean loadContents,
            BotService botService,
            StateHandler stateHandler) throws Exception {

        this.name = name;
        this.autoCommit = autoCommit;
        this.commitCount = commitCount;
        this.maxCommitWait = maxCommitWait;
        this.delayForNoMessage = delayForNoMessage;
        this.delayOnException = delayOnException;
        this.maxCacheSize = maxCacheSize;
        this.delayForMaxCache = delayForMaxCache;
        this.finCache = finCache;

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

        this.traceLoadService = traceLoadService;
        this.loadError = loadError;
        this.loadContents = loadContents;
        this.botService = botService;
        this.stateHandler = stateHandler;

    }

    public int getState() {
        return state;
    }

    public void start() throws Exception {

        if (thread != null && thread.isAlive())
            stop();
        thread = new Thread(this, name);
        state = STATE_INIT;
        while (true) {
            try {
                logger.info(Util.join("initailizing channel:", name));
                initialize();
                logger.info(Util.join("finish initailizing channel:", name));
                break;
            } catch (Exception e) {
                logger.error("fail to initailize channel:", name, " error:", e);
                try {
                    Thread.sleep(delayOnException);
                } catch (InterruptedException ie) {
                    logger.error("", ie);
                }
            }
        }

        logger.info(Util.join("success to initailize channel:", name));
        thread.start();

    }

    private void initialize() throws Exception {
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

    public void stop() throws Exception {
        isShutdown = true;
        if (thread != null) {
            thread.interrupt();
            // MQ 채널 FFDC 발생으로 주석처리 20221213
            // try {
            // thread.join();
            // } catch (InterruptedException e) {
            // logger.error("", e);
            // }
        }
    }

    @Override
    public void run() {
        isShutdown = false;
        logger.info(Util.join("start direct loader:", name));
        state = STATE_RUNNING;

        while (Thread.currentThread() == thread && !isShutdown) {
            try {
                // logger.info(Util.join(name, " is alive!!!!!"));
                // --------------------------------------------------------------------------
                // do commit when times committing.
                // --------------------------------------------------------------------------
                if ((traceList.size() > 0 &&
                        (traceList.size() % commitCount == 0 ||
                                (System.currentTimeMillis() - commitLapse >= maxCommitWait)))) {
                    try {
                        commit();
                        totalCommitCount = totalCommitCount + traceList.size();
                    } finally {
                        commitLapse = System.currentTimeMillis();
                    }
                }

                if (finCache.getCheckedSize() >= maxCacheSize) {
                    try {
                        Thread.sleep(delayForMaxCache);
                    } catch (InterruptedException e) {
                        isShutdown = true;
                        break;
                    }
                    logger.info(
                            Util.join(
                                    "Channel[",
                                    name,
                                    "] can't handle messages any more because the size of cache[",
                                    finCache.getName(), "] is reached the max size:", maxCacheSize));
                    continue;
                }

                // --------------------------------------------------------------------------
                // 1 get message from queue
                // 2 parse Trace message
                // 3 add Trace to list for loading to db TOP0501
                // 4 change State of message
                // 5 cacheing State of message
                // 6 add State to list for loading to db TOP0503
                // --------------------------------------------------------------------------
                {
                    // 1 get message
                    Object msg = trace();
                    if (msg != null) {
                        // 2 parse message to Trace
                        // 3 add Trace to list
                        Trace trace = null;
                        trace = parser.parse(msg);
                        if (trace == null) {
                            try {
                                Thread.sleep(delayForNoMessage);
                                continue;
                            } catch (InterruptedException e1) {
                                isShutdown = true;
                                break;
                            }
                        }
                        checkRequiredField(trace);
                        trace.setRegDate(Util.getFormatedDate("yyyyMMddHHmmssSSS"));
                        trace.setStateCheckHandlerId(stateCheckerId);
                        traceList.add(trace);
                        stateHandler.handleState(trace, stateList);
                    }
                }
            } catch (RequiredFieldException re) {

                try {
                    commit(); // 문제의 메시지를 버린다.
                } catch (Exception e2) {
                    logger.error("", e2);
                }

                logger.info("ERROR:SKIP:PARSING:001:Trace msg has no the required field(".concat(re.getFieldName())
                        .concat(")"), re);

                try {
                    Thread.sleep(delayOnException);
                } catch (InterruptedException e1) {
                    isShutdown = true;
                    // return;
                    break;
                }

            } catch (ZeroLengthMessageException ze) {
                // to-do : handle NoMoreMessageException
                // 2022.07
                // MQ 채널이 가끔 이상해질때 사이즈 0인 메시지가 발생하는지
                // parser.parse(msg) 에서
                // if(msg.getTotalMessageLength() == 0) 인 경우가 발생된다.
                // 다음에 이경우가 발생하면 큐에 제대로 된 메시지를 넣어보자.
                // 아무래도 이건 MQ 버그인듯 싶다.

                logger.info("Length of message is 0, continue to get next message");
                try {
                    Thread.sleep(delayOnException);
                } catch (InterruptedException e1) {
                    isShutdown = true;
                    break;
                }

            } catch (NoMoreMessageException e) {
                try {
                    Thread.sleep(delayForNoMessage);
                } catch (InterruptedException e1) {
                    isShutdown = true;
                    break;
                }
            } catch (HaveNoTraceInfoException e) {

                try {
                    commit(); // 문제의 메시지를 버린다.
                } catch (Exception e2) {
                    logger.error("", e2);
                }

                // 에러 캐시에 따로 보관할지 옵션처리해주자
                // errorMessageCache.put(UUID.randomUUID(),d);
                // 에러케시에 넣지 말고 그냥 로그만 남기자. 2022.10
                byte[] data = e.getData();
                String msg = "Trace msg have no header";
                msg = data == null ? msg
                        : msg.concat(",length:").concat(String.valueOf(data.length)).concat(",data:")
                                .concat(new String(data));
                logger.error(msg, e);

                try {
                    Thread.sleep(delayForNoMessage);
                } catch (InterruptedException e1) {
                    isShutdown = true;
                    // return;
                    break;
                }

            } catch (Exception e) {

                try {
                    rollback();
                } catch (Exception e2) {
                    logger.error("", e2);
                }

                if (channelExceptionHandler != null) {
                    channelExceptionHandler.handleException("", e);
                } else {
                    logger.error("", e);
                }

                try {
                    Thread.sleep(delayOnException);
                } catch (InterruptedException e1) {
                    isShutdown = true;
                    // return;
                    break;
                }

            }
        }

        isShutdown = true;
        state = STATE_SHUTDOWN;
        logger.info(Util.join("stop direct loader:", name));
        logger.info(Util.join("totalCommitCount:", totalCommitCount));

    }

    private Object trace() throws Exception {
        return mh.get(waitTime);
    }

    // 1 load db TOP0501
    // 2 load db TOP0503
    // 3 clear Trace list
    // 4 clear State list
    // 5 commit mom
    private void commit() throws Exception {
        int count = traceList.size();
        if (count > 0) {
            // method a
            traceLoadService.load(traceList, loadError, loadContents);
            botService.mergeBotsSynchronized(stateList, finCache);
        }
        // method b
        // botService.mergeBots(traceList, loadError, loadContents, stateList,
        // finCache);

        // method c
        // stateHandler.commitStates(traceList, loadError, loadContents, stateList,
        // finCache);

        traceList.clear();
        stateList.clear();
        mh.commit();
        tpm.count(count);
    }

    private void rollback() throws Exception {
        mh.rollback();
        traceList.clear();
        stateList.clear();
    }

    public void setThroughputMonitor(ThroughputMonitor tpm) {
        this.tpm = tpm;
    }

    public ThroughputMonitor getThroughputMonitor() {
        return tpm;
    }

    /**
     * 필수항목 체크
     * 202210 추가
     * 필수 체크항목이 더 핑요하면 더 넣도록 한다.
     * 체크항목이 많아지면 속도가 느려짐
     */
    private void checkRequiredField(Trace trace) throws RequiredFieldException {

        logger.debug(Util.toJSONPrettyString(trace));

        if (Util.isEmpty(trace.getIntegrationId()))
            throw new RequiredFieldException("integrationId");
        if (Util.isEmpty(trace.getOriginHostId()))
            throw new RequiredFieldException("originHostId");
        if (Util.isEmpty(trace.getDate()))
            throw new RequiredFieldException("date");
        if (Util.isEmpty(trace.getProcessId()))
            throw new RequiredFieldException("processId");
        if (Util.isEmpty(trace.getStatus()))
            throw new RequiredFieldException("status");
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parser
     */
    public Parser getParser() {
        return parser;
    }

    /**
     * @param parser the parser to set
     */
    public void setParser(Parser parser) {
        this.parser = parser;
    }
}
