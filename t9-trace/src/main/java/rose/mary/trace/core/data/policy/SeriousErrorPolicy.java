package rose.mary.trace.core.data.policy;

import java.io.Serializable;

public class SeriousErrorPolicy implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int shutdownServer = 100;
	public static final int stopServer = 200;
	public static final int stopChannel = 300;

	public static final SeriousErrorPolicy SHUTDOWN = new SeriousErrorPolicy(shutdownServer);
	public static final SeriousErrorPolicy STOP_SERVER = new SeriousErrorPolicy(stopServer);
	public static final SeriousErrorPolicy STOP_CHANNEL = new SeriousErrorPolicy(stopChannel);

	int policy = stopChannel;

	public SeriousErrorPolicy() {
		this.policy = stopChannel;
	}

	public SeriousErrorPolicy(int policy) {
		this.policy = policy;
	}

	public int getPolicy() {
		return policy;
	}

	public void setPolicy(int policy) {
		this.policy = policy;
	}

	@Override
	public String toString() {
		String str = "unknown policy";
		switch (policy) {
			case shutdownServer:
				str = "shutdownServerPolicy";
				break;
			case stopChannel:
				str = "stopChannelPolicy";
				break;
			case stopServer:
				str = "stopServerPolicy";
				break;
		}
		return str;
	}

}
