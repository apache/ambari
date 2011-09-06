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

package org.apache.hms.common.entity.command;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.entity.command.Command;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@XmlRootElement
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@command")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER) 
@XmlType(name="", propOrder = {})
public class ClusterCommand extends Command {
  private ClusterManifest cm;
  private static Log LOG = LogFactory.getLog(ClusterCommand.class);

  public ClusterCommand() {
    this.cmd = CmdType.CREATE;
  }

  public ClusterCommand(String clusterName, ClusterManifest cm) {
    cm.setClusterName(clusterName);
    this.cm = cm;
  }
  
  public ClusterCommand(String clusterName) {
    ClusterManifest cm = new ClusterManifest();
    cm.setClusterName(clusterName);
    this.cm = cm;
  }
  
  public ClusterCommand(ClusterManifest cm) {
    this.cm = cm;
  }

  public ClusterManifest getClusterManifest() {
    try {
      this.cm.load();
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
    return this.cm;
  }
  
  public void setClusterManifest(ClusterManifest cm) {
    this.cm = cm;
  }
}
