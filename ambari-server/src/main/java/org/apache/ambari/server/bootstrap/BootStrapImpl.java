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

package org.apache.ambari.server.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.bootstrap.BSResponse.BSRunStat;
import org.apache.ambari.server.configuration.Configuration;
import org.mortbay.log.Log;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BootStrapImpl {
  private Configuration conf;
  private File bootStrapDir;
  private String bootScript;
  private BSRunner bsRunner;
 
  /* Monotonically increasing requestid for the bootstrap api to query on */
  AtomicLong requestId = new AtomicLong();
  private static final int MAX_ENTRIES = 100;
  private FifoLinkedHashMap<Long, BootStrapStatus> bsStatus;

  /**
   * Only Store the Last 100 BootStrap Status'es
   *
   */
  @SuppressWarnings("serial")
  public static class FifoLinkedHashMap<K, V> extends 
  LinkedHashMap<K, V> {
    protected boolean removeEldestEntry(Map.Entry<K, 
        V> eldest) {
      return size() > MAX_ENTRIES;
    }

  }
  
  /**
   * Return {@link BootStrapStatus} for a given responseId.
   * @param requestId the responseId for which the status needs to be returned.
   * @return status for a specific response id. A response Id of -1 means the 
   * latest responseId.
   */
  public synchronized BootStrapStatus getStatus(long requestId) {
    if (! bsStatus.containsKey(requestId)) {
      return null;
    }
    return bsStatus.get(requestId);
  }
  
  class BSRunner extends Thread {
    private  boolean finished = false;
    private SshHostInfo sshHostInfo;
    private String bootDir;
    private String bsString;
    private String bsScript;
    int requestId;
    public BSRunner(SshHostInfo sshHostInfo, String bootDir, String bsScript,
        int requestId)
    {
      this.requestId = requestId;
      this.sshHostInfo = sshHostInfo;
      this.bsString = bsString;
      this.bsScript = bsScript;
    }
    String[] command = new String[3];
    
    @Override
    public synchronized void run() {
      
    }

    public synchronized boolean isRunning() {
      return !this.finished;
    }
  }

  @Inject
  public BootStrapImpl(Configuration conf) {
    this.conf = conf;
    this.bootStrapDir = conf.getBootStrapDir();
    this.bootScript = conf.getBootStrapScript();   
    this.bsStatus = new FifoLinkedHashMap<Long, BootStrapStatus>();
  }

  public synchronized void init() throws IOException {
    boolean mkdirs = bootStrapDir.mkdirs();
    if (!mkdirs) throw new IOException("Unable to make directory for " +
        "bootstrap " + bootStrapDir);

  }

  public  synchronized BSResponse runBootStrap(SshHostInfo info) {
    BSResponse response = new BSResponse();
    /* Run some checks for ssh host */
    Log.info("BootStrapping hosts " + info.hostListAsString());
    if (bsRunner != null) {
      response.setLog("BootStrap in Progress: Cannot Run more than one.");
      response.setStatus(BSRunStat.ERROR);
      return response;
    } 
    
    
    response.setStatus(BSRunStat.OK);
    response.setLog("Running Bootstrap now.");
    return response;
  }

}
