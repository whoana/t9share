/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.exception;

/**
 * <pre>
 * rose.mary.trace.exception
 * RequiredFieldException.java
 * </pre>
 * 
 * @author whoana
 * @date Aug 5, 2019
 */
public class RequiredFieldException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String fieldName = null;

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public RequiredFieldException(String fieldName) {
		super();
		this.fieldName = fieldName;
	}

}
