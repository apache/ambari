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

package org.apache.ambari.view.pig.resources.jobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.resources.jobs.utils.JobPolling;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object that provides operations for templeton jobs
 * CRUD overridden to support
 */
public class JobResourceManager extends PersonalCRUDResourceManager<PigJob> {
  protected TempletonApi api;

  private final static Logger LOG =
      LoggerFactory.getLogger(JobResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  public JobResourceManager(ViewContext context) {
    super(PigJob.class, context);
    setupPolling();
  }

  /**
   * Get templeton api business delegate
   * @return templeton api business delegate
   */
  public TempletonApi getTempletonApi() {
    if (api == null) {
      api = connectToTempletonApi(context);
    }
    return api;
  }

  /**
   * Set templeton api business delegate
   * @param api templeton api business delegate
   */
  public void setTempletonApi(TempletonApi api) {
    this.api = api;
  }

  private void setupPolling() {
    List<PigJob> notCompleted = this.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        PigJob job = (PigJob) item;
        return job.isInProgress();
      }
    });

    for(PigJob job : notCompleted) {
      JobPolling.pollJob(context, job);
    }
  }

  @Override
  public PigJob create(PigJob object) {
    object.setStatus(PigJob.Status.SUBMITTING);
    PigJob job = super.create(object);
    LOG.debug("Submitting job...");

    try {
      submitJob(object);
    } catch (RuntimeException e) {
      object.setStatus(PigJob.Status.SUBMIT_FAILED);
      save(object);
      LOG.debug("Job submit FAILED");
      throw e;
    }
    LOG.debug("Job submit OK");
    object.setStatus(PigJob.Status.SUBMITTED);
    save(object);
    return job;
  }

  /**
   * Kill Templeton Job
   * @param object job object
   * @throws IOException network error
   */
  public void killJob(PigJob object) throws IOException {
    LOG.debug("Killing job...");

    try {
      getTempletonApi().killJob(object.getJobId());
    } catch (IOException e) {
      LOG.debug("Job kill FAILED");
      throw e;
    }
    LOG.debug("Job kill OK");
  }

  /**
   * Running job
   * @param job job bean
   */
  private void submitJob(PigJob job) {
    String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
    String statusdir = String.format(context.getProperties().get("dataworker.jobs.path") +
            "/%s/%s_%s", getUsername(),
        job.getTitle().toLowerCase().replaceAll("[^a-zA-Z0-9 ]+", "").replace(" ", "_"),
        date);

    String newPigScriptPath = statusdir + "/script.pig";
    String newSourceFilePath = statusdir + "/source.pig";
    String newPythonScriptPath = statusdir + "/udf.py";
    String templetonParamsFilePath = statusdir + "/params";
    try {
      // additional file can be passed to copy into work directory
      if (job.getSourceFileContent() != null && !job.getSourceFileContent().isEmpty()) {
        String sourceFileContent = job.getSourceFileContent();
        job.setSourceFileContent(null); // we should not store content in DB
        save(job);

        FSDataOutputStream stream = BaseService.getHdfsApi(context).create(newSourceFilePath, true);
        stream.writeBytes(sourceFileContent);
        stream.close();
      } else {
        if (job.getSourceFile() != null && !job.getSourceFile().isEmpty()) {
          // otherwise, just copy original file
          if (!BaseService.getHdfsApi(context).copy(job.getSourceFile(), newSourceFilePath)) {
            throw new ServiceFormattedException("Can't copy source file from " + job.getSourceFile() +
                " to " + newPigScriptPath);
          }
        }
      }
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create/copy source file: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create/copy source file: " + e.toString(), e);
    }

    try {
      // content can be passed from front-end with substituted arguments
      if (job.getForcedContent() != null && !job.getForcedContent().isEmpty()) {
        String forcedContent = job.getForcedContent();
        // variable for sourceFile can be passed from front-ent
        forcedContent = forcedContent.replace("${sourceFile}",
            context.getProperties().get("dataworker.defaultFs") + newSourceFilePath);
        job.setForcedContent(null); // we should not store content in DB
        save(job);

        FSDataOutputStream stream = BaseService.getHdfsApi(context).create(newPigScriptPath, true);
        stream.writeBytes(forcedContent);
        stream.close();
      } else {
        // otherwise, just copy original file
        if (!BaseService.getHdfsApi(context).copy(job.getPigScript(), newPigScriptPath)) {
          throw new ServiceFormattedException("Can't copy pig script file from " + job.getPigScript() +
              " to " + newPigScriptPath);
        }
      }
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create/copy pig script file: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create/copy pig script file: " + e.toString(), e);
    }

    if (job.getPythonScript() != null && !job.getPythonScript().isEmpty()) {
      try {
        if (!BaseService.getHdfsApi(context).copy(job.getPythonScript(), newPythonScriptPath)) {
          throw new ServiceFormattedException("Can't copy python udf script file from " + job.getPythonScript() +
              " to " + newPythonScriptPath);
        }
      } catch (IOException e) {
        throw new ServiceFormattedException("Can't create/copy python udf file: " + e.toString(), e);
      } catch (InterruptedException e) {
        throw new ServiceFormattedException("Can't create/copy python udf file: " + e.toString(), e);
      }
    }

    try {
      FSDataOutputStream stream = BaseService.getHdfsApi(context).create(templetonParamsFilePath, true);
      if (job.getTempletonArguments() != null) {
        stream.writeBytes(job.getTempletonArguments());
      }
      stream.close();
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create params file: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create params file: " + e.toString(), e);
    }
    job.setPigScript(newPigScriptPath);

    job.setStatusDir(statusdir);
    job.setDateStarted(System.currentTimeMillis() / 1000L);

    TempletonApi.JobData data = null;
    try {
      data = getTempletonApi().runPigQuery(new File(job.getPigScript()), statusdir, job.getTempletonArguments());
    } catch (IOException templetonBadResponse) {
      String msg = String.format("Templeton bad response: %s", templetonBadResponse.toString());
      LOG.debug(msg);
      throw new ServiceFormattedException(msg, templetonBadResponse);
    }
    job.setJobId(data.id);

    JobPolling.pollJob(context, job);
  }

  /**
   * Get job status
   * @param job job object
   */
  public void retrieveJobStatus(PigJob job) {
    TempletonApi.JobInfo info = null;
    try {
      info = getTempletonApi().checkJob(job.getJobId());
    } catch (IOException e) {
      LOG.warn(String.format("IO Exception: %s", e));
      return;
    }

    if (info.status != null && (info.status.containsKey("runState"))) {
      //TODO: retrieve from RM
      int runState = ((Double) info.status.get("runState")).intValue();
      switch (runState) {
        case PigJob.RUN_STATE_KILLED:
          LOG.debug(String.format("Job KILLED: %s", job.getJobId()));
          job.setStatus(PigJob.Status.KILLED);
          break;
        case PigJob.RUN_STATE_FAILED:
          LOG.debug(String.format("Job FAILED: %s", job.getJobId()));
          job.setStatus(PigJob.Status.FAILED);
          break;
        case PigJob.RUN_STATE_PREP:
        case PigJob.RUN_STATE_RUNNING:
          job.setStatus(PigJob.Status.RUNNING);
          break;
        case PigJob.RUN_STATE_SUCCEEDED:
          LOG.debug(String.format("Job COMPLETED: %s", job.getJobId()));
          job.setStatus(PigJob.Status.COMPLETED);
          break;
        default:
          LOG.debug(String.format("Job in unknown state: %s", job.getJobId()));
          job.setStatus(PigJob.Status.UNKNOWN);
          break;
      }
    }
    Pattern pattern = Pattern.compile("\\d+");
    Matcher matcher = null;
    if (info.percentComplete != null) {
      matcher = pattern.matcher(info.percentComplete);
    }
    if (matcher != null && matcher.find()) {
      job.setPercentComplete(Integer.valueOf(matcher.group()));
    } else {
      job.setPercentComplete(null);
    }
    save(job);
  }

  /**
   * Checks connection to WebHCat
   * @param context View Context
   */
  public static void webhcatSmokeTest(ViewContext context) {
    try {
      TempletonApi api = connectToTempletonApi(context);
      api.status();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private static TempletonApi connectToTempletonApi(ViewContext context) {
    String webhcatUrl = context.getProperties().get("dataworker.webhcat.url");
    if (webhcatUrl == null) {
      String message = "dataworker.webhcat.url is not configured!";
      LOG.error(message);
      throw new MisconfigurationFormattedException("dataworker.webhcat.url");
    }
    return new TempletonApi(context.getProperties().get("dataworker.webhcat.url"),
        getTempletonUser(context), getTempletonUser(context), context);
  }

  /**
   * Extension point to use different usernames in templeton
   * requests instead of logged in user
   * @return username in templeton
   */
  private static String getTempletonUser(ViewContext context) {
    String username = context.getProperties().get("dataworker.webhcat.user");
    if (username == null) {
      String message = "dataworker.webhcat.user is not configured!";
      LOG.error(message);
      throw new MisconfigurationFormattedException("dataworker.webhcat.user");
    }
    return username;
  }
}
