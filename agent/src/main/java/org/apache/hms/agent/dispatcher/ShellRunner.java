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

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.agent.Agent;
import org.apache.hms.common.entity.ScriptCommand;
import org.apache.hms.common.rest.Response;
import org.apache.hms.common.util.ExceptionUtil;

public class ShellRunner {
  private static Log log = LogFactory.getLog(ShellRunner.class);

  public Response run(ScriptCommand cmd) {
    Response r = new Response();
    StringBuilder stdout = new StringBuilder();
    StringBuilder errorBuffer = new StringBuilder();
    Process proc;
    try {
      String[] parameters = cmd.getParms();
      int size = 0;
      if(parameters!=null) {
        size = parameters.length;
      }
      String[] cmdArray = new String[size+1];
      cmdArray[0]=cmd.getScript();
      for(int i=0;i<size;i++) {
        cmdArray[i+1]=parameters[i];      
      }
      proc = Runtime.getRuntime().exec(cmdArray);
      DataInputStream in = new DataInputStream(proc.getInputStream());
      DataInputStream err = new DataInputStream(proc.getErrorStream());
      String str;
      while ((str = in.readLine()) != null) {
        stdout.append(str);
        stdout.append("\n");
      }
      while ((str = err.readLine()) != null) {
        errorBuffer.append(str);
        errorBuffer.append("\n");
      }
      int exitCode = proc.waitFor();
      r.setCode(exitCode);
      r.setError(errorBuffer.toString());
      r.setOutput(stdout.toString());
    } catch (Exception e) {
      r.setCode(1);
      r.setError(ExceptionUtil.getStackTrace(e));
      log.error(ExceptionUtil.getStackTrace(e));
    }
    log.info(cmd);
    log.info(r);
    return r;
  }

}
