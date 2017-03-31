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
package org.apache.ambari.logsearch.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface ServiceLogData extends CommonLogData, ComponentTypeLogData, HostLogData {

  @JsonProperty("level")
  String getLevel();

  void setLevel(String level);

  @JsonProperty("line_number")
  Integer getLineNumber();

  void setLineNumber(Integer lineNumber);

  @JsonProperty("logtime")
  Date getLogTime();

  void setLogTime(Date logTime);

  @JsonProperty("ip")
  String getIp();

  void setIp(String ip);

  @JsonProperty("path")
  String getPath();

  void setPath(String path);

  @JsonProperty("type")
  String getType();

  void setType(String type);

  @JsonProperty("host")
  String getHost();

  void setHost(String host);
}
