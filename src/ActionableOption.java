import org.apache.commons.cli.Option;

/**
 * Author: Zerin_IS
 * File: ActionableOption.java
 * Date: 15.04.2011
 */

public class ActionableOption extends Option implements Actionable{
	
	private static final long serialVersionUID = 1L;
	private boolean baseOption;

	public ActionableOption(String opt, boolean hasArg, String description, boolean isBaseOption) {
		super(opt, hasArg, description);
		baseOption = isBaseOption;
	}
	
	public boolean isBaseOption() {
		return baseOption;
	}
	
	public void action() {
		
	}
}
