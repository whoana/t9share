/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.simulator;

import java.util.List;

import rose.mary.trace.core.helper.module.mte.MTEHeader;

/**
 * <pre>
 * rose.mary.trace.test
 * TraceMsgCreator.java
 * </pre>
 * 
 * @author whoana
 * @date Jul 24, 2019
 */
public interface TraceMsgCreator {

	public List<MTEHeader> create();

	/**
	 * @param interfaceId
	 * @param status
	 * @return
	 */
	public List<MTEHeader> create(String interfaceId, String status);

	/**
	 * <pre>
	 *  하나의 노드 메시지 발생
	 * </pre>
	 * 
	 * @param interfaceId
	 * @param status
	 * @param node
	 * @return
	 */
	public MTEHeader create(
			String interfaceId,
			String date,
			String time,
			String groupId,
			String hostId,
			String status,
			String nodeName,
			String processHostId,
			String processOsType,
			String processOsVersion,
			String processId,
			String processType,
			String processMode,
			String hubCnt,
			String spokeCnt,
			String recvSpokeCnt,
			String hopCnt,
			String appType,
			String recordCnt,
			String recordSize,
			String dataSize,
			String dataCompressYn,
			String errorCode,
			String errorMessage);

	/**
	 * @param string
	 * @param i
	 * @return
	 */
	public String createInterfaceId(String prefix, int index);

	/**
	 * @return
	 */
	public String createStatus(int index);

}
