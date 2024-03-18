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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Run 'docker inspect' on container ids - and read response and convert it from json response to a map object
 */
public class DockerInspectContainerCommand implements ContainerCommand<List<Map<String, Object>>> {

  private static final Logger logger = LoggerFactory.getLogger(DockerInspectContainerCommand.class);

  @Override
  public List<Map<String, Object>> execute(Map<String, String> params) {
    List<String> containerIds = Arrays.asList(params.get("containerIds").split(","));
    CommandResponse commandResponse = null;
    List<Map<String, Object>> listResponse = new ArrayList<>();
    List<String> commandList = new ArrayList<>();
    commandList.add("/usr/local/bin/docker");
    commandList.add("inspect");
    commandList.addAll(containerIds);
    try {
      commandResponse = CommandExecutionHelper.executeCommand(commandList, null);
      if (commandResponse.getExitCode() != 0) {
        logger.error("Error during inspect containers request: {} (exit code: {})", commandResponse.getStdErr(), commandResponse.getExitCode());
      } else {
        String jsonResponse = StringUtils.join(commandResponse.getStdOut(), "");
        ObjectMapper mapper = new ObjectMapper();
        listResponse = mapper.readValue(jsonResponse, List.class);
      }
    } catch (Exception e) {
      logger.error("Error during inspect containers request", e);
    }
    return listResponse;
  }
}
