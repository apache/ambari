/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder.docker.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Run 'docker ps -a -q' (+ logfeeder type filter) and save the response in a string list (container ids)
 */
public class DockerListContainerCommand implements ContainerCommand<List<String>> {

  private static final Logger logger = LoggerFactory.getLogger(DockerListContainerCommand.class);

  @Override
  public List<String> execute(Map<String, String> params) {
    CommandResponse commandResponse = null;
    List<String> commandList = new ArrayList<>();
    commandList.add("/usr/local/bin/docker");
    commandList.add("ps");
    commandList.add("-a");
    commandList.add("-q");
    // TODO: add --filter="label=logfeeder.log.type"
    try {
      commandResponse = CommandExecutionHelper.executeCommand(commandList, null);
      if (commandResponse.getExitCode() != 0) {
        logger.error("Error during inspect containers request: {} (exit code: {})", commandResponse.getStdErr(), commandResponse.getExitCode());
      }
    } catch (Exception e) {
      logger.error("Error during inspect containers request", e);
    }
    return commandResponse != null ? commandResponse.getStdOut() : null;
  }
}
