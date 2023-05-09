package rose.mary.trace.core.helper.checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rose.mary.trace.core.config.OldStateCheckHandlerConfig;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.system.SystemUtil;

public class OldStateCheckHandler implements StateChecker {

	Logger logger = LoggerFactory.getLogger(getClass());

	OldStateCheckHandlerConfig config;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int KEY_SNDR = 10;
	public static final int KEY_BRKR = 20;
	public static final int KEY_REPL = 30;
	public static final int KEY_RBRK = 40;
	public static final int KEY_RCVR = 50;

	/**
	 * 우미건설 1건발생 트레킹 패턴 process_mode 처리 코드 추가 
	 * 20230119  
	 */
	public static final int KEY_SNRC = 60;

	final static public String ST_SUCCESS = "00";
	final static public String ST_ING = "01";
	final static public String ST_FAIL = "99";

	boolean usePreviousProcessInfo = false;

	public OldStateCheckHandler(OldStateCheckHandlerConfig config) {
		this.config = config;
	}


	/**
	 * <pre>
	 * 	State 객체 업데이트 
	 *  --------------------------------
	 *  cost 처리시간 값 구하는 부분은 미완성 
	 *  송신 시작 시간을 State 객체에  마킹하고 
	 *  수신 도착 시간에 State 의 cost 값에 계산해서 넣는 방식을 고려해 보자 .
	 * <pre>
	 */
	@Override
	public void checkAndSet(boolean first, Trace trace, State state) {

		String trackingDate = trace.getDate();
		String orgHostId = trace.getOriginHostId();
		String status = trace.getStatus();
		String type = trace.getType();
		int typeSeq = config.getNodeMap().putIfAbsent(type, 0);
		int recordCount = trace.getRecordCount();
		int dataAmount = trace.getDataSize();
		String compress = trace.getCompress();
		int cost = 0;		
		int todoNodeCount = trace.getTodoNodeCount();
		String errorCode = trace.getErrorCode();
		String errorMessage = trace.getErrorMessage();
		String integrationId = trace.getIntegrationId();

		state.setSkip(false);
		if (first) {
			// ------------------------------------------
			// 최초로 도착한 헤더정보를 기준으로 세팅되는 값
			// ------------------------------------------
			state.setIntegrationId(integrationId);
			state.setTrackingDate(trackingDate);
			state.setOrgHostId(orgHostId);
			state.setCompress(compress);
			state.setCost(cost);// 처리시간값(단위는 자유)
			state.setDataAmount(dataAmount);
			state.setRecordCount(recordCount);
			state.setTodoNodeCount(todoNodeCount);
			state.setFinishSenderCount(0);
		}

		switch (typeSeq) {
			case KEY_SNDR:
				state.setFinishSenderCount(state.getFinishSenderCount() + 1);
				if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {

				}
				break;
			case KEY_BRKR:
				KEY_RBRK: if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {
					state.setSkip(true);
				}
				break;
			case KEY_REPL:
				if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {
					state.setSkip(true);
				}
				break;

			case KEY_RCVR:
				state.setFinishNodeCount(state.getFinishNodeCount() + 1);
				if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {
				}


				// @Todo
				// 응답 도착 시점에 전체 cost 를 구할 수 있으나 
				// 현재 런타임 구조에서는 트래킹이 순차적으로 수집되지 않을 경우가 있어 
				// 전체 cost 계산하는데 문제가 있다. 
				// 일단 RCVR, SNRC 일 때문 cost 를 계산하도록 해둔다. 
				// SNDR, BRKR 이 RCVR, SNRC 보다 나중에 와서 값을 덮어 씌울 수 있으므로  해당 상태에서는 cost 값을 계산하지 않는다. (20230404).
				// -------------------------------------------------------------
				// cost 구하기 
				// -------------------------------------------------------------
				try {
					String processDate = trace.getProcessDate();
					long elapsedMillis = SystemUtil.getElapsedMillis(processDate, trackingDate);			
					cost = (int)elapsedMillis;
				} catch (Exception e) {
					cost = 0;
				}
				state.setCost(cost);


				break;

			case KEY_SNRC:
				/**
				 * 20230119
				 * 송수신이 동시에 오는 CASE ?
				 * 우미건설에서 BRKR 1건만 로 오는 경우가 있어 확인하는 과정에서 발견함.
				 */
				state.setTodoNodeCount(1);
				state.setFinishSenderCount(state.getFinishSenderCount() + 1);				
				state.setFinishNodeCount(state.getFinishNodeCount() + 1);
				if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {
				}


				// @Todo
				// 응답 도착 시점에 전체 cost 를 구할 수 있으나 
				// 현재 런타임 구조에서는 트래킹이 순차적으로 수집되지 않을 경우가 있어 
				// 전체 cost 계산하는데 문제가 있다. 
				// 일단 RCVR, SNRC 일 때문 cost 를 계산하도록 해둔다.
				// SNDR, BRKR 이 RCVR, SNRC 보다 나중에 와서 값을 덮어 씌울 수 있으므로  해당 상태에서는 cost 값을 계산하지 않는다. (20230404).
				// -------------------------------------------------------------
				// cost 구하기 
				// -------------------------------------------------------------
				try {
					String processDate = trace.getProcessDate();
					long elapsedMillis = SystemUtil.getElapsedMillis(processDate, trackingDate);			
					cost = (int)elapsedMillis;
				} catch (Exception e) {
					cost = 0;
				}
				state.setCost(cost);
				
				break;
				
			default:
				if (ST_FAIL.equals(status)) {
					state.setErrorCode(errorCode);
					state.setErrorMessage(errorMessage);
					state.setErrorNodeCount(state.getErrorNodeCount() + 1);
				} else {
				}
				break;
		}

		

		// 트레킹 키값 조합 디버깅시 사용할 용도로 남겨둠 .20220826
		// String tk = state.getBotId() + "@" + typeSeq;

		String beforeStatus = state.getStatus();
		boolean notFinished = !state.isFinish();
		boolean senderReceived = state.isFinishSender();
		int finishNodeCount = state.getFinishNodeCount();
		// --------------------------------------------------
		// 완료여부 세팅
		// --------------------------------------------------
		if (notFinished && finishNodeCount >= todoNodeCount && senderReceived) {
			// 직전상태 미완료 이며, 처리해야할 노드 숫자가 발생된 트래킹 노드 수가 일치하고 첫번째 노드를 받았으면
			state.setFinish(true); // 완료처리
		}

		// --------------------------------------------------
		// 최종 상태(State.status) 세팅
		// --------------------------------------------------
		if (ST_ING.equals(beforeStatus)) {// 이전 상태가 "진행중" 일때만 상태 변경 처리
			if (ST_FAIL.equals(status)) {
				state.setStatus(ST_FAIL);// 실패
			} else {
				if (state.isFinish()) {
					state.setStatus(ST_SUCCESS);// 성공
				} else {
					state.setStatus(ST_ING);// 진행중
				}
			}
		}

		// state.setContext(tk);
		// logger.debug("boter, tk=[" + tk + "]");
	}

}
