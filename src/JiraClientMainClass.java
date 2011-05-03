import java.net.URISyntaxException;


public class JiraClientMainClass {


	/**
	 * Main executable method
	 * 
	 * @param commandLineArguments Commmand-line arguments.
	 */
	public static void main(final String[] commandLineArguments)
	{
		//Start for pipilining
		/*InputStream in;
		 
		if (args.length == 0) {
		  in = new FileInputStream(args[0]);
		}
		else {
		  in = System.in;
		}*/
		JiraCli jiraCli = new JiraCli(commandLineArguments);
		//Test only
		//need new login
		/*try{
			JiraClient jc = new JiraClient("http://sandbox.onjira.com", "sphinks", "654321");
			System.out.println(jc.getIssueProject("TST-1"));
		}catch(URISyntaxException ex){
			System.err.println("Incorrect URI: " + ex.toString());
		}*/
	}
}
