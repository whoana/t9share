package rose.mary.trace.loader;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.StateEvent;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.envs.Variables;
import rose.mary.trace.core.helper.checker.StateCheckerMap;

import rose.mary.trace.core.util.IntCounter;

public class RouteHandler {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    CacheProxy<String, State> finCache;

    List<CacheProxy<String, StateEvent>> botCaches;

    List<CacheProxy<String, State>> cloneCaches;

    CacheProxy<String, Integer> routingCache;

    IntCounter counter;

    Object monitor = new Object();

    public RouteHandler(
            CacheProxy<String, State> finCache,
            List<CacheProxy<String, StateEvent>> botCaches,
            List<CacheProxy<String, State>> cloneCaches,
            CacheProxy<String, Integer> routingCache) {
        this.finCache = finCache;
        this.botCaches = botCaches;
        this.cloneCaches = cloneCaches;
        this.routingCache = routingCache;
        this.counter = new IntCounter(0, botCaches.size() - 1, 1);
    }

    public void handleState(Trace trace) throws Exception {

        synchronized (monitor) {

            String botId = Util.join(trace.getIntegrationId(), "@", trace.getDate(), "@", trace.getOriginHostId());

            Integer index = getBotCacheIndex(botId);

            // 라우팅 정보를 이용하여 finCache 를 스레드 별로 나누도록 소스 변경 고려
            State state = finCache.get(botId);

            boolean first = false;
            if (state == null) {
                long currentDate = System.currentTimeMillis();
                state = new State();
                state.setCreateDate(currentDate);
                state.setBotId(botId);
                first = true;
                finCache.put(botId, state);
            }

            StateCheckerMap.map.get(trace.getStateCheckHandlerId()).checkAndSet(first, trace, state);
            if (!state.skip()) {
                state.setLoaded(false);

                CacheProxy<String, StateEvent> botCache = botCaches.get(index);

                // 2022.08.23 dup 에러가 발생됨. merge 문을 사용하였음에도 발생.
                // 동일 배치처리 SQL 블럭에 동일 건이 포함되면 merge 문에서도 에러가 발생되지 않나 싶다.
                // 키값으로 uniqId 대신에 state.getBotId() 를 사용하는 것은 어떨까?
                // String uniqId = state.getBotId();
                String uniqId = UUID.randomUUID().toString();
                StateEvent se = new StateEvent();
                se.setId(uniqId);
                se.setBotId(state.getBotId());

                // 20221219 예외 검중중
                // 예외 발생됨, ISPN000136 , Error executing command PutKeyValueCommand on Cache
                // 'fc01', writing keys [BATCH_571@20221106210810351688@HOST_SEND]
                // 주석 처리해보자

                // finCache.put(botId, state);

                botCache.put(uniqId, se);

                if (Variables.stateTrace) {
                    logger.info(Util.join(
                            "rh:", state.getBotId(),
                            ":status:", state.getStatus(),
                            ", fnc:" + state.getFinishNodeCount(),
                            ", fsc:", state.getFinishSenderCount(),
                            ", type:", trace.getType(),
                            ", host:", trace.getHostId()));
                }

            }
        }
    }

    public void handleStateByClone(Trace trace) throws Exception {
        synchronized (monitor) {

            String botId = Util.join(trace.getIntegrationId(), "@", trace.getDate(), "@", trace.getOriginHostId());
            State state = finCache.get(botId);

            boolean first = false;
            if (state == null) {
                long currentDate = System.currentTimeMillis();
                state = new State();
                state.setCreateDate(currentDate);
                state.setBotId(botId);
                first = true;
                finCache.put(botId, state);
            }

            StateCheckerMap.map.get(trace.getStateCheckHandlerId()).checkAndSet(first, trace, state);
            if (!state.skip()) {
                state.setLoaded(false);

                Integer index = getCacheIndex(botId);

                CacheProxy<String, State> cloneCache = cloneCaches.get(index);
                State copyState = clone(state);

                finCache.put(botId, state);

                cloneCache.put(UUID.randomUUID().toString(), copyState);

                logger.info(Util.join("thread:", Thread.currentThread().getName(), "[cache", index, "]:",
                        Util.toJSONString(copyState)));

            }
        }
    }

    private State clone(State state) {
        State another = new State();
        another.setCreateDate(state.getCreateDate());
        another.setBotId(state.getBotId());
        another.setIntegrationId(state.getIntegrationId());
        another.setTrackingDate(state.getTrackingDate());
        another.setOrgHostId(state.getOrgHostId());
        another.setStatus(state.getStatus());
        another.setMatch(state.getMatch());
        another.setRecordCount(state.getRecordCount());
        another.setDataAmount(state.getDataAmount());
        another.setCompress(state.getCompress());
        another.setCost(state.getCost());
        another.setTodoNodeCount(state.getTodoNodeCount());
        another.setFinishNodeCount(state.getFinishNodeCount());
        another.setErrorNodeCount(state.getErrorNodeCount());
        another.setErrorCode(state.getErrorCode());
        another.setErrorMessage(state.getErrorMessage());
        another.setFinish(state.isFinish());
        another.setLoaded(state.isLoaded());
        another.setSkip(state.isSkip());
        another.setContext(state.getContext());
        another.setRegDate(state.getRegDate());
        another.setModDate(state.getModDate());
        another.setRetry(state.getRetry());
        another.setRetryErrorMsg(state.getRetryErrorMsg());
        another.setFinishSenderCount(state.getFinishSenderCount());
        another.setBackendLog(state.getBackendLog());

        return another;
    }

    private Integer getBotCacheIndex(String botId) throws Exception {
        Integer index = routingCache.get(botId);
        if (index == null) {
            index = counter.getAndIncrease();
            routingCache.put(botId, index);
        }
        return index;
    }

    private Integer getCacheIndex(String botId) throws Exception {
        Integer index = routingCache.get(botId);
        if (index == null) {
            index = counter.getAndIncrease();
            routingCache.put(botId, index);
        }
        return index;
    }
}
