/*
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

package org.apache.ambari.server.api.services.mpackadvisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackAdvisorCommandType;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MpackAdvisorRunner {

  private final static Logger LOG = LoggerFactory.getLogger(MpackAdvisorRunner.class);

  @Inject
  private Configuration configs;

  /**
   * Runs mpack_advisor_wrapper.py script in the specified {@code actionDirectory}.
   *
   * @param serviceAdvisorType PYTHON or JAVA
   * @param maCommandType {@link StackAdvisorCommandType} to run.
   * @param actionDirectory directory for the action
   */
  public void runScript(ServiceInfo.ServiceAdvisorType serviceAdvisorType, MpackAdvisorCommandType maCommandType, File actionDirectory)
      throws MpackAdvisorException {
    LOG.info(String.format("MpackAdvisorRunner. serviceAdvisorType=%s, actionDirectory=%s, command=%s", serviceAdvisorType, actionDirectory,
        maCommandType));

    String outputFile = actionDirectory + File.separator + "mpackadvisor.out";
    String errorFile = actionDirectory + File.separator + "mpackadvisor.err";

    String hostsFile = actionDirectory + File.separator + "hosts.json";
    String servicesFile = actionDirectory + File.separator + "services.json";

    LOG.info("MpackAdvisorRunner. Expected files: hosts.json={}, services.json={}, output={}, error={}", hostsFile, servicesFile, outputFile, errorFile);

    int stackAdvisorReturnCode = -1;

    switch (serviceAdvisorType) {
      case JAVA:
        // TODO : Future Reference point to start : StackAdvisorRunner.java
        break;
      case PYTHON:
        LOG.info("MpackAdvisorRunner.runScript(): Calling Python Mpack Advisor.");
        ProcessBuilder builder = prepareShellCommand(ServiceInfo.ServiceAdvisorType.PYTHON, MpackAdvisorHelper.pythonMpackAdvisorScript, maCommandType,
            actionDirectory, outputFile, errorFile);
        builder.environment().put("METADATA_DIR_PATH", configs.getProperty(Configuration.METADATA_DIR_PATH));
        builder.environment().put("BASE_SERVICE_ADVISOR", Paths.get(configs.getProperty(Configuration.METADATA_DIR_PATH), "service_advisor.py").toString());
        builder.environment().put("BASE_STACK_ADVISOR", Paths.get(configs.getProperty(Configuration.METADATA_DIR_PATH), "stack_advisor.py").toString());
        stackAdvisorReturnCode = launchProcess(builder);
        break;
    }

    // For both Python and Java, need to process log files for now.
    processLogs(stackAdvisorReturnCode, outputFile, errorFile);
  }

  /**
   * Launch a process, wait for it to finish, and return its exit code.
   * @param builder Process Builder
   * @return
   * @throws MpackAdvisorException
   */
  private int launchProcess(ProcessBuilder builder) throws MpackAdvisorException {
    int exitCode = -1;
    Process process = null;
    try {
      process = builder.start();
      exitCode = process.waitFor();
    } catch (Exception ioe) {
      String message = "Error executing Mpack Advisor: ";
      LOG.error(message, ioe);
      throw new MpackAdvisorException(message + ioe.getMessage());
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
    return exitCode;
  }

  /**
   * Process the exit code and logs. If non-zero then parse the log files and raise the appropriate exception.
   * @param exitCode Process exit code
   * @param outputFile Path to output file
   * @param errorFile Path to error file
   * @throws MpackAdvisorException
   */
  private void processLogs(int exitCode, String outputFile, String errorFile) throws MpackAdvisorException {
    String outMessage = printMessage("stdout", outputFile);
    String errMessage = printMessage("stderr", errorFile);

    try {
      if (exitCode != 0) {
        String errorMessage;
        if (errMessage != null) {
          // We want to get the last line.
          int index = errMessage.lastIndexOf("\n");
          if (index > 0 && index == (errMessage.length() - 1)) {
            index = errMessage.lastIndexOf("\n", index - 1); // sentence ended with newline
          }
          if (index > -1) {
            errMessage = errMessage.substring(index + 1).trim();
          }
          errorMessage = String.format("Mpack Advisor reported an error. Exit Code: %s. Error: %s ", exitCode, errMessage);
        } else {
          errorMessage = String.format("Error occurred during Mpack Advisor execution. Exit Code: %s", exitCode);
        }
        errorMessage += "\nStdOut file: " + outputFile + "\n";
        errorMessage += "\nStdErr file: " + errorFile;
        switch (exitCode) {
          case 1:
            throw new MpackAdvisorRequestException(errorMessage);
          case 2:
            throw new MpackAdvisorException(errorMessage);
        }
      }
    } catch (MpackAdvisorException ex) {
      throw ex;
    }
  }

  /**
   * Logs messages from the output/error file.
   * @param type Severity type, stdout or stderr
   * @param file File path
   * @return File to a String
   */
  private String printMessage(String type, String file) {
    String message = null;
    try {
      message = FileUtils.readFileToString(new File(file)).trim();
      LOG.info("    Advisor script {}: {}", type, message);
    } catch (IOException io) {
      LOG.error("Error in reading script log files", io);
    }
    return message;
  }

  /**
   * Gets an instance of a {@link ProcessBuilder} that's ready to execute the
   * shell command to run the stack advisor script. This will take the
   * environment variables from the current process.
   *
   * @param serviceAdvisorType Python or Java
   * @param script Python script or jar path
   * @param maCommandType command type such as validate, configure
   * @param actionDirectory Directory that contains hosts.json, services.json, and output files
   * @param outputFile Output file path
   * @param errorFile Error file path
   * @return Process that can launch
   */
  ProcessBuilder prepareShellCommand(ServiceInfo.ServiceAdvisorType serviceAdvisorType, String script,
                                     MpackAdvisorCommandType maCommandType,
                                     File actionDirectory, String outputFile, String errorFile) {
    String hostsFile = actionDirectory + File.separator + "hosts.json";
    String servicesFile = actionDirectory + File.separator + "services.json";

    // includes the original command plus the arguments for it
    List<String> builderParameters = new ArrayList<>();

    switch (serviceAdvisorType) {
      case PYTHON:
      case JAVA:
        if (System.getProperty("os.name").contains("Windows")) {
          builderParameters.add("cmd");
          builderParameters.add("/c");
        } else {
          builderParameters.add("sh");
          builderParameters.add("-c");
        }
        break;
      default:
        break;
    }

    // for the 3rd argument, build a single parameter since we use -c
    // ProcessBuilder doesn't support output redirection until JDK 1.7
    String commandStringParameters[] = new String[] {script, maCommandType.toString(), hostsFile, servicesFile, "1>", outputFile, "2>", errorFile};

    StringBuilder commandString = new StringBuilder();
    for (String command : commandStringParameters) {
      commandString.append(command).append(" ");
    }

    builderParameters.add(commandString.toString());

    LOG.debug("MpackAdvisorRunner. Mpack advisor command is {}", StringUtils.join(" ", builderParameters));

    return new ProcessBuilder(builderParameters);
  }

  public void setConfigs(Configuration configs) {
    this.configs = configs;
  }
}
