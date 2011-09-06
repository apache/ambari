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
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.hms.common.entity.RestSource;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@XmlRootElement
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@command")
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public abstract class Command extends RestSource {
  @XmlElement
  protected String id;
  
  @XmlElement
  @XmlJavaTypeAdapter(CmdTypeAdapter.class)
  protected CmdType cmd;

  @XmlElement(name="dry-run")
  protected boolean dryRun = false;
  
  public String getId() {
    return id;
  }

  public CmdType getCmd() {
    return cmd;
  }

  public boolean getDryRun() {
    return dryRun;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public void setCmd(CmdType cmd) {
    this.cmd = cmd;
  }
  
  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("cmd=");
    sb.append(cmd);
    sb.append(", dry-run=");
    sb.append(dryRun);
    return sb.toString();
  }
  
  public static enum CmdType {
    CREATE("create"),
    DELETE("delete"),
    STATUS("status"),
    UPGRADE("upgrade");
    
    String cmd;
    private CmdType(String cmd) {
      this.cmd = cmd;
    }
    
    @Override
    public String toString() {
      return cmd;      
    }
  }
  
  public static class CmdTypeAdapter extends XmlAdapter<String, CmdType> {

    @Override
    public String marshal(CmdType obj) throws Exception {
      return obj.toString();
    }

    @Override
    public CmdType unmarshal(String str) throws Exception {
      for (CmdType j : CmdType.class.getEnumConstants()) {
        if (j.toString().equals(str)) {
          return j;
        }
      }
      throw new Exception("Can't convert " + str + " to " + CmdType.class.getName());
    }
    
  }

}
