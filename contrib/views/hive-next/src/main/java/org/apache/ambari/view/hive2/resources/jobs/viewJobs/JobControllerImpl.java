/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive2.resources.jobs.viewJobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.ConnectionFactory;
import org.apache.ambari.view.hive2.ConnectionSystem;
import org.apache.ambari.view.hive2.actor.message.HiveJob;
import org.apache.ambari.view.hive2.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive2.client.AsyncJobRunner;
import org.apache.ambari.view.hive2.client.AsyncJobRunnerImpl;
import org.apache.ambari.view.hive2.client.ConnectionConfig;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.resources.jobs.ModifyNotificationDelegate;
import org.apache.ambari.view.hive2.resources.jobs.ModifyNotificationInvocationHandler;
import org.apache.ambari.view.hive2.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive2.resources.savedQueries.SavedQuery;
import org.apache.ambari.view.hive2.resources.savedQueries.SavedQueryResourceManager;
import org.apache.ambari.view.hive2.utils.BadRequestFormattedException;
import org.apache.ambari.view.hive2.utils.FilePaginator;
import org.apache.ambari.view.hive2.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

public class JobControllerImpl implements JobController, ModifyNotificationDelegate {
    private final static Logger LOG =
            LoggerFactory.getLogger(JobControllerImpl.class);

    private ViewContext context;
    private HdfsApi hdfsApi;
    private Job jobUnproxied;
    private Job job;
    private boolean modified;

    private SavedQueryResourceManager savedQueryResourceManager;
    private IATSParser atsParser;

    /**
     * JobController constructor
     * Warning: Create JobControllers ONLY using JobControllerFactory!
     */
    public JobControllerImpl(ViewContext context, Job job,
                             SavedQueryResourceManager savedQueryResourceManager,
                             IATSParser atsParser,
                             HdfsApi hdfsApi) {
        this.context = context;
        setJobPOJO(job);
        this.savedQueryResourceManager = savedQueryResourceManager;
        this.atsParser = atsParser;
        this.hdfsApi = hdfsApi;

    }

    public String getQueryForJob() {
        FilePaginator paginator = new FilePaginator(job.getQueryFile(), hdfsApi);
        String query;
        try {
            query = paginator.readPage(0);  //warning - reading only 0 page restricts size of query to 1MB
        } catch (IOException e) {
            throw new ServiceFormattedException("F030 Error when reading file " + job.getQueryFile(), e);
        } catch (InterruptedException e) {
            throw new ServiceFormattedException("F030 Error when reading file " + job.getQueryFile(), e);
        }
        return query;
    }

    private static final String DEFAULT_DB = "default";

    public String getJobDatabase() {
        if (job.getDataBase() != null) {
            return job.getDataBase();
        } else {
            return DEFAULT_DB;
        }
    }


    @Override
    public void submit() throws Throwable {
        String jobDatabase = getJobDatabase();
        String query = getQueryForJob();
        ConnectionSystem system = ConnectionSystem.getInstance();
        AsyncJobRunner asyncJobRunner = new AsyncJobRunnerImpl(context, system.getOperationController(context), system.getActorSystem());
        SQLStatementJob asyncJob = new SQLStatementJob(HiveJob.Type.ASYNC, getStatements(jobDatabase, query), context.getUsername(), job.getId(), job.getLogFile());
        asyncJobRunner.submitJob(getHiveConnectionConfig(), asyncJob, job);

    }

    private String[] getStatements(String jobDatabase, String query) {
        List<String> queries = Lists.asList("use " + jobDatabase, query.split(";"));
        List<String> cleansedQueries = FluentIterable.from(queries).transform(new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable String s) {
                return s.trim();
            }
        }).filter(new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String s) {
                return !StringUtils.isEmpty(s);
            }
        }).toList();
        return cleansedQueries.toArray(new String[0]);
    }


    @Override
    public void cancel() throws ItemNotFound {
      ConnectionSystem system = ConnectionSystem.getInstance();
      AsyncJobRunner asyncJobRunner = new AsyncJobRunnerImpl(context, system.getOperationController(context), system.getActorSystem());
      asyncJobRunner.cancelJob(job.getId(), context.getUsername());
    }

    @Override
    public void update() {
        updateJobDuration();
    }


    @Override
    public Job getJob() {
        return job;
    }

    /**
     * Use carefully. Returns unproxied bean object
     * @return unproxied bean object
     */
    @Override
    public Job getJobPOJO() {
        return jobUnproxied;
    }

    public void setJobPOJO(Job jobPOJO) {
        Job jobModifyNotificationProxy = (Job) Proxy.newProxyInstance(jobPOJO.getClass().getClassLoader(),
                new Class[]{Job.class},
                new ModifyNotificationInvocationHandler(jobPOJO, this));
        this.job = jobModifyNotificationProxy;

        this.jobUnproxied = jobPOJO;
    }


    @Override
    public void afterCreation() {
        setupStatusDirIfNotPresent();
        setupQueryFileIfNotPresent();
        setupLogFileIfNotPresent();

        setCreationDate();
    }

    public void setupLogFileIfNotPresent() {
        if (job.getLogFile() == null || job.getLogFile().isEmpty()) {
            setupLogFile();
        }
    }

    public void setupQueryFileIfNotPresent() {
        if (job.getQueryFile() == null || job.getQueryFile().isEmpty()) {
            setupQueryFile();
        }
    }

    public void setupStatusDirIfNotPresent() {
        if (job.getStatusDir() == null || job.getStatusDir().isEmpty()) {
            setupStatusDir();
        }
    }

    private static final long MillisInSecond = 1000L;

  public void updateJobDuration() {
    job.setDuration((System.currentTimeMillis() / MillisInSecond) - (job.getDateSubmitted() / MillisInSecond));
  }

  public void setCreationDate() {
    job.setDateSubmitted(System.currentTimeMillis());
  }

  private void setupLogFile() {
    LOG.debug("Creating log file for job#" + job.getId());

        String logFile = job.getStatusDir() + "/" + "logs";
        try {
            HdfsUtil.putStringToFile(hdfsApi, logFile, "");
        } catch (HdfsApiException e) {
            throw new ServiceFormattedException(e);
        }

        job.setLogFile(logFile);
        LOG.debug("Log file for job#" + job.getId() + ": " + logFile);
    }

    private void setupStatusDir() {
        String newDirPrefix = makeStatusDirectoryPrefix();
        String newDir = null;
        try {
            newDir = HdfsUtil.findUnallocatedFileName(hdfsApi, newDirPrefix, "");
        } catch (HdfsApiException e) {
            throw new ServiceFormattedException(e);
        }

        job.setStatusDir(newDir);
        LOG.debug("Status dir for job#" + job.getId() + ": " + newDir);
    }

    private String makeStatusDirectoryPrefix() {
        String userScriptsPath = context.getProperties().get("jobs.dir");

        if (userScriptsPath == null) { // TODO: move check to initialization code
            String msg = "jobs.dir is not configured!";
            LOG.error(msg);
            throw new MisconfigurationFormattedException("jobs.dir");
        }

        String normalizedName = String.format("hive-job-%s", job.getId());
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
        return String.format(userScriptsPath +
                "/%s-%s", normalizedName, timestamp);
    }

    private void setupQueryFile() {
        String statusDir = job.getStatusDir();
        assert statusDir != null : "setupStatusDir() should be called first";

        String jobQueryFilePath = statusDir + "/" + "query.hql";

        try {

            if (job.getForcedContent() != null) {

                HdfsUtil.putStringToFile(hdfsApi, jobQueryFilePath, job.getForcedContent());
                job.setForcedContent("");  // prevent forcedContent to be written to DB

            } else if (job.getQueryId() != null) {

                String savedQueryFile = getRelatedSavedQueryFile();
                hdfsApi.copy(savedQueryFile, jobQueryFilePath);
                job.setQueryFile(jobQueryFilePath);

            } else {

                throw new BadRequestFormattedException("queryId or forcedContent should be passed!", null);

            }

        } catch (IOException e) {
            throw new ServiceFormattedException("F040 Error when creating file " + jobQueryFilePath, e);
        } catch (InterruptedException e) {
            throw new ServiceFormattedException("F040 Error when creating file " + jobQueryFilePath, e);
        } catch (HdfsApiException e) {
            throw new ServiceFormattedException(e);
        }
        job.setQueryFile(jobQueryFilePath);

        LOG.debug("Query file for job#" + job.getId() + ": " + jobQueryFilePath);
    }


    private ConnectionConfig getHiveConnectionConfig() {
        return ConnectionFactory.create(context);
    }

    private String getRelatedSavedQueryFile() {
        SavedQuery savedQuery;
        try {
            savedQuery = savedQueryResourceManager.read(job.getQueryId());
        } catch (ItemNotFound itemNotFound) {
            throw new BadRequestFormattedException("queryId not found!", itemNotFound);
        }
        return savedQuery.getQueryFile();
    }

    @Override
    public boolean onModification(Object object) {
        setModified(true);
        return true;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public void clearModified() {
        setModified(false);
    }
}
