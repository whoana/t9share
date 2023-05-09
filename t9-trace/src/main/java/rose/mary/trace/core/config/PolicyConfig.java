package rose.mary.trace.core.config;

public class PolicyConfig {
	String name = "policyConfig";
	int policy = 100;
	int policyCheckDelay = 1000;
	int databaseCheckDelay = 10000;
	int exceptionDelay = 1000;
	int policyCount = 1;
	boolean disable = false;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPolicy() {
		return policy;
	}

	public void setPolicy(int policy) {
		this.policy = policy;
	}

	public int getPolicyCheckDelay() {
		return policyCheckDelay;
	}

	public void setPolicyCheckDelay(int policyCheckDelay) {
		this.policyCheckDelay = policyCheckDelay;
	}

	public int getExceptionDelay() {
		return exceptionDelay;
	}

	public void setExceptionDelay(int exceptionDelay) {
		this.exceptionDelay = exceptionDelay;
	}

	public int getPolicyCount() {
		return policyCount;
	}

	public void setPolicyCount(int policyCount) {
		this.policyCount = policyCount;
	}

	public int getDatabaseCheckDelay() {
		return databaseCheckDelay;
	}

	public void setDatabaseCheckDelay(int databaseCheckDelay) {
		this.databaseCheckDelay = databaseCheckDelay;
	}

	public boolean isDisable() {
		return disable;
	}

	public void setDisable(boolean disable) {
		this.disable = disable;
	}

	

}
