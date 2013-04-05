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
package org.apache.ambari.server.actionmanager;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class ExecutionCommandWrapper {
  private static Log LOG = LogFactory.getLog(ExecutionCommandWrapper.class);
  String jsonExecutionCommand = null;
  ExecutionCommand executionCommand = null;

  public ExecutionCommandWrapper(String jsonExecutionCommand) {
    this.jsonExecutionCommand = jsonExecutionCommand;
  }

  public ExecutionCommandWrapper(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  public ExecutionCommand getExecutionCommand() {
    if (executionCommand != null) {
      return executionCommand;
    } else if (jsonExecutionCommand != null) {
      try {
        executionCommand = StageUtils.stringToExecutionCommand(jsonExecutionCommand);
        return executionCommand;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string"
              + " representations are null");
    }
  }

  public String getJson() {
    if (jsonExecutionCommand != null) {
      return jsonExecutionCommand;
    } else if (executionCommand != null) {
      try {
        jsonExecutionCommand = StageUtils.jaxbToString(executionCommand);
        return jsonExecutionCommand;
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException(
          "Invalid ExecutionCommandWrapper, both object and string"
              + " representations are null");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ExecutionCommandWrapper wrapper = (ExecutionCommandWrapper) o;
    
    if (executionCommand != null && wrapper.executionCommand != null) {
      return executionCommand.equals(wrapper.executionCommand);
    } else {
      return getJson().equals(wrapper.getJson());
    }
  }

  @Override
  public int hashCode() {
    if (executionCommand != null) {
      return executionCommand.hashCode();
    } else if (jsonExecutionCommand != null) {
      return jsonExecutionCommand.hashCode();
    }
    throw new RuntimeException("Invalid Wrapper object");
  }

  void invalidateJson() {
    if (executionCommand == null) {
      throw new RuntimeException("Invalid Wrapper object");
    }
    jsonExecutionCommand = null;
  }
}
