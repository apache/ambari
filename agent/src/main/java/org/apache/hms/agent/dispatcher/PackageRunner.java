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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.agent.Agent;
import org.apache.hms.common.entity.PackageCommand;
import org.apache.hms.common.entity.PackageInfo;
import org.apache.hms.common.entity.ScriptCommand;
import org.apache.hms.common.entity.agent.DaemonAction;
import org.apache.hms.common.rest.Response;
import org.apache.hms.common.util.FileUtil;

public class PackageRunner {
  private static Log log = LogFactory.getLog(PackageRunner.class);

  public Response install(PackageCommand dc) {
    dc.setCmd("install");
    if(dc.getDryRun()) {
      return dryRun(dc);
    }
    return helper(dc);
  }
  
  public Response remove(PackageCommand dc) {
    dc.setCmd("erase");
    return helper(dc);
  }
  
  public Response query(PackageCommand dc) {
    dc.setCmd("info");
    return helper(dc);
  }
  
  private Response helper(PackageCommand dc) {
    ScriptCommand cmd = new ScriptCommand();
    cmd.setCmd(dc.getCmd());
    Response r = null;
    cmd.setScript("yum");
    String[] parms = null;
    if(dc.getPackages().length>0) {
      parms = new String[dc.getPackages().length+2];
      for(int i = 0; i< dc.getPackages().length;i++) {
        parms[i+2] = dc.getPackages()[i].getName();
      }
    }
    if(parms != null) {
      parms[0] = dc.getCmd();
      parms[1] = "-y";
      cmd.setParms(parms);
      ShellRunner shell = new ShellRunner();
      r = shell.run(cmd);
    } else {
      r = new Response();
      r.setCode(1);
      r.setError("Invalid package name");
    }
    return r;    
  }
  
  private Response dryRun(PackageCommand dc) {
    Response r = null;
    ScriptCommand cmd = new ScriptCommand();
    cmd.setCmd(dc.getCmd());
    cmd.setScript("yum");
    PackageInfo[] packages = dc.getPackages();
    String[] parms = new String[packages.length+4];
    parms[0] = "install";
    parms[1] = "-y";
    parms[2] = "--downloadonly";
    parms[3] = "--downloaddir=/tmp/system_update";
    for(int i=0;i<packages.length;i++) {
      parms[i+4] = packages[i].getName();
    }
    cmd.setParms(parms);
    ShellRunner shell = new ShellRunner();
    r = shell.run(cmd);
    if(r.getCode()!=1) {
      return r;
    } else {
      cmd.setScript("rpm");
      String[] rpmParms = new String[3];
      rpmParms[0] = "-i";
      rpmParms[1] = "--test";
      rpmParms[2] = "/tmp/system_update/*.rpm";
      cmd.setParms(rpmParms);
      r = shell.run(cmd);
      FileUtil.deleteDir(new File("/tmp/system_update"));
      return r;
    }
  }
}
