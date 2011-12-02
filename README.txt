Relogger, written by Ari Rabkin
Contact asrabkin@gmail.com with any questions.


Relogger, the retroactive enhancement logger lets you add new features to the logging subsystem for existing legacy Java programs. If your program uses standard Java loggers like Log4j or Apache Commons logging, Relogger gives you an enhanced logger *retroactively*, without the need for any source code changes.

Support for additional loggers can be added upon request, even support for System.{out,err}.println() "logging".


FEATURES

Relogger offers several features not present in conventional loggers.

- By the magic of program rewriting, every log statement is numbered uniquely. The number of the corresponding statement is inserted into every log message.  This eases analysis.
- It's possible to enable or disable any particular statement, by number, at run-time.
- You can get every message printed once, providing an example of all the trace/debug messages.
- The overhead of not printing a message is *lower* with relogger than with the underlying logger, so the performance cost will be low.

FUTURE FEATURES, (can be added if useful)

- Using the print-just-once feature for sampling and rate estimation.
- Tagging messages with arbitrary strings, like "USER" or "ADMINISTRATOR."
- Changing the printed priority; if you think an ERROR message isn't, you can change that without recompiling, or even relaunching, your program.

BUILDING RELOGGER

Just say 'ant'.  You should get a numberedlogs.jar


USING RELOGGER

To use relogger:
 Add asm-3.3.1-all.jar to your application classpath. This is the bytecode manipulation library used by Relogger.
 Add -javaagent:numberedlogs.jar to the java VM arguments list for your application. You need to add it before the main-class argument -- this is a VM argument, not an application argument.

You should replace numberedlogs.jar with the path to wherever you put the relogger library.

You can also pass several arguments. The syntax looks like this:
-javaagent:numberedlogs.jar=portno=2344,file=/tmp/relogger

Formally:
	-javaagent:<path to agent>=<arglist>
	<arglist> = <option=value>
		  = <option=value>,<arglist>

Available options:
	port/portno  	The UDP port on which to listen for instructions. Defaults to 2345
	file		The location to read and store statement-numbering information.  Defaults to 'relogger/mapping.out'
	alwaysonce	Whether to print every message at least once. Any value for this option evaluates to true, except 'false'
	

COMMAND SYNTAX
	
Relogger takes several commands. You can send send these by UDP to the command port (defaults to 2345). 

In addition to UDP, it's possible to configure Relogger by creating a file named "commands" in the same directory as the mapping file (by default, 'relogger').  This file is checked for changes every two seconds. It should contain a list of commands, using the same syntax as for UDP.

	up <statement>		Enables a given statement-set
	on <statement>

	down <statement>	Disables a given statement-set
	off

	once <statement>	Print the given statement-set at least once the next time it appears


Many of these commands take a statement-set as argument. This is a string that designates one or more log statements.

<statement> =  [numeric ID]
            =  [canonical ID of the form 0x...._lineno]
            =  [class name:lineno]


TAGS [Work-in-progress]

Tags are an in-progress feature. A tag is a string that labels a set of messages. This comes with two new commands:
	addtag <statement> <tag> 	Add the given tag to the given statement-set

	rmtag <statement> <tag>		Remove the given tag from the given statement-set
	deltag <statement> <tag>


