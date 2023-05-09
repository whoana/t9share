/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import pep.per.mint.common.util.Util;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.exception.HaveNoTraceInfoException;
import rose.mary.trace.core.helper.module.mte.MTEStruct;

/**
 * <pre>
 * rose.mary.trace.parser
 * ByteMessageParser.java
 * </pre>
 *
 * @author whoana
 * @date Aug 16, 2019
 */
public class BytesMessageParser extends Parser {

  Logger logger = LoggerFactory.getLogger(BytesMessageParser.class);

  Map<String, Integer> nodeMap;

  public BytesMessageParser() {
  }

  public BytesMessageParser(Map<String, Integer> nodeMap) {
    this.nodeMap = nodeMap;
  }

  @Override
  public Trace parse(Object traceObject) throws Exception {
    BytesMessage msg = (BytesMessage) traceObject;

    byte[] strucId = new byte[4];
    msg.readBytes(strucId);
    int version = msg.readInt();
    int strucLength = msg.readInt();
    int encoding = msg.readInt();
    int codedCharSetId = msg.readInt();
    byte[] format = new byte[8];
    msg.readBytes(format);
    int flags = msg.readInt();
    int nameValueCCSID = msg.readInt();
    int mcdLength = msg.readInt();
    byte[] mcd = new byte[mcdLength];
    msg.readBytes(mcd);
    int dataLength = msg.readInt();
    byte[] usrData = new byte[dataLength];
    msg.readBytes(usrData);

    // 20221020, ilink msg data 추가하였으나 이벤트 메시지 상에 데이터는 넣지 않는다고 하여 뺀다.
    // byte[] data = new byte[(int) msg.getBodyLength()];
    // msg.readBytes(data);

    if (usrData == null || usrData.length <= 11) { // <usr></usr>
      HaveNoTraceInfoException e = new HaveNoTraceInfoException(
          "[E1001]userData is null or length <= 11:");
      e.setData(usrData);
      throw e;
    }
    
    

    Trace trace = parseUsrData(usrData);
    // trace.setData(data);

    trace.setId(
        Util.join(
            trace.getIntegrationId(),
            idSeperator,
            trace.getDate(),
            idSeperator,
            trace.getHostId(),
            idSeperator,
            trace.getProcessId()));

    if (nodeMap != null) {
      trace.setSeq(nodeMap.getOrDefault(trace.getType(), 0));
    }

    trace.setProcessEndDate(trace.getProcessDate());

    //------------------------------------------------
    // 20230417
    //read data part
    //------------------------------------------------   
    /* 
    { 
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while(true){
        byte[] b = new byte[1024];
        int res = msg.readBytes(b);
        if(res == -1) break;
        baos.write(b);
        baos.flush();
      }
      if(baos.size() > 0){        
        trace.setData(baos.toByteArray()); 
      }
    }
    */

    //method 2 : 
    // 데이터 길이  = 전체 메시지 길이 - strucLength    
    {
      long msgBodyLength = msg.getBodyLength();
      long len = msgBodyLength - strucLength;
      byte[] b = new byte[(int)len];
      int res = msg.readBytes(b, (int)len);
      if(res > -1){
        trace.setData(b); // 기본 charset UTF8 
      }
    }

    //String usrDataString = new String(trace.getData());
   
    return trace;
  }

  /**
   * @param usrData
   * @return
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public Trace parseUsrData(byte[] usrData) throws Exception {
    logger.debug("usrData:\n".concat(new String(usrData)));

    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

    ByteArrayInputStream is = new ByteArrayInputStream(usrData);

    Trace trace = new Trace();

    DefaultHandler dh = new DefaultHandler() {
      String category = null;
      String field = null;
      StringBuilder buffer = new StringBuilder();

      public void startDocument() throws SAXException {
      }

      public void endDocument() throws SAXException {
      }

      public void startElement(
          String uri,
          String localName,
          String qName,
          Attributes attributes) throws SAXException {
        // reset field
        field = null;

        if (qName.equalsIgnoreCase(MTEStruct.a)) {
          category = MTEStruct.a;
        } else if (qName.equalsIgnoreCase(MTEStruct.j)) {
          category = MTEStruct.j;
        } else if (qName.equalsIgnoreCase(MTEStruct.b)) {
          category = MTEStruct.b;
        } else if (qName.equalsIgnoreCase(MTEStruct.c)) {
          category = MTEStruct.c;
        } else if (qName.equalsIgnoreCase(MTEStruct.d)) {
          category = MTEStruct.d;
          // }else if(qName.equalsIgnoreCase(MTEStruct.e)) {
          // category = MTEStruct.e;
          // }else if(qName.equalsIgnoreCase(MTEStruct.f)) {
          // category = MTEStruct.f;
        } else if (qName.equalsIgnoreCase(MTEStruct.g)) {
          category = MTEStruct.g;
          // }else if(qName.equalsIgnoreCase(MTEStruct.h)) {
          // category = MTEStruct.h;
          // }else if(qName.equalsIgnoreCase(MTEStruct.i)) {
          // category = MTEStruct.i;
        } else {

          if (MTEStruct.a.equals(category)) {
            if (qName.equalsIgnoreCase(MTEStruct.a_host_id))
              field = MTEStruct.a_host_id;
            else if (qName.equalsIgnoreCase(MTEStruct.a_group_id))
              field = MTEStruct.a_group_id;
            else if (qName.equalsIgnoreCase(MTEStruct.a_intf_id))
              field = MTEStruct.a_intf_id;
            else if (qName.equalsIgnoreCase(MTEStruct.a_date))
              field = MTEStruct.a_date;
            else if (qName.equalsIgnoreCase(MTEStruct.a_time))
              field = MTEStruct.a_time;
            else if (qName.equalsIgnoreCase(MTEStruct.a_global_id))
              field = MTEStruct.a_global_id;
          } else if (MTEStruct.j.equals(category)) {
            if (qName.equalsIgnoreCase(MTEStruct.j_host_id))
              field = MTEStruct.j_host_id;
            else if (qName.equalsIgnoreCase(MTEStruct.j_process_id))
              field = MTEStruct.j_process_id;
          } else if (MTEStruct.b.equals(category)) {
            if (qName.equalsIgnoreCase(MTEStruct.b_host_id))
              field = MTEStruct.b_host_id;
            else if (qName.equalsIgnoreCase(MTEStruct.b_os_type))
              field = MTEStruct.b_os_type;
            else if (qName.equalsIgnoreCase(MTEStruct.b_os_version))
              field = MTEStruct.b_os_version;
          } else if (MTEStruct.c.equals(category)) {
            if (qName.equalsIgnoreCase(MTEStruct.c_date))
              field = MTEStruct.c_date;
            else if (qName.equalsIgnoreCase(MTEStruct.c_time))
              field = MTEStruct.c_time;
            else if (qName.equalsIgnoreCase(MTEStruct.c_process_mode))
              field = MTEStruct.c_process_mode;
            else if (qName.equalsIgnoreCase(MTEStruct.c_process_type))
              field = MTEStruct.c_process_type;
            else if (qName.equalsIgnoreCase(MTEStruct.c_process_id))
              field = MTEStruct.c_process_id;
            else if (qName.equalsIgnoreCase(MTEStruct.c_hub_cnt))
              field = MTEStruct.c_hub_cnt;
            else if (qName.equalsIgnoreCase(MTEStruct.c_spoke_cnt))
              field = MTEStruct.c_spoke_cnt;
            else if (qName.equalsIgnoreCase(MTEStruct.c_recv_spoke_cnt))
              field = MTEStruct.c_recv_spoke_cnt;
            else if (qName.equalsIgnoreCase(MTEStruct.c_hop_cnt))
              field = MTEStruct.c_hop_cnt;
            else if (qName.equalsIgnoreCase(MTEStruct.c_appl_type))
              field = MTEStruct.c_appl_type;
            else if (qName.equalsIgnoreCase(MTEStruct.c_timezone))
              field = MTEStruct.c_timezone;
            else if (qName.equalsIgnoreCase(MTEStruct.c_elaspsed_time))
              field = MTEStruct.c_elaspsed_time;
          } else if (MTEStruct.d.equals(category)) {
            if (qName.equalsIgnoreCase(MTEStruct.d_status))
              field = MTEStruct.d_status;
            else if (qName.equalsIgnoreCase(MTEStruct.d_error_type))
              field = MTEStruct.d_error_type;
            else if (qName.equalsIgnoreCase(MTEStruct.d_error_code))
              field = MTEStruct.d_error_code;
            else if (qName.equalsIgnoreCase(MTEStruct.d_error_message))
              field = MTEStruct.d_error_message;
            // }else if(MTEStruct.e.equals(category)) {
            // }else if(MTEStruct.f.equals(category)) {

          } else if (MTEStruct.g.equals(category)) {
            if (qName.equals(MTEStruct.g_data_size))
              field = MTEStruct.g_data_size;
            else if (qName.equalsIgnoreCase(MTEStruct.g_record_cnt))
              field = MTEStruct.g_record_cnt;
            else if (qName.equalsIgnoreCase(MTEStruct.g_data_compress))
              field = MTEStruct.g_data_compress;
            // }else if(MTEStruct.h.equals(category)) {
            // }else if(MTEStruct.i.equals(category)) {
          }
        }
        buffer = new StringBuilder();
      }

      // todo : categroy field null check
      public void characters(char[] ch, int start, int length)
          throws SAXException {
        if (buffer == null) {
          buffer = new StringBuilder();
        }
        buffer.append(ch, start, length);
      }

      public void endElement(String uri, String localName, String qName)
          throws SAXException {

        // String ca = category;
        // String fi = field;
        // String value = buffer.toString().trim();
        //
        // System.out.println("category:" + category);
        // System.out.println("field:" + field);
        // System.out.println("value:" + value);
        try {
          logger.debug(
              "qName:".concat(qName)
                  .concat("--> ")
                  .concat(category)
                  .concat(".")
                  .concat(field)
                  .concat(":")
                  .concat(buffer.toString().trim()));
        } catch (Exception e) {
        }

        
        if (MTEStruct.a.equals(category) && field != null) {
          if (field.equals(MTEStruct.a_host_id)){
            String v = buffer.toString().trim();
            trace.setOriginHostId(v);
          }else if (field.equals(MTEStruct.a_intf_id))
            trace.setIntegrationId(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.a_date))
            trace.setDate(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.a_time))
            trace.setDate(
                trace.getDate() + buffer.toString().trim());
        } else if (MTEStruct.j.equals(category) && field != null) {
          if (field.equals(MTEStruct.j_host_id))
            trace.setPreviousHostId(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.j_process_id))
            trace.setPreviousProcessId(buffer.toString().trim());
        } else if (MTEStruct.b.equals(category) && field != null) {
          if (field.equals(MTEStruct.b_os_type))
            trace.setOs(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.b_os_version))
            trace.setOs(
                trace.getOs() + buffer.toString().trim());
          else if (field.equals(MTEStruct.b_host_id))
            trace.setHostId(
                buffer.toString().trim());
        } else if (MTEStruct.c.equals(category) && field != null) {
          if (field.equals(MTEStruct.c_date))
            trace.setProcessDate(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.c_time))
            trace.setProcessDate(
                trace.getProcessDate() + buffer.toString().trim());
          else if (field.equals(MTEStruct.c_process_mode))
            trace.setType(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.c_process_id))
            trace.setProcessId(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.c_timezone))
            trace.setTimezone(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.c_elaspsed_time))
            trace.setElapsedTime(buffer.toString().trim());
          else if (field.equals(MTEStruct.c_recv_spoke_cnt)) {
            try {
              String value = buffer.toString().trim();
              trace.setTodoNodeCount(
                  Integer.parseInt(Util.isEmpty(value) ? "1" : value));
            } catch (Exception e) {
              trace.setTodoNodeCount(1);
            }
          }
        } else if (MTEStruct.d.equals(category) && field != null) {
          if (field.equals(MTEStruct.d_status))
            trace.setStatus(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.d_error_code))
            trace.setErrorCode(
                buffer.toString().trim());
          else if (field.equals(MTEStruct.d_error_message))
            trace.setErrorMessage(buffer.toString().trim());
        } else if (MTEStruct.g.equals(category) && field != null) {
          if (field.equals(MTEStruct.g_data_size)) {
            String value = buffer.toString().trim();
            trace.setDataSize(
                Integer.parseInt(Util.isEmpty(value) ? "0" : value));
          } else if (field.equals(MTEStruct.g_record_cnt)) {
            String value = buffer.toString().trim();
            trace.setRecordCount(
                Integer.parseInt(Util.isEmpty(value) ? "0" : value));
          } else if (field.equals(MTEStruct.g_data_compress)) {
            String value = buffer.toString().trim();
            value = Util.isEmpty(value) ? "N" : (value.toLowerCase().equals("true") ? "Y" : "N");
            trace.setCompress(value);
          }
        }

        field = null;
      }
    };
    parser.parse(is, dh);

    /**
     * 메지지 내용 체크
     */
    String os = trace.getOs(); // os varchar(50)
    if (os != null && os.length() > 50)
      trace.setOs(os.substring(0, 50));


    logger.debug("parse msg: " + Util.toJSONPrettyString(trace));

    return trace;
  }
}
