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

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Agent environment data.
 */
public class AgentEnv {

  /**
   * Various directories, configurable in <code>ambari-agent.ini</code>
   */
  private Directory[] paths = new Directory[0];

  /**
   * Java processes running on the system.  Default empty array.
   */
  private JavaProc[] javaProcs = new JavaProc[0];
  
  /**
   * Various RPM package versions.
   */
  private Rpm[] rpms = new Rpm[0];
  
  /**
   * Number of pid files found in <code>/var/run/hadoop</code>
   */
  private int varRunHadoopPidCount = 0;
  
  /**
   * Number of log files found in <code>/var/log/hadoop</code>
   */
  private int varLogHadoopLogCount = 0;

  /**
   * Directories that match name <code>/etc/alternatives/*conf</code>
   */
  private Alternative[] etcAlternativesConf = new Alternative[0];

  /**
   * Output for repo listing.  Command to do this varies, but for RHEL it is
   * <code>yum -C repolist</code>
   */
  private String repoInfo;
  

  public Directory[] getPaths() {
      return paths;
  }
  
  public void setPaths(Directory[] dirs) {
    paths = dirs;
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
  
  public void setJavaProcs(JavaProc[] procs) {
    javaProcs = procs;
  }
  
  public JavaProc[] getJavaProcs() {
    return javaProcs;
  }
  
  public void setRpms(Rpm[] rpm) {
    rpms = rpm;
  }
  
  public Rpm[] getRpms() {
    return rpms;
  }
  
  public void setEtcAlternativesConf(Alternative[] dirs) {
    etcAlternativesConf = dirs;
  }
  
  public Alternative[] getEtcAlternativesConf() {
    return etcAlternativesConf;
  }
  
  public void setRepoInfo(String info) {
    repoInfo = info;
  }
  
  public String getRepoInfo() {
    return repoInfo;
  }
  
  /**
   * Represents information about rpm-installed packages
   */
  public static class Rpm {
    private String rpmName;
    private boolean rpmInstalled = false;
    private String rpmVersion;
    
    public void setName(String name) {
      rpmName = name;
    }
    
    public String getName() {
      return rpmName;
    }
    
    public void setInstalled(boolean installed) {
      rpmInstalled = installed;
    }
    
    public boolean isInstalled() {
      return rpmInstalled;
    }
    
    public void setVersion(String version) {
      rpmVersion = version;
    }
    
    @JsonSerialize(include=Inclusion.NON_NULL)
    public String getVersion() {
      return rpmVersion;
    }
  }
  
  /**
   * Represents information about a directory of interest.
   */
  public static class Directory {
    private String dirName;
    private String dirType;
    
    public void setName(String name) {
      dirName = name;
    }
    
    public String getName() {
      return dirName;
    }
    
    public void setType(String type) {
      dirType = type;
    }
    
    public String getType() {
      return dirType;
    }
  }
  
  /**
   * Represents information about running java processes.
   */
  public static class JavaProc {
    private String user;
    private int pid = 0;
    private boolean is_hadoop = false;
    private String command;
    
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
    
    public void setHadoop(boolean hadoop) {
      is_hadoop = hadoop;
    }
    
    public boolean isHadoop() {
      return is_hadoop;
    }
    
    public void setCommand(String cmd) {
      command = cmd;
    }
    
    public String getCommand() {
      return command;
    }
  }
  
  public static class Alternative {
    private String altName;
    private String altTarget;
    
    public void setName(String name) {
      altName = name;
    }
    
    public String getName() {
      return altName;
    }
    
    public void setTarget(String target) {
      altTarget = target;
    }
    
    public String getTarget() {
      return altTarget;
    }
  }
  
}
