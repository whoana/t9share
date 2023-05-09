package rose.mary.trace.core.data.common;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Node implements Serializable{

    private static final long serialVersionUID = 1L;

    String id;
    String type;
    String date;
    String previousId;
    String status;
 

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getPreviousId() {
        return previousId;
    }
    public void setPreviousId(String previousId) {
        this.previousId = previousId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public static void main(String[] args) {
        Node node1 = new Node();
        node1.setDate("2022082410000000001");
        node1.setId("1");
        node1.setType("SNDR");
        node1.setStatus("00");

        Node node2 = new Node();
        node2.setDate("2022082410000000002");
        node2.setId("2");
        node2.setType("BRKR");
        node2.setStatus("00");
        node2.setPreviousId(node1.getId());
        
        Node node3 = new Node();
        node3.setDate("2022082410000000003");
        node3.setId("3");
        node2.setType("RCVR");
        node3.setStatus("00");
        node3.setPreviousId(node2.getId());

        Map<String, Node> nodeMap = new LinkedHashMap<String, Node>();
        nodeMap.put("1", node1);
        nodeMap.put("2", node2);
        nodeMap.put("3", node3);

        System.out.println("map:" + nodeMap);
        int nodeLength = nodeMap.size();
        System.out.println("processNodeLength:" + nodeLength);


        // int toddNodeCount = 1;
        // int finishNodeCount = finishNodeCount(nodeMap);
        // boolean hasError = containError(nodeMap);
        // String status;
        
        // if(!hasError){
        //     if(finishNodeCount >= toddNodeCount){
        //         status = SUCCESS;
        //     }else{
        //         status = ING;
        //     }
        // }esle{
        //     status = ERROR;
        // }
    


 

    }
    
}
