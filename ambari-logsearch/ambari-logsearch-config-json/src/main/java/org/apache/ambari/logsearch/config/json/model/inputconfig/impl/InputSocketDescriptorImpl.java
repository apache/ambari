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
package org.apache.ambari.logsearch.config.json.model.inputconfig.impl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputSocketDescriptor;

@ShipperConfigTypeDescription(
  name = "Socket Input",
  description = "Socket (TCP/UDP) inputs have the following parameters in addition to the general parameters:"
)
public class InputSocketDescriptorImpl extends InputDescriptorImpl implements InputSocketDescriptor {

  @ShipperConfigElementDescription(
    path = "/input/[]/port",
    type = "int",
    description = "Unique port for specific socket input",
    examples = {"61999"}
  )
  @Expose
  @SerializedName("port")
  private Integer port;

  @ShipperConfigElementDescription(
    path = "/input/[]/protocol",
    type = "int",
    description = "Protocol type for socket server (tcp / udp - udp is not supported right now)",
    examples = {"udp", "tcp"},
    defaultValue = "tcp"
  )
  @Expose
  @SerializedName("protocol")
  private String protocol;

  @ShipperConfigElementDescription(
    path = "/input/[]/secure",
    type = "boolean",
    description = "Use SSL",
    examples = {"true"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("secure")
  private Boolean secure;

  @ShipperConfigElementDescription(
    path = "/input/[]/log4j",
    type = "boolean",
    description = "Use Log4j serialized objects (e.g.: SocketAppender)",
    examples = {"true"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("log4j")
  private Boolean log4j;

  @Override
  public Integer getPort() {
    return this.port;
  }

  @Override
  public String getProtocol() {
    return this.protocol;
  }

  @Override
  public Boolean isSecure() {
    return this.secure;
  }

  @Override
  public Boolean isLog4j() {
    return this.log4j;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public void setLog4j(Boolean log4j) {
    this.log4j = log4j;
  }
}
