package rose.mary.trace.core.data.common;

import java.io.Serializable;

public class StateEvent implements Serializable{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


    String id;

    String botId;

    long timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    

    
}
