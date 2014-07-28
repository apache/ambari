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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;

@Singleton
public class StackAdvisorRunner {

  private static Log LOG = LogFactory.getLog(StackAdvisorRunner.class);

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
    LOG.info(String.format("Script=%s, actionDirectory=%s, command=%s", script, actionDirectory,
        saCommand));

    String outputFile = actionDirectory + File.separator + "stackadvisor.out";
    String errorFile = actionDirectory + File.separator + "stackadvisor.err";

    String shellCommand[] = prepareShellCommand(script, saCommand, actionDirectory, outputFile,
        errorFile);
    String[] env = new String[] {};

    try {
      Process process = Runtime.getRuntime().exec(shellCommand, env);

      try {
        LOG.info(String.format("Stack-advisor output=%s, error=%s", outputFile, errorFile));

        int exitCode = process.waitFor();
        try {
          String outMessage = FileUtils.readFileToString(new File(outputFile));
          String errMessage = FileUtils.readFileToString(new File(errorFile));
          LOG.info("Script log message: " + outMessage + "\n\n" + errMessage);
        } catch (IOException io) {
          LOG.info("Error in reading script log files", io);
        }

        return exitCode == 0;
      } finally {
        process.destroy();
      }
    } catch (Exception io) {
      LOG.info("Error executing stack advisor " + io.getMessage());
      return false;
    }
  }

  private String[] prepareShellCommand(String script, StackAdvisorCommand saCommand,
      File actionDirectory, String outputFile, String errorFile) {
    String hostsFile = actionDirectory + File.separator + "hosts.json";
    String servicesFile = actionDirectory + File.separator + "services.json";

    String shellCommand[] = new String[] { "sh", "-c", null /* to be calculated */};
    String commands[] = new String[] { script, saCommand.toString(), hostsFile, servicesFile };

    StringBuilder commandString = new StringBuilder();
    for (String command : commands) {
      commandString.append(" " + command);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(commandString);
    }

    commandString.append(" 1> " + outputFile + " 2>" + errorFile);
    shellCommand[2] = commandString.toString();

    return shellCommand;
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
