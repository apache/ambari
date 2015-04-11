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

package org.apache.ambari.server.api.services.stackadvisor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class StackAdvisorRunner {

  private final static Logger LOG = LoggerFactory.getLogger(StackAdvisorRunner.class);

  /**
   * Runs stack_advisor.py script in the specified {@code actionDirectory}.
   *
   * @param script stack advisor script
   * @param saCommandType {@link StackAdvisorCommandType} to run.
   * @param actionDirectory directory for the action
   */
  public void runScript(String script, StackAdvisorCommandType saCommandType, File actionDirectory)
      throws StackAdvisorException {
    LOG.info(String.format("Script=%s, actionDirectory=%s, command=%s", script, actionDirectory,
        saCommandType));

    String outputFile = actionDirectory + File.separator + "stackadvisor.out";
    String errorFile = actionDirectory + File.separator + "stackadvisor.err";

    ProcessBuilder builder = prepareShellCommand(script, saCommandType,
        actionDirectory, outputFile,
        errorFile);

    try {
      Process process = builder.start();

      try {
        LOG.info("Stack-advisor output={}, error={}", outputFile, errorFile);

        int exitCode = process.waitFor();
        String outMessage;
        String errMessage = null;
        try {
          outMessage = FileUtils.readFileToString(new File(outputFile)).trim();
          errMessage = FileUtils.readFileToString(new File(errorFile)).trim();
          LOG.info("Stack advisor output files");
          LOG.info("    advisor script stdout: {}", outMessage);
          LOG.info("    advisor script stderr: {}", errMessage);
        } catch (IOException io) {
          LOG.error("Error in reading script log files", io);
        }
        if (exitCode > 0) {
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
            errorMessage = "Stack Advisor reported an error: " + errMessage;
          } else {
            errorMessage = "Error occurred during stack advisor execution";
          }
          errorMessage += "\nStdOut file: " + outputFile + "\n";
          errorMessage += "\nStdErr file: " + errorFile;
          switch (exitCode) {
            case 1:
              throw new StackAdvisorRequestException(errorMessage);
            case 2:
              throw new StackAdvisorException(errorMessage);
          }
        }
      } finally {
        process.destroy();
      }
    } catch (StackAdvisorException ex) {
      throw ex;
    } catch (Exception ioe) {
      String message = "Error executing stack advisor: ";
      LOG.error(message, ioe);
      throw new StackAdvisorException(message + ioe.getMessage());
    }
  }

  /**
   * Gets an instance of a {@link ProcessBuilder} that's ready to execute the
   * shell command to run the stack advisor script. This will take the
   * environment variables from the current process.
   *
   * @param script
   * @param saCommandType
   * @param actionDirectory
   * @param outputFile
   * @param errorFile
   * @return
   */
  ProcessBuilder prepareShellCommand(String script,
      StackAdvisorCommandType saCommandType,
      File actionDirectory, String outputFile, String errorFile) {
    String hostsFile = actionDirectory + File.separator + "hosts.json";
    String servicesFile = actionDirectory + File.separator + "services.json";

    // includes the original command plus the arguments for it
    List<String> builderParameters = new ArrayList<String>();
    if (System.getProperty("os.name").contains("Windows")) {
      builderParameters.add("cmd");
      builderParameters.add("/c");
    } else {
      builderParameters.add("sh");
      builderParameters.add("-c");
    }

    // for the 3rd argument, build a single parameter since we use -c
    // ProcessBuilder doesn't support output redirection until JDK 1.7
    String commandStringParameters[] = new String[] { script,
        saCommandType.toString(), hostsFile,
        servicesFile, "1>", outputFile, "2>", errorFile };

    StringBuilder commandString = new StringBuilder();
    for (String command : commandStringParameters) {
      commandString.append(command).append(" ");
    }

    builderParameters.add(commandString.toString());

    LOG.debug("Stack advisor command is {}", builderParameters);

    return new ProcessBuilder(builderParameters);
  }
}
