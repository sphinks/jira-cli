import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

import org.apache.commons.cli.*;


public class JiraClientMainClass {

	private static Options options = new Options();

	/**
	 * Apply Apache Commons CLI PosixParser to command-line arguments.
	 * 
	 * @param commandLineArguments Command-line arguments to be processed with
	 *    Posix-style parser.
	 */
	public static void usePosixParser(final String[] commandLineArguments)
	{
		final CommandLineParser cmdLinePosixParser = new PosixParser();
		final Options posixOptions = constructPosixOptions();
		CommandLine commandLine;
		try
		{
			commandLine = cmdLinePosixParser.parse(posixOptions, commandLineArguments);
			
			SimpleGrammar sg = new SimpleGrammar();
			Rule r = new Rule(Command.ISSUE, new Object[] {Command.LOGIN});
			sg.addRule(r);
			ParserResult aplicableRule = sg.parser(commandLine);
		}
		catch (ParseException parseException)  // checked exception
		{
			System.err.println(
					"Encountered exception while parsing using PosixParser:\n"
					+ parseException.getMessage() );
		}
	}

	/**
	 * Construct and provide Posix-compatible Options.
	 * 
	 * @return Options expected from command-line of Posix form.
	 */
	public static Options constructPosixOptions()
	{
		final Options posixOptions = new Options();
		Collection<Option> commands = Command.commands.values();
		for (Iterator<Option> i = commands.iterator(); i.hasNext();) {
			posixOptions.addOption((Option)i.next());
		}
		return posixOptions;
	}

	/**
	 * Display command-line arguments without processing them in any further way.
	 * 
	 * @param commandLineArguments Command-line arguments to be displayed.
	 */
	public static void displayProvidedCommandLineArguments(
			final String[] commandLineArguments,
			final OutputStream out)
	{
		final StringBuffer buffer = new StringBuffer();
		for ( final String argument : commandLineArguments )
		{
			buffer.append(argument).append(" ");
		}
		try
		{
			out.write((buffer.toString() + "\n").getBytes());
		}
		catch (IOException ioEx)
		{
			System.err.println(
					"WARNING: Exception encountered trying to write to OutputStream:\n"
					+ ioEx.getMessage() );
			System.out.println(buffer.toString());
		}
	}

	/**
	 * Display example application header.
	 * 
	 * @out OutputStream to which header should be written.
	 */
	public static void displayHeader(final OutputStream out)
	{
		final String header = "[JIRA CLI]\n";
		try
		{
			out.write(header.getBytes());
		}
		catch (IOException ioEx)
		{
			System.out.println(header);
		}
	}

	/**
	 * Write the provided number of blank lines to the provided OutputStream.
	 * 
	 * @param numberBlankLines Number of blank lines to write.
	 * @param out OutputStream to which to write the blank lines.
	 */
	public static void displayBlankLines(
			final int numberBlankLines,
			final OutputStream out)
	{
		try
		{
			for (int i=0; i<numberBlankLines; ++i)
			{
				out.write("\n".getBytes());
			}
		}
		catch (IOException ioEx)
		{
			for (int i=0; i<numberBlankLines; ++i)
			{
				System.out.println();
			}
		}
	}

	/**
	 * Print usage information to provided OutputStream.
	 * 
	 * @param applicationName Name of application to list in usage.
	 * @param options Command-line options to be part of usage.
	 * @param out OutputStream to which to write the usage information.
	 */
	public static void printUsage(
			final String applicationName,
			final Options options,
			final OutputStream out)
	{
		final PrintWriter writer = new PrintWriter(out);
		final HelpFormatter usageFormatter = new HelpFormatter();
		usageFormatter.printUsage(writer, 80, applicationName, options);
		writer.flush();
	}

	/**
	 * Write "help" to the provided OutputStream.
	 */
	public static void printHelp(
			final Options options,
			final int printedRowWidth,
			final String header,
			final String footer,
			final int spacesBeforeOption,
			final int spacesBeforeOptionDescription,
			final boolean displayUsage,
			final OutputStream out)
	{
		final String commandLineSyntax = "java -cp jira-cli.jar";
		final PrintWriter writer = new PrintWriter(out);
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(
				writer,
				printedRowWidth,
				commandLineSyntax,
				header,
				options,
				spacesBeforeOption,
				spacesBeforeOptionDescription,
				footer,
				displayUsage);
		writer.flush();
	}
	
	/**
	 * Method to separate different commands using pipe symbol "|"
	 * @param commandLineArguments Command-line arguments
	 * @return list of commands
	 */
	public static List<String[]> separateCommands(String[] commandLineArguments) {
		List<String[]> commands = new LinkedList<String[]>();
		String[] tempStringArray = null;
		int lastSeparator = 0;
		for ( int i = 0; i < commandLineArguments.length; i++ )
		{
			if ( commandLineArguments[i].equals("|") ) {
				tempStringArray = Arrays.copyOfRange(commandLineArguments, lastSeparator, i);
				lastSeparator = i+1;
				commands.add(tempStringArray);
			}
		}
		if (lastSeparator < commandLineArguments.length-1) {
			tempStringArray = Arrays.copyOfRange(commandLineArguments, lastSeparator, commandLineArguments.length);
			commands.add(tempStringArray);
		}
		if (commands.isEmpty()) {
			commands.add(commandLineArguments);
		}
		return commands;
	}

	/**
	 * Main executable method used to demonstrate Apache Commons CLI.
	 * 
	 * @param commandLineArguments Commmand-line arguments.
	 */
	public static void main(final String[] commandLineArguments)
	{
		LinkedList<String[]> commands = (LinkedList<String[]>)separateCommands(commandLineArguments);
		final String applicationName = "jira-cli";
		displayBlankLines(1, System.out);
		displayHeader(System.out);
		displayBlankLines(2, System.out);
		if (commandLineArguments.length < 1)
		{
			displayBlankLines(2, System.out);

			System.out.println("-- HELP --");
			printHelp(
					constructPosixOptions(), 80, "POSIX HELP", "End of POSIX Help",
					3, 5, true, System.out);
		}
		
		
		displayProvidedCommandLineArguments(commandLineArguments, System.out);
		constructPosixOptions();
		for (Iterator<String[]> listIterator = commands.iterator(); listIterator.hasNext();) {
			
			usePosixParser(listIterator.next());
		}
	}
	
	
	
	/**
	 * @param args
	 */
	/*public static void main(String[] args) {
		
		
		/*URI jiraServerUri = null;
		JiraRestClient restClient = null;
		final NullProgressMonitor pm = new NullProgressMonitor();
		
		final JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
		try{
			jiraServerUri = new URI("http://sandbox.onjira.com");
			restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, "sphinks", "654321");
			

			
		}catch(URISyntaxException ex){
			System.out.println("Can`t establish connection with " + jiraServerUri.toString());
		}*/
		
		
		
		
		/*Scanner scanner = new Scanner();
		char[] in;
		String tmpStr = "";
		for (int i = 0; i < args.length; i++) {
			tmpStr += args[i] + ' ';
		}
		in = tmpStr.toCharArray(); 
		Token[] tokenArray = scanner.scan(in);
		System.out.println("//////////");
	    for (int i = 0; i < tokenArray.length; i++){
	    	System.out.println(tokenArray[i].getKind());
	    }*/
		/*CommandParcer cp = new CommandParcer();
		Command com = cp.parseCommand(args[0]);
		if (com.equals(new Command(CommandsList.issue,"",""))) {
			final Issue issue = restClient.getIssueClient().getIssue("TST-1", pm);
			System.out.println(issue);
		}*/

	//}

}
