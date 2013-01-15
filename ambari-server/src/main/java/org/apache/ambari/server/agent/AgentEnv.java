/**
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
package org.apache.ambari.server.agent;

import java.util.Map;

/**
 * Agent environment data.
 */
public class AgentEnv {
  /**
   * Various directories in /etc
   */
  private Map<String, String> etcDirs = null;
  /**
   * Various directories in /var/run
   */
  private Map<String, String> varRunDirs = null;
  /**
   * Various directories in /var/log
   */
  private Map<String, String> varLogDirs = null;

  /**
   * Java processes with the word "hadoop" in them, users and pids
   */
  private JavaProc[] hadoopJavaProcs = null;
  /**
   * Number of pid files found in /var/run/hadoop
   */
  private int varRunHadoopPidCount = 0;
  /**
   * Number of log files found in /var/log/hadoop
   */
  private int varLogHadoopLogCount = 0;

  
  public Map<String, String> getEtcDirs() {
    return etcDirs;
  }
  
  public void setEtcDirs(Map<String, String> dirs) {
    etcDirs = dirs;
  }
  
  public Map<String, String> getVarRunDirs() {
    return varRunDirs;
  }
  
  public void setVarRunDirs(Map<String, String> dirs) {
    varRunDirs = dirs;
  }
  
  public Map<String, String> getVarLogDirs() {
    return varLogDirs;
  }
  
  public void setVarLogDirs(Map<String, String> dirs) {
    varLogDirs = dirs;
  }
  
  public void setVarRunHadoopPidCount(int count) {
    varRunHadoopPidCount = count;
  }
  
  public int getVarRunHadoopPidCount() {
    return varRunHadoopPidCount;
  }
  
  public void setVarLogHadoopLogCount(int count) {
    varLogHadoopLogCount = count;
  }
  
  public int getVarLogHadoopLogCount() {
    return varLogHadoopLogCount;
  }
  
  public void setHadoopJavaProcs(JavaProc[] procs) {
    hadoopJavaProcs = procs;
  }
  
  public JavaProc[] getHadoopJavaProcs() {
    return hadoopJavaProcs;
  }
  
  public static class JavaProc {
    private String user;
    private int pid;
    
    public void setUser(String user) {
      this.user = user;
    }
    
    public String getUser() {
      return user;
    }
    
    public void setPid(int pid) {
      this.pid = pid;
    }
    
    public int getPid() {
      return pid;
    }
  }
  
}
