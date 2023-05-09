package rose.mary.trace.core.finisher.plugin;
 
import rose.mary.trace.core.data.common.State;

public interface Cleaner {

	public int clean(long currentTime, State state) throws Exception;
	
}
