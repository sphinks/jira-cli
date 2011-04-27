import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Author: Zerin_IS
 * File: JiraCli.java
 * Date: 25.04.2011
 */


public class JiraCli {
	
	public JiraCli(String[] commandLineArguments) {
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
					constructPosixOptions(), 80, "Start of help", "End of Help",
					3, 5, true, System.out);
		}else{
		
		
			displayProvidedCommandLineArguments(commandLineArguments, System.out);
			constructPosixOptions();
			for (Iterator<String[]> listIterator = commands.iterator(); listIterator.hasNext();) {
				
				usePosixParser(listIterator.next());
			}
		}
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
			if (aplicableRule instanceof SuccessfullParserResult) {
				System.out.println(aplicableRule.toString());
			}else{
				System.err.println(aplicableRule.toString());
			}
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
}
