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
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.bootstrap.BSResponse.BSRunStat;
import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BootStrapImpl {
  private Configuration conf;
  private File bootStrapDir;
  private String bootScript;
  private BSRunner bsRunner;
  long timeout;

  private static Log LOG = LogFactory.getLog(BootStrapImpl.class);

  /* Monotonically increasing requestid for the bootstrap api to query on */
  int requestId = 0;
  private FifoLinkedHashMap<Long, BootStrapStatus> bsStatus;


  @Inject
  public BootStrapImpl(Configuration conf) {
    this.conf = conf;
    this.bootStrapDir = conf.getBootStrapDir();
    this.bootScript = conf.getBootStrapScript();
    this.bsStatus = new FifoLinkedHashMap<Long, BootStrapStatus>();
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

  /**
   * update status of a request. Mostly called by the status collector thread.
   * @param requestId the request id.
   * @param status the status of the update.
   */
  private synchronized void updateStatus(long requestId, BootStrapStatus status) {
    bsStatus.put(requestId, status);
  }


  /**
   * Run the bs script to ssh to a list of hosts
   * with a ssh key.
   */
  class BSRunner extends Thread {
    private  boolean finished = false;
    private SshHostInfo sshHostInfo;
    private File bootDir;
    private String bsScript;
    private File requestIdDir;
    private File sshKeyFile;
    int requestId;

    public BSRunner(SshHostInfo sshHostInfo, String bootDir, String bsScript,
        int requestId, long timeout)
    {
      this.requestId = requestId;
      this.sshHostInfo = sshHostInfo;
      this.bsScript = bsScript;
      this.bootDir = new File(bootDir);
      this.requestIdDir = new File(bootDir, Integer.toString(requestId));
      this.sshKeyFile = new File(this.requestIdDir, "sshKey");
      BootStrapStatus status = new BootStrapStatus();
      status.setLog("RUNNING");
      status.setStatus(BSStat.RUNNING);
      BootStrapImpl.this.updateStatus(requestId, status);
    }

    /**
     * Update the gathered data from reading output
     *
     */
    private class BSStatusCollector implements Runnable {
      @Override
      public void run() {
        BSHostStatusCollector collector = new BSHostStatusCollector(requestIdDir,
            sshHostInfo.getHosts());
        collector.run();
        List<BSHostStatus> hostStatus = collector.getHostStatus();
        BootStrapStatus status = new BootStrapStatus();
        status.setHostsStatus(hostStatus);
        status.setLog("");
        status.setStatus(BSStat.RUNNING);
        BootStrapImpl.this.updateStatus(requestId, status);
      }
    }

    private String createHostString(List<String> list) {
      StringBuilder ret = new StringBuilder();
      if (list == null) {
        return "";
      }
      for (String host: list) {
        ret.append(host).append(",");
      }
      return ret.toString();
    }

    /** Create request id dir for each bootstrap call **/
    private void createRunDir() throws IOException {
      if (!bootDir.exists()) {
        // create the bootdir directory.
        if (! bootDir.mkdirs()) {
          throw new IOException("Cannot create " + bootDir);
        }
      }
      /* create the request id directory */
      if (requestIdDir.exists()) {
        /* delete the directory and make sure we start back */
        FileUtils.deleteDirectory(requestIdDir);
      }
      /* create the directory for the run dir */
      if (! requestIdDir.mkdirs()) {
        throw new IOException("Cannot create " + requestIdDir);
      }
    }

    private void writeSshKeyFile(String data) throws IOException {
      FileUtils.writeStringToFile(sshKeyFile, data);
    }

    public synchronized void finished() {
      this.finished = true;
    }

    @Override
    public void run() {
      String hostString = createHostString(sshHostInfo.getHosts());
      String commands[] = new String[4];
      BSStat stat = BSStat.RUNNING;
      String scriptlog = "";
      try {
        createRunDir();
        writeSshKeyFile(sshHostInfo.getSshKey());
        /* Running command:
         * script hostlist bsdir sshkeyfile
         */
        commands[0] = this.bsScript;
        commands[1] = hostString;
        commands[2] = this.requestIdDir.toString();
        commands[3] = this.sshKeyFile.toString();
        LOG.info("Host= " + hostString + " bs=" + this.bsScript + " requestDir=" +
            requestIdDir + " keyfile=" + this.sshKeyFile);
        Process process = Runtime.getRuntime().exec(commands);
        /** Startup a scheduled executor service to look through the logs
         */
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        BSStatusCollector statusCollector = new BSStatusCollector();
        ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(statusCollector,
            0, 10, TimeUnit.SECONDS);
        LOG.info("Kicking off the scheduler for polling on logs in " +
            this.requestIdDir);
        try {
          int exitCode = process.waitFor();
          StringWriter writer_1 = new StringWriter();
          IOUtils.copy(process.getInputStream(), writer_1);
          String outMesg = writer_1.toString();
          //if (outMesg == null)  outMesg = "";
          StringWriter writer_2 = new StringWriter();
          IOUtils.copy(process.getErrorStream(), writer_2);
          String errMesg = writer_2.toString();
          //if (errMesg == null)  errMesg = "";
          scriptlog = outMesg + "\n" + errMesg;
          if (exitCode != 0) {
            stat = BSStat.ERROR;
          } else {
            stat = BSStat.SUCCESS;
          }
        } catch (InterruptedException e) {
          throw new IOException(e);
        } finally {
          handle.cancel(true);
          /* schedule a last update */
          scheduler.schedule(new BSStatusCollector(), 0, TimeUnit.SECONDS);
          scheduler.shutdownNow();
          try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            LOG.info("Interruped while waiting for scheduler");
          }
          process.destroy();
        }
      } catch(IOException io) {
        LOG.info("Error executing bootstrap ", io);
      } finally {
        /* get the bstatus */
        BootStrapStatus tmpStatus = getStatus(requestId);
        tmpStatus.setLog(scriptlog);
        tmpStatus.setStatus(stat);
        updateStatus(requestId, tmpStatus);
        finished();
      }
    }

    public synchronized boolean isRunning() {
      return !this.finished;
    }
  }

  public synchronized void init() throws IOException {
    if (!bootStrapDir.exists()) {
      boolean mkdirs = bootStrapDir.mkdirs();
      if (!mkdirs) throw new IOException("Unable to make directory for " +
          "bootstrap " + bootStrapDir);
    }
  }

  public  synchronized BSResponse runBootStrap(SshHostInfo info) {
    BSResponse response = new BSResponse();
    /* Run some checks for ssh host */
    LOG.info("BootStrapping hosts " + info.hostListAsString());
    if (bsRunner != null) {
      response.setLog("BootStrap in Progress: Cannot Run more than one.");
      response.setStatus(BSRunStat.ERROR);
      return response;
    }
    requestId++;

    bsRunner = new BSRunner(info, bootStrapDir.toString(),
        bootScript, requestId, 0L);
    bsRunner.start();
    response.setStatus(BSRunStat.OK);
    response.setLog("Running Bootstrap now.");
    response.setRequestId(requestId);
    return response;
  }

}
