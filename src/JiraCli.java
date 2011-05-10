import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
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
			usePosixParser(commandLineArguments);
		}
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
			try{
				JiraClient jc = new JiraClient("http://sandbox.onjira.com", "sphinks", "654321");
				/*if (commandLine.hasOption(Command.LOGIN.getOpt())) {
					String[] arguments = commandLine.getOptionValues(Command.LOGIN.getOpt());
					System.out.println("We try to Login with: " + arguments[0] + ' ' + arguments[1]);
					System.out.println(jc.performCommand(commandLine.getOptions()[0]));
				}
				if (commandLine.hasOption(Command.ISSUE.getOpt())) {
					String[] arguments = commandLine.getOptionValues(Command.ISSUE.getOpt());
					System.out.println("We try to Issue with: " + arguments[0] + ' ' + arguments[1]);
					System.out.println(jc.performCommand(commandLine.getOptions()[0]));
				}*/
				if (commandLine.hasOption(Command.HELP.getOpt())) {
					displayBlankLines(2, System.out);
					System.out.println("-- HELP --");
					printHelp(
							constructPosixOptions(), 80, "Options", "-- HELP --",
							3, 5, true, System.out);
				}else{
					System.out.println(jc.performCommand(commandLine.getOptions()));
				}
			}catch(URISyntaxException ex){
				System.err.println("Incorrect URI: " + ex.toString());
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
		Option opt = OptionBuilder.withLongOpt( "block-size" )
        .withDescription( "use SIZE-byte blocks" )
        .hasArg()
        .withArgName("SIZE")
        .create();
		posixOptions.addOption(opt);
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
