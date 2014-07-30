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
   * @param saCommand {@link StackAdvisorCommand} to run.
   * @param actionDirectory directory for the action
   * @return {@code true} if script completed successfully, {@code false}
   *         otherwise.
   */
  public boolean runScript(String script, StackAdvisorCommand saCommand, File actionDirectory) {
    LOG.info("Script={}, actionDirectory={}, command={}", script,
        actionDirectory, saCommand);

    String outputFile = actionDirectory + File.separator + "stackadvisor.out";
    String errorFile = actionDirectory + File.separator + "stackadvisor.err";

    ProcessBuilder builder = prepareShellCommand(script, saCommand,
        actionDirectory, outputFile,
        errorFile);

    try {
      Process process = builder.start();

      try {
        LOG.info("Stack-advisor output={}, error={}", outputFile, errorFile);

        int exitCode = process.waitFor();
        try {
          String outMessage = FileUtils.readFileToString(new File(outputFile));
          String errMessage = FileUtils.readFileToString(new File(errorFile));
          LOG.info("Stack advisor output files");
          LOG.info("    advisor script stdout: {}", outMessage);
          LOG.info("    advisor script stderr: {}", errMessage);
        } catch (IOException io) {
          LOG.error("Error in reading script log files", io);
        }

        return exitCode == 0;
      } finally {
        process.destroy();
      }
    } catch (Exception ioe) {
      LOG.error("Error executing stack advisor", ioe);
      return false;
    }
  }

  /**
   * Gets an instance of a {@link ProcessBuilder} that's ready to execute the
   * shell command to run the stack advisor script. This will take the
   * environment variables from the current process.
   * 
   * @param script
   * @param saCommand
   * @param actionDirectory
   * @param outputFile
   * @param errorFile
   * @return
   */
  private ProcessBuilder prepareShellCommand(String script,
      StackAdvisorCommand saCommand,
      File actionDirectory, String outputFile, String errorFile) {
    String hostsFile = actionDirectory + File.separator + "hosts.json";
    String servicesFile = actionDirectory + File.separator + "services.json";

    // includes the original command plus the arguments for it
    List<String> builderParameters = new ArrayList<String>();
    builderParameters.add("sh");
    builderParameters.add("-c");

    // for the 3rd argument, build a single parameter since we use -c
    // ProcessBuilder doesn't support output redirection until JDK 1.7
    String commandStringParameters[] = new String[] { script,
        saCommand.toString(), hostsFile,
        servicesFile, "1>", outputFile, "2>", errorFile };

    StringBuilder commandString = new StringBuilder();
    for (String command : commandStringParameters) {
      commandString.append(command).append(" ");
    }

    builderParameters.add(commandString.toString());

    LOG.debug("Stack advisor command is {}", builderParameters);

    return new ProcessBuilder(builderParameters);
  }

  public enum StackAdvisorCommand {
    RECOMMEND_COMPONENT_LAYOUT("recommend-component-layout"),

    VALIDATE_COMPONENT_LAYOUT("validate-component-layout"),

    RECOMMEND_CONFIGURATIONS("recommend-configurations"),

    VALIDATE_CONFIGURATIONS("validate-configurations");

    private final String name;

    private StackAdvisorCommand(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
