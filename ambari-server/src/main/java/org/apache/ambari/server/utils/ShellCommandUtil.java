/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;

/**
 * Logs OpenSsl command exit code with description
 */
public class ShellCommandUtil {
  private static final Log LOG = LogFactory.getLog(ShellCommandUtil.class);
  private static final Object WindowsProcessLaunchLock = new Object();
  private static final String PASS_TOKEN = "pass:";
  private static final String KEY_TOKEN = "-key ";
  private static final String AMBARI_SUDO = "ambari-sudo.sh";

  /*
  public static String LogAndReturnOpenSslExitCode(String command, int exitCode) {
    logOpenSslExitCode(command, exitCode);
    return getOpenSslCommandResult(command, exitCode);
  }
  */
  public static void logOpenSslExitCode(String command, int exitCode) {
    if (exitCode == 0) {
      LOG.info(getOpenSslCommandResult(command, exitCode));
    } else {
      LOG.warn(getOpenSslCommandResult(command, exitCode));
    }

  }

  public static String hideOpenSslPassword(String command) {
    int start;
    if (command.contains(PASS_TOKEN)) {
      start = command.indexOf(PASS_TOKEN) + PASS_TOKEN.length();
    } else if (command.contains(KEY_TOKEN)) {
      start = command.indexOf(KEY_TOKEN) + KEY_TOKEN.length();
    } else {
      return command;
    }
    CharSequence cs = command.subSequence(start, command.indexOf(" ", start));
    return command.replace(cs, "****");
  }

  public static String getOpenSslCommandResult(String command, int exitCode) {
    return new StringBuilder().append("Command ").append(hideOpenSslPassword(command)).append(" was finished with exit code: ")
        .append(exitCode).append(" - ").append(getOpenSslExitCodeDescription(exitCode)).toString();
  }

  private static String getOpenSslExitCodeDescription(int exitCode) {
    switch (exitCode) {
      case 0: {
        return "the operation was completely successfully.";
      }
      case 1: {
        return "an error occurred parsing the command options.";
      }
      case 2: {
        return "one of the input files could not be read.";
      }
      case 3: {
        return "an error occurred creating the PKCS#7 file or when reading the MIME message.";
      }
      case 4: {
        return "an error occurred decrypting or verifying the message.";
      }
      case 5: {
        return "the message was verified correctly but an error occurred writing out the signers certificates.";
      }
      default:
        return "unsupported code";
    }
  }


  /**
   * Set to true when run on Windows platforms
   */
  public static final boolean WINDOWS
      = System.getProperty("os.name").startsWith("Windows");

  /**
   * Set to true when run on Linux platforms
   */
  public static final boolean LINUX
      = System.getProperty("os.name").startsWith("Linux");

  /**
   * Set to true when run on Mac OS platforms
   */
  public static final boolean MAC
      = System.getProperty("os.name").startsWith("Mac");

  /**
   * Set to true when run if platform is detected to be UNIX compatible
   */
  public static final boolean UNIX_LIKE = LINUX || MAC;

  /**
   * Permission mask 600 allows only owner to read and modify file.
   * Other users (except root) can't read/modify file
   */
  public static final String MASK_OWNER_ONLY_RW = "600";

  /**
   * Permission mask 777 allows everybody to read/modify/execute file
   */
  public static final String MASK_EVERYBODY_RWX = "777";


  /**
   * Gets file permissions on Linux systems.
   * Under Windows/Mac, command always returns MASK_EVERYBODY_RWX
   *
   * @param path
   */
  public static String getUnixFilePermissions(String path) {
    String result = MASK_EVERYBODY_RWX;
    if (LINUX) {
      try {
        result = runCommand(new String[]{"stat", "-c", "%a", path}).getStdout();
      } catch (IOException e) {
        // Improbable
        LOG.warn(String.format("Can not perform stat on %s", path), e);
      } catch (InterruptedException e) {
        LOG.warn(String.format("Can not perform stat on %s", path), e);
      }
    } else {
      LOG.debug(String.format("Not performing stat -s \"%%a\" command on file %s " +
          "because current OS is not Linux. Returning 777", path));
    }
    return result.trim();
  }

  /**
   * Sets file permissions to a given value on Linux systems.
   * On Windows/Mac, command is silently ignored
   *
   * @param mode
   * @param path
   */
  public static void setUnixFilePermissions(String mode, String path) {
    if (LINUX) {
      try {
        runCommand(new String[]{"chmod", mode, path});
      } catch (IOException e) {
        // Improbable
        LOG.warn(String.format("Can not perform chmod %s %s", mode, path), e);
      } catch (InterruptedException e) {
        LOG.warn(String.format("Can not perform chmod %s %s", mode, path), e);
      }
    } else {
      LOG.debug(String.format("Not performing chmod %s command for file %s " +
          "because current OS is not Linux ", mode, path));
    }
  }

  /**
   * Test if a file or directory exists
   *
   * @param path the path to test
   * @param sudo true to execute the command using sudo (ambari-sudo); otherwise false
   * @return the shell command result, success indicates the file or directory exists
   * @throws IOException
   * @throws InterruptedException
   */
  public static Result pathExists(String path, boolean sudo) throws IOException, InterruptedException {
    String[] command = {
        (WINDOWS) ? "dir" : "/bin/ls",
        path
    };

    return runCommand(command, null, null, sudo);
  }

  /**
   * Creates the specified directory and any directories in the path
   *
   * @param directoryPath the directory to create
   * @param sudo          true to execute the command using sudo (ambari-sudo); otherwise false
   * @return the shell command result
   */
  public static Result mkdir(String directoryPath, boolean sudo) throws IOException, InterruptedException {

    // If this directory already exists, do not try to create it
    if (pathExists(directoryPath, sudo).isSuccessful()) {
      return new Result(0, "The directory already exists, skipping.", ""); // Success!
    } else {
      ArrayList<String> command = new ArrayList<String>();

      command.add("/bin/mkdir");

      if (!WINDOWS) {
        command.add("-p"); // create parent directories
      }

      command.add(directoryPath);

      return runCommand(command.toArray(new String[command.size()]), null, null, sudo);
    }
  }


  /**
   * Copies a source file to the specified destination.
   *
   * @param srcFile  the path to the source file
   * @param destFile the path to the destination file
   * @param force    true to force copy even if the file exists
   * @param sudo     true to execute the command using sudo (ambari-sudo); otherwise false
   * @return the shell command result
   */
  public static Result copyFile(String srcFile, String destFile, boolean force, boolean sudo) throws IOException, InterruptedException {
    ArrayList<String> command = new ArrayList<String>();

    if (WINDOWS) {
      command.add("copy");

      if (force) {
        command.add("/Y"); // force overwrite
      }
    } else {
      command.add("cp");
      command.add("-p"); // preserve mode, ownership, timestamps

      if (force) {
        command.add("-f"); // force overwrite
      }
    }

    command.add(srcFile);
    command.add(destFile);

    return runCommand(command.toArray(new String[command.size()]), null, null, sudo);
  }

  /**
   * Runs a command with a given set of environment variables
   *
   * @param args               a String[] of the command and its arguments
   * @param vars               a Map of String,String setting an environment variable to run the command with
   * @param interactiveHandler a handler to provide responses to queries from the command,
   *                           or null if no queries are expected
   * @param sudo               true to execute the command using sudo (ambari-sudo); otherwise false
   * @return Result
   * @throws IOException
   * @throws InterruptedException
   */
  public static Result runCommand(String[] args, Map<String, String> vars, InteractiveHandler interactiveHandler, boolean sudo)
      throws IOException, InterruptedException {

    String[] processArgs;

    if (sudo) {
      processArgs = new String[args.length + 1];
      processArgs[0] = AMBARI_SUDO;
      System.arraycopy(args, 0, processArgs, 1, args.length);
    } else {
      processArgs = args;
    }

    ProcessBuilder builder = new ProcessBuilder(processArgs);

    if (vars != null) {
      Map<String, String> env = builder.environment();
      env.putAll(vars);
    }

    Process process;
    if (WINDOWS) {
      synchronized (WindowsProcessLaunchLock) {
        // To workaround the race condition issue with child processes
        // inheriting unintended handles during process launch that can
        // lead to hangs on reading output and error streams, we
        // serialize process creation. More info available at:
        // http://support.microsoft.com/kb/315939
        process = builder.start();
      }
    } else {
      process = builder.start();
    }

    // If an interactiveHandler is supplied ask it for responses to queries from the command
    // using the InputStream and OutputStream retrieved from the Process object. Use the remainder
    // of the data from the InputStream as the data for stdout.
    InputStream inputStream = process.getInputStream();
    if (interactiveHandler != null) {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      while (!interactiveHandler.done()) {
        StringBuilder query = new StringBuilder();

        while (reader.ready()) {
          query.append((char) reader.read());
        }

        String response = interactiveHandler.getResponse(query.toString());

        if (response != null) {
          writer.write(response);
          writer.newLine();
          writer.flush();
        }
      }

      writer.close();
    }

    //TODO: not sure whether output buffering will work properly
    // if command output is too intensive
    process.waitFor();
    String stdout = streamToString(inputStream);
    String stderr = streamToString(process.getErrorStream());
    int exitCode = process.exitValue();
    return new Result(exitCode, stdout, stderr);
  }

  /**
   * Runs a command with a given set of environment variables
   *
   * @param args a String[] of the command and its arguments
   * @param vars a Map of String,String setting an environment variable to run the command with
   * @return Result
   * @throws IOException
   * @throws InterruptedException
   */
  public static Result runCommand(String[] args, Map<String, String> vars)
      throws IOException, InterruptedException {
    return runCommand(args, vars, null, false);
  }

  /**
   * Run a command
   *
   * @param args A String[] of the command and its arguments
   * @return Result
   * @throws IOException
   * @throws InterruptedException
   */
  public static Result runCommand(String[] args) throws IOException,
      InterruptedException {
    return runCommand(args, null);
  }

  private static String streamToString(InputStream is) throws IOException {
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader reader = new BufferedReader(isr);
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    return sb.toString();
  }

  public static class Result {

    Result(int exitCode, String stdout, String stderr) {
      this.exitCode = exitCode;
      this.stdout = stdout;
      this.stderr = stderr;
    }

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public int getExitCode() {
      return exitCode;
    }

    public String getStdout() {
      return stdout;
    }

    public String getStderr() {
      return stderr;
    }

    public boolean isSuccessful() {
      return exitCode == 0;
    }
  }

  /**
   * InteractiveHandler is a handler for interactive sessions with command line commands.
   * <p>
   * Classes should implement this interface if there is a need to supply responses to queries from
   * the executed command (interactively, via stdin).
   */
  public interface InteractiveHandler {

    /**
     * Indicates whether this {@link InteractiveHandler} expects more queries (<code>true</code>
     * or not (<code>false</code>)
     *
     * @return true if more queries are expected; false otherwise
     */
    boolean done();

    /**
     * Gnven a query, returns the relative response to send to the shell command (via stdin)
     *
     * @param query a string containing the query that needs a response
     * @return a string or null if no response is needed
     */
    String getResponse(String query);
  }
}
