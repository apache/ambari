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
package org.apache.ambari.logsearch.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputSocketDescriptor;

@ApiModel
public class LSServerInputSocket extends LSServerInput {

  @JsonProperty("port")
  private Integer port;

  @JsonProperty("protocol")
  private String protocol;

  @JsonProperty("secure")
  private Boolean secure;

  @JsonProperty("log4j")
  private Boolean log4j;

  public LSServerInputSocket(InputDescriptor inputDescriptor) {
    super(inputDescriptor);
    InputSocketDescriptor inputSocketDescriptor = (InputSocketDescriptor) inputDescriptor;
    this.port = inputSocketDescriptor.getPort();
    this.protocol = inputSocketDescriptor.getProtocol();
    this.secure = inputSocketDescriptor.isSecure();
    this.log4j = inputSocketDescriptor.isLog4j();
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public Boolean getSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public Boolean getLog4j() {
    return log4j;
  }

  public void setLog4j(Boolean log4j) {
    this.log4j = log4j;
  }
}
