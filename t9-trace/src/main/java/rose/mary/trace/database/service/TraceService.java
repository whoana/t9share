/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.database.service;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.database.mapper.m01.TraceMapper;

/**
 * <pre>
 * rose.mary.trace.database.service.TraceLoadService
 * TraceLoadService.java
 * </pre>
 * 
 * @author whoana
 * @date Aug 28, 2019
 */
@Service
public class TraceService {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("sqlSessionFactory01")
	SqlSessionFactory sqlSessionFactory;

	public void load(Collection<Trace> col, boolean loadError, boolean loadContents) throws Exception {
		boolean autoCommit = false;
		SqlSession session = null;
		List<BatchResult> bs = null;
		try {
			session = sqlSessionFactory.openSession(ExecutorType.BATCH, autoCommit);

			for (Trace trace : col) {
				// int res =
				// session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insert",
				// trace);
				int res = session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.upsert", trace);

				// if(loadError) {
				// session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insertError",
				// trace);
				// }
				
				// 20230416 
				// ADD 
				// 우미건설 요청 (ILIN MI 사이트)
				// 테스트 미완료
				if (loadContents && !Util.isEmpty(trace.getData())) {					
					session.delete("rose.mary.trace.database.mapper.m01.TraceMapper.deleteData", trace);
					session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insertData", trace);
				}
			}

			bs = session.flushStatements();

			session.commit();
		} catch (Exception e) {

			if (bs != null && bs.size() > 0) {
				for (BatchResult rs : bs) {
					int uc = rs.getUpdateCounts()[0];
					// session.delete(rs.getSql(), rs.getParameterObjects());
					logger.info("sql:" + rs.getSql() + ", mappedStatement:" + rs.getMappedStatement() + ", pdateCounts:"
							+ uc);
				}
			}
			session.rollback();
			throw e;
		} finally {
			if (session != null)
				session.close();
		}
	}

	@Autowired
	TraceMapper traceMapper;

	public boolean exist(String integrationId, String date, String orgHostId, String processId) throws Exception {
		return (traceMapper.exist(integrationId, date, orgHostId, processId) > 0);
	}

	public int insert(Trace trace) throws Exception {
		return traceMapper.insert(trace);
	}

	public int upsert(Trace trace) throws Exception {
		return traceMapper.upsert(trace);
	}

	public List<Trace> getTraces(String integrationId, String trackingDate, String orgHostId) throws Exception {
		return traceMapper.getList(integrationId, trackingDate, orgHostId);
	}

	public void load(Collection<Trace> col, boolean loadError, boolean loadContents, Collection<State> states,
			CacheProxy<String, State> finCache) throws Exception {
		boolean autoCommit = false;
		SqlSession session = null;
		List<BatchResult> bs = null;
		try {
			session = sqlSessionFactory.openSession(ExecutorType.BATCH, autoCommit);

			for (Trace trace : col) {
				// int res =
				// session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insert",
				// trace);
				int res = session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.upsert", trace);

				// if(loadError) {
				// session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insertError",
				// trace);
				// }
				//
				
				// 20230416 
				// ADD 
				// 우미건설 요청 (ILIN MI 사이트)
				// 테스트 미완료
				if (loadContents && !Util.isEmpty(trace.getData())) {					
					session.delete("rose.mary.trace.database.mapper.m01.TraceMapper.deleteData", trace);
					session.insert("rose.mary.trace.database.mapper.m01.TraceMapper.insertData", trace);
				}
				
			}

			bs = session.flushStatements();

			session.commit();
		} catch (Exception e) {

			if (bs != null && bs.size() > 0) {
				for (BatchResult rs : bs) {
					int uc = rs.getUpdateCounts()[0];
					// session.delete(rs.getSql(), rs.getParameterObjects());
					logger.info("sql:" + rs.getSql() + ", mappedStatement:" + rs.getMappedStatement() + ", pdateCounts:"
							+ uc);
				}
			}
			session.rollback();
			throw e;
		} finally {
			if (session != null)
				session.close();
		}
	}

}
