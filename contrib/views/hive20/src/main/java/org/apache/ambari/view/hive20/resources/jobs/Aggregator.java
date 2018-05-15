/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.jobs;

import akka.actor.ActorRef;
import org.apache.ambari.view.hive20.actor.message.job.SaveDagInformation;
import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.Indexed;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive20.resources.IResourceManager;
import org.apache.ambari.view.hive20.resources.files.FileService;
import org.apache.ambari.view.hive20.resources.jobs.atsJobs.HiveQueryId;
import org.apache.ambari.view.hive20.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive20.resources.jobs.atsJobs.TezDagId;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobInfo;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * View Jobs and ATS Jobs aggregator.
 * There are 4 options:
 * 1) ATS ExecuteJob without operationId
 *    *Meaning*: executed outside of HS2
 *    - ExecuteJob info only from ATS
 * 2) ATS ExecuteJob with operationId
 *    a) Hive View ExecuteJob with same operationId is not present
 *        *Meaning*: executed with HS2
 *      - ExecuteJob info only from ATS
 *    b) Hive View ExecuteJob with operationId is present (need to merge)
 *        *Meaning*: executed with HS2 through Hive View
 *      - ExecuteJob info merged from ATS and from Hive View DataStorage
 * 3) ExecuteJob present only in Hive View, ATS does not have it
 *   *Meaning*: executed through Hive View, but Hadoop ExecuteJob was not created
 *   it can happen if user executes query without aggregation, like just "select * from TABLE"
 *   - ExecuteJob info only from Hive View
 */
public class Aggregator {
  protected final static Logger LOG =
    LoggerFactory.getLogger(Aggregator.class);

  private final IATSParser ats;
  private IResourceManager<Job> viewJobResourceManager;
  private final ActorRef operationController;

  public Aggregator(IResourceManager<Job> jobResourceManager,
                    IATSParser ats, ActorRef operationController) {
    this.viewJobResourceManager = jobResourceManager;
    this.ats = ats;
    this.operationController = operationController;
  }

  /**
   * gets all the jobs for 'username' where the job submission time is between 'startTime' (inclusive)
   * and endTime (exclusive).
   * Fetches the jobs from ATS and DB merges and update DB. returns the combined list.
   *
   * @param username:  username for which jobs have to be fetched.
   * @param startTime: inclusive, time in secs from epoch
   * @param endTime:   exclusive, time in secs from epoch
   * @return: list of jobs
   */
  public List<Job> readAllForUserByTime(String username, long startTime, long endTime) {
    List<HiveQueryId> queryIdList = ats.getHiveQueryIdsForUserByTime(username, startTime, endTime);
    List<Job> allJobs = fetchDagsAndMergeJobs(queryIdList);
    List<Job> dbOnlyJobs = readDBOnlyJobs(username, queryIdList, startTime, endTime);
    allJobs.addAll(dbOnlyJobs);

    return allJobs;
  }

  /**
   * fetches the new state of jobs from ATS and from DB. Does merging/updating as required.
   * @param jobInfos: infos of job to get
   * @return: list of updated Job
   */
  public List<Job> readJobsByIds(List<JobInfo> jobInfos) {
    //categorize jobs
    List<String> jobsWithHiveIds = new LinkedList<>();
    List<String> dbOnlyJobs = new LinkedList<>();

    for (JobInfo jobInfo : jobInfos) {
      if (null == jobInfo.getHiveId() || jobInfo.getHiveId().trim().isEmpty()) {
        dbOnlyJobs.add(jobInfo.getJobId());
      } else {
        jobsWithHiveIds.add(jobInfo.getHiveId());
      }
    }

    List<HiveQueryId> queryIdList = ats.getHiveQueryIdByEntityList(jobsWithHiveIds);
    List<Job> allJobs = fetchDagsAndMergeJobs(queryIdList);
    List<Job> dbJobs = readJobsFromDbByJobId(dbOnlyJobs);

    allJobs.addAll(dbJobs);
    return allJobs;
  }

  /**
   * gets the jobs from the Database given their id
   * @param jobsIds: list of ids of jobs
   * @return: list of all the jobs found
   */
  private List<Job> readJobsFromDbByJobId(List<String> jobsIds) {
    List<Job> jobs = new LinkedList<>();
    for (final String jid : jobsIds) {
      try {
        Job job = getJobFromDbByJobId(jid);
        jobs.add(job);
      } catch (ItemNotFound itemNotFound) {
        LOG.error("Error while finding job with id : {}", jid, itemNotFound);
      }
    }

    return jobs;
  }

  /**
   * fetches the job from DB given its id
   * @param jobId: the id of the job to fetch
   * @return: the job
   * @throws ItemNotFound: if job with given id is not found in db
   */
  private Job getJobFromDbByJobId(final String jobId) throws ItemNotFound {
    if (null == jobId)
      return null;

    List<Job> jobs = viewJobResourceManager.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        return item.getId().equals(jobId);
      }

      @Override
      public String whereStatement() {
        return "id = '" + jobId + "'"; // even IDs are string
      }
    });

    if (null != jobs && !jobs.isEmpty())
      return jobs.get(0);

    throw new ItemNotFound(String.format("Job with id %s not found.", jobId));
  }

  /**
   * returns all the jobs from ATS and DB (for this instance) for the given user.
   * @param username
   * @return
   */
  public List<Job> readAll(String username) {
    List<HiveQueryId> queries = ats.getHiveQueryIdsForUser(username);
    LOG.debug("HiveQueryIds fetched : {}", queries);
    List<Job> allJobs = fetchDagsAndMergeJobs(queries);
    List<Job> dbOnlyJobs = readDBOnlyJobs(username, queries, null, null);
    LOG.debug("Jobs only present in DB: {}", dbOnlyJobs);
    allJobs.addAll(dbOnlyJobs);
    return allJobs;
  }

  /**
   * reads all the jobs from DB for username and excludes the jobs mentioned in queries list
   * @param username : username for which the jobs are to be read.
   * @param queries : the jobs to exclude
   * @param startTime: can be null, if not then the window start time for job
   * @param endTime: can be null, if not then the window end time for job
   * @return : the jobs in db that are not in the queries
   */
  private List<Job> readDBOnlyJobs(String username, List<HiveQueryId> queries, Long startTime, Long endTime) {
    List<Job> dbOnlyJobs = new LinkedList<>();
    HashMap<String, String> operationIdVsHiveId = new HashMap<>();

    for (HiveQueryId hqid : queries) {
      operationIdVsHiveId.put(hqid.operationId, hqid.entity);
    }
    LOG.debug("operationIdVsHiveId : {} ", operationIdVsHiveId);
    //cover case when operationId is present, but not exists in ATS
    //e.g. optimized queries without executing jobs, like "SELECT * FROM TABLE"
    List<Job> jobs = viewJobResourceManager.readAll(new OnlyOwnersFilteringStrategy(username));
    for (Job job : jobs) {
      if (null != startTime && null != endTime && null != job.getDateSubmitted()
        && (job.getDateSubmitted() < startTime || job.getDateSubmitted() >= endTime || operationIdVsHiveId.containsKey(job.getGuid()))
        ) {
        continue; // don't include this in the result
      } else {
        dbOnlyJobs.add(job);
      }
    }
    return dbOnlyJobs;
  }

  private List<Job> fetchDagsAndMergeJobs(List<HiveQueryId> queries) {
    List<Job> allJobs = new LinkedList<Job>();

    for (HiveQueryId atsHiveQuery : queries) {
      JobImpl atsJob = null;
      if (hasOperationId(atsHiveQuery)) {
        try {
          Job viewJob = getJobByOperationId(atsHiveQuery.operationId);
          TezDagId atsTezDag = getTezDagFromHiveQueryId(atsHiveQuery);
          atsJob = mergeHiveAtsTez(atsHiveQuery, atsTezDag, viewJob);
        } catch (ItemNotFound itemNotFound) {
          LOG.error("Ignore : {}", itemNotFound.getMessage());
          continue;
        }
      } else {
        TezDagId atsTezDag = getTezDagFromHiveQueryId(atsHiveQuery);
        atsJob = atsOnlyJob(atsHiveQuery, atsTezDag);
      }

      atsJob.setHiveQueryId(atsHiveQuery.entity);
      allJobs.add(atsJob);
    }

    return allJobs;
  }

  /**
   * @param atsHiveQuery
   * @param atsTezDag
   * @param viewJob
   * @return
   */
  private JobImpl mergeHiveAtsTez(HiveQueryId atsHiveQuery, TezDagId atsTezDag, Job viewJob) throws ItemNotFound {
    saveJobInfoIfNeeded(atsHiveQuery, atsTezDag, viewJob);
    return mergeAtsJobWithViewJob(atsHiveQuery, atsTezDag, viewJob);
  }

  public Job readATSJob(Job viewJob) throws ItemNotFound {

    if (viewJob.getStatus().equals(Job.JOB_STATE_INITIALIZED) || viewJob.getStatus().equals(Job.JOB_STATE_UNKNOWN))
      return viewJob;

    String hexGuid = viewJob.getGuid();


    HiveQueryId atsHiveQuery = ats.getHiveQueryIdByOperationId(hexGuid);

    TezDagId atsTezDag = getTezDagFromHiveQueryId(atsHiveQuery);

    saveJobInfoIfNeeded(atsHiveQuery, atsTezDag, viewJob, true);
    return mergeAtsJobWithViewJob(atsHiveQuery, atsTezDag, viewJob);
  }

  private TezDagId getTezDagFromHiveQueryId(HiveQueryId atsHiveQuery) {
    TezDagId atsTezDag;
    if (atsHiveQuery.version >= HiveQueryId.ATS_15_RESPONSE_VERSION) {
      atsTezDag = ats.getTezDAGByEntity(atsHiveQuery.entity);
    } else if (atsHiveQuery.dagNames != null && atsHiveQuery.dagNames.size() > 0) {
      String dagName = atsHiveQuery.dagNames.get(0);

      atsTezDag = ats.getTezDAGByName(dagName);
    } else {
      atsTezDag = new TezDagId();
    }
    return atsTezDag;
  }

  protected boolean hasOperationId(HiveQueryId atsHiveQuery) {
    return atsHiveQuery.operationId != null;
  }

  protected JobImpl mergeAtsJobWithViewJob(HiveQueryId atsHiveQuery, TezDagId atsTezDag, Job viewJob) {
    JobImpl atsJob;
    try {
      atsJob = new JobImpl(PropertyUtils.describe(viewJob));
    } catch (IllegalAccessException e) {
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    } catch (InvocationTargetException e) {
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    } catch (NoSuchMethodException e) {
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    }
    fillAtsJobFields(atsJob, atsHiveQuery, atsTezDag);
    return atsJob;
  }

  protected void saveJobInfoIfNeeded(HiveQueryId hiveQueryId, TezDagId tezDagId, Job viewJob) throws ItemNotFound {
    saveJobInfoIfNeeded(hiveQueryId, tezDagId, viewJob, false);
  }

  protected void saveJobInfoIfNeeded(HiveQueryId hiveQueryId, TezDagId tezDagId, Job viewJob, boolean useActorSystem) throws ItemNotFound {
    boolean updateDb = false;
    String dagName = null;
    String dagId = null;
    String applicationId = null;
    if (viewJob.getDagName() == null || viewJob.getDagName().isEmpty()) {
      if (hiveQueryId.dagNames != null && hiveQueryId.dagNames.size() > 0) {
        dagName = hiveQueryId.dagNames.get(0);
        updateDb = true;
      }
    }
    if (tezDagId.status != null && (tezDagId.status.compareToIgnoreCase(Job.JOB_STATE_UNKNOWN) != 0) &&
        !viewJob.getStatus().equalsIgnoreCase(tezDagId.status)) {
      dagId = tezDagId.entity;
      applicationId = tezDagId.applicationId;
      updateDb = true;
    }

    if(updateDb) {
      if (useActorSystem) {
        LOG.info("Saving DAG information via actor system for job id: {}", viewJob.getId());
        operationController.tell(new SaveDagInformation(viewJob.getId(), dagName, dagId, applicationId), ActorRef.noSender());
      } else {
        viewJob.setDagName(dagName);
        viewJob.setDagId(dagId);
        viewJob.setApplicationId(applicationId);
        viewJobResourceManager.update(viewJob, viewJob.getId());
      }
    }
  }

  protected JobImpl atsOnlyJob(HiveQueryId atsHiveQuery, TezDagId atsTezDag) {
    JobImpl atsJob = new JobImpl();
    atsJob.setId(atsHiveQuery.entity);
    fillAtsJobFields(atsJob, atsHiveQuery, atsTezDag);

    String query = atsHiveQuery.query;
    atsJob.setTitle(query.substring(0, (query.length() > 42) ? 42 : query.length()));

    atsJob.setQueryFile(FileService.JSON_PATH_FILE + atsHiveQuery.url + "#otherinfo.QUERY!queryText");
    return atsJob;
  }

  protected JobImpl fillAtsJobFields(JobImpl atsJob, HiveQueryId atsHiveQuery, TezDagId atsTezDag) {
    atsJob.setApplicationId(atsTezDag.applicationId);

    if (atsHiveQuery.dagNames != null && atsHiveQuery.dagNames.size() > 0)
      atsJob.setDagName(atsHiveQuery.dagNames.get(0));
    atsJob.setDagId(atsTezDag.entity);
    if (atsHiveQuery.starttime != 0)
      atsJob.setDateSubmitted(atsHiveQuery.starttime);
    atsJob.setDuration(atsHiveQuery.duration);
    return atsJob;
  }

  protected Job getJobByOperationId(final String opId) throws ItemNotFound {
    List<Job> jobs = viewJobResourceManager.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        Job opHandle = (Job) item;
        return opHandle.getGuid().equals(opId);
      }

      @Override
      public String whereStatement() {
        return "guid='" + opId + "'";
      }
    });

    if (jobs.size() != 1)
      throw new ItemNotFound();

    return jobs.get(0);
  }
}
