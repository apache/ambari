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

import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ncole
 *
 */
class BSRunner extends Thread {
  private static Log LOG = LogFactory.getLog(BSRunner.class);

  private  boolean finished = false;
  private SshHostInfo sshHostInfo;
  private File bootDir;
  private String bsScript;
  private File requestIdDir;
  private File sshKeyFile;
  private int requestId;
  private String agentSetupScript;
  private String agentSetupPassword;
  private String ambariHostname;
  private boolean verbose;
  private BootStrapImpl bsImpl;

  public BSRunner(BootStrapImpl impl, SshHostInfo sshHostInfo, String bootDir,
      String bsScript, String agentSetupScript, String agentSetupPassword,
      int requestId, long timeout, String hostName, boolean isVerbose)
  {
    this.requestId = requestId;
    this.sshHostInfo = sshHostInfo;
    this.bsScript = bsScript;
    this.bootDir = new File(bootDir);
    this.requestIdDir = new File(bootDir, Integer.toString(requestId));
    this.sshKeyFile = new File(this.requestIdDir, "sshKey");
    this.agentSetupScript = agentSetupScript;
    this.agentSetupPassword = agentSetupPassword;
    this.ambariHostname = hostName;
    this.verbose = isVerbose;
    this.bsImpl = impl;
    BootStrapStatus status = new BootStrapStatus();
    status.setLog("RUNNING");
    status.setStatus(BSStat.RUNNING);
    bsImpl.updateStatus(requestId, status);
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
      bsImpl.updateStatus(requestId, status);
    }
  }

  private String createHostString(List<String> list) {
    StringBuilder ret = new StringBuilder();
    if (list == null) {
      return "";
    }

    int i = 0;
    for (String host: list) {
      ret.append(host);
      if (i++ != list.size()-1)
        ret.append(",");
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
    String commands[] = new String[6];
    String shellCommand[] = new String[3];
    BSStat stat = BSStat.RUNNING;
    String scriptlog = "";
    try {
      createRunDir();
      if (LOG.isDebugEnabled()) {
        // FIXME needs to be removed later
        // security hole
        LOG.debug("Using ssh key=\""
            + sshHostInfo.getSshKey() + "\"");
      }

      writeSshKeyFile(sshHostInfo.getSshKey());
      /* Running command:
       * script hostlist bsdir sshkeyfile
       */
      shellCommand[0] = "sh";
      shellCommand[1] = "-c";
      
      commands[0] = this.bsScript;
      commands[1] = hostString;
      commands[2] = this.requestIdDir.toString();
      commands[3] = this.sshKeyFile.toString();
      commands[4] = this.agentSetupScript.toString();
      commands[5] = this.ambariHostname;
      LOG.info("Host= " + hostString + " bs=" + this.bsScript + " requestDir=" +
          requestIdDir + " keyfile=" + this.sshKeyFile + " server=" + this.ambariHostname);

      String[] env = new String[] { "AMBARI_PASSPHRASE=" + agentSetupPassword };
      if (this.verbose)
        env = new String[] { env[0], " BS_VERBOSE=\"-vvv\" " };

      StringBuilder commandString = new StringBuilder();
      for (String comm : commands) {
        commandString.append(" " + comm);
      }   
     
      if (LOG.isDebugEnabled()) {
        LOG.debug(commandString);
      }
      
      String bootStrapOutputFile = requestIdDir + File.separator + "bootstrap.out";
      String bootStrapErrorFile = requestIdDir + File.separator + "bootstrap.err";
      commandString.append(
          " 1> " + bootStrapOutputFile + " 2>" + bootStrapErrorFile);
      
      shellCommand[2] = commandString.toString();
      Process process = Runtime.getRuntime().exec(shellCommand, env);

      /** Startup a scheduled executor service to look through the logs
       */
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      BSStatusCollector statusCollector = new BSStatusCollector();
      ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(statusCollector,
          0, 10, TimeUnit.SECONDS);
      LOG.info("Kicking off the scheduler for polling on logs in " +
          this.requestIdDir);
      try {

        LOG.info("Bootstrap output, log="
              + bootStrapErrorFile + " " + bootStrapOutputFile);
        int exitCode = process.waitFor();
        String outMesg = "";
        String errMesg = "";
        try {
          outMesg = FileUtils.readFileToString(new File(bootStrapOutputFile));
          errMesg = FileUtils.readFileToString(new File(bootStrapErrorFile));
        } catch(IOException io) {
          LOG.info("Error in reading files ", io);
        }
        scriptlog = outMesg + "\n\n" + errMesg;
        LOG.info("Script log Mesg " + scriptlog);
        if (exitCode != 0) {
          stat = BSStat.ERROR;
        } else {
          stat = BSStat.SUCCESS;
        }

        scheduler.schedule(new BSStatusCollector(), 0, TimeUnit.SECONDS);
        long startTime = System.currentTimeMillis();
        while (true) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Waiting for hosts status to be updated");
          }
          boolean pendingHosts = false;
          BootStrapStatus tmpStatus = bsImpl.getStatus(requestId);
          for (BSHostStatus status : tmpStatus.getHostsStatus()) {
            if (status.getStatus().equals("RUNNING")) {
              pendingHosts = true;
            }
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Whether hosts status yet to be updated, pending="
                + pendingHosts);
          }
          if (!pendingHosts) {
            break;
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // continue
          }
          long now = System.currentTimeMillis();
          if (now >= (startTime+15000)) {
            LOG.warn("Gave up waiting for hosts status to be updated");
            break;
          }
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
      LOG.info("Error executing bootstrap " + io.getMessage());
      stat = BSStat.ERROR;
    } finally {
      /* get the bstatus */
      BootStrapStatus tmpStatus = bsImpl.getStatus(requestId);
      tmpStatus.setLog(scriptlog);
      tmpStatus.setStatus(stat);
      bsImpl.updateStatus(requestId, tmpStatus);
      bsImpl.reset();
      finished();
    }
  }

  public synchronized boolean isRunning() {
    return !this.finished;
  }
}
