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

package org.apache.hms.agent.dispatcher;

import org.apache.hms.common.entity.ScriptCommand;
import org.apache.hms.common.entity.agent.DaemonAction;
import org.apache.hms.common.rest.Response;

public class DaemonRunner {
  
  public Response startDaemon(DaemonAction dc) {
    ScriptCommand cmd = new ScriptCommand();
    StringBuilder sb = new StringBuilder();
    sb.append("/etc/init.d/");
    sb.append(dc.getDaemonName());
    cmd.setScript(sb.toString());
    String[] parms = new String[1];
    parms[0] = "start";
    cmd.setParms(parms);
    ShellRunner shell = new ShellRunner();
    return shell.run(cmd);
  }
  
  public Response stopDaemon(DaemonAction dc) {
    ScriptCommand cmd = new ScriptCommand();
    StringBuilder sb = new StringBuilder();
    sb.append("/etc/init.d/");
    sb.append(dc.getDaemonName());
    cmd.setScript(sb.toString());
    String[] parms = new String[1];
    parms[0] = "stop";
    cmd.setParms(parms);
    ShellRunner shell = new ShellRunner();
    return shell.run(cmd);
  }
  
  public Response checkDaemon(DaemonAction dc) {
    ScriptCommand cmd = new ScriptCommand();
    StringBuilder sb = new StringBuilder();
    sb.append("/etc/init.d/");
    sb.append(dc.getDaemonName());
    cmd.setScript(sb.toString());
    String[] parms = new String[1];
    parms[0] = "status";
    cmd.setParms(parms);
    ShellRunner shell = new ShellRunner();
    return shell.run(cmd);
  }
}
