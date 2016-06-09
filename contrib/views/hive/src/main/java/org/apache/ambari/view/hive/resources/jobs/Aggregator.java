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

package org.apache.ambari.view.hive.resources.jobs;

import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive.resources.IResourceManager;
import org.apache.ambari.view.hive.resources.files.FileService;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.HiveQueryId;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.TezDagId;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobInfo;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * View Jobs and ATS Jobs aggregator.
 * There are 4 options:
 * 1) ATS Job without operationId
 *    *Meaning*: executed outside of HS2
 *    - Job info only from ATS
 * 2) ATS Job with operationId
 *    a) Hive View Job with same operationId is not present
 *        *Meaning*: executed with HS2
 *      - Job info only from ATS
 *    b) Hive View Job with operationId is present (need to merge)
 *        *Meaning*: executed with HS2 through Hive View
 *      - Job info merged from ATS and from Hive View DataStorage
 * 3) Job present only in Hive View, ATS does not have it
 *   *Meaning*: executed through Hive View, but Hadoop Job was not created
 *   it can happen if user executes query without aggregation, like just "select * from TABLE"
 *   - Job info only from Hive View
 */
public class Aggregator {
  protected final static Logger LOG =
    LoggerFactory.getLogger(Aggregator.class);

  private final IATSParser ats;
  private final IOperationHandleResourceManager operationHandleResourceManager;
  private IResourceManager<Job> viewJobResourceManager;

  public Aggregator(IResourceManager<Job> jobResourceManager,
                    IOperationHandleResourceManager operationHandleResourceManager,
                    IATSParser ats) {
    this.viewJobResourceManager = jobResourceManager;
    this.operationHandleResourceManager = operationHandleResourceManager;
    this.ats = ats;
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
   * returns the job which is associated with the given (guid)
   * @param optId: the operationId for which the job needs to be fetched.
   * @return: the job
   * @throws ItemNotFound: if no job was found to be associated with given operationId (guid) or if no
   * StoredOperationHandle found with guid optId.
   */
  private Job getJobFromDbByOperationId(final String optId) throws ItemNotFound {
    StoredOperationHandle handle = getStoredOperationHandleByGuid(optId);
    Job job = operationHandleResourceManager.getJobByHandle(handle);
    return job;
  }

  /**
   * returns the StoredOperationHandle with given id
   * @param optId: id of the StoredOperationHandle
   * @return: StoredOperationHandle
   * @throws ItemNotFound: if no StoredOperationHandle found for given guid (operationId).
   */
  private StoredOperationHandle getStoredOperationHandleByGuid(final String optId) throws ItemNotFound {
    LOG.debug("stored procedure with operation id : {} in DB", optId);
    List<StoredOperationHandle> operationHandles = operationHandleResourceManager.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        StoredOperationHandle soh = (StoredOperationHandle) item;
        return soh.getGuid().equals(optId);
      }

      @Override
      public String whereStatement() {
        return " guid = '" + optId + "'";
      }
    });

    if (null != operationHandles && !operationHandles.isEmpty()) {
      return operationHandles.get(0);
    }

    throw new ItemNotFound(String.format("Stored operation handle with id %s not found", optId));
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
    List<Job> dbOnlyJobs = new LinkedList<Job>();
    HashMap<String, String> operationIdVsHiveId = new HashMap<>();

    for (HiveQueryId hqid : queries) {
      operationIdVsHiveId.put(hqid.operationId, hqid.entity);
    }
    LOG.info("operationIdVsHiveId : {} ", operationIdVsHiveId);
    //cover case when operationId is present, but not exists in ATS
    //e.g. optimized queries without executing jobs, like "SELECT * FROM TABLE"
    List<Job> jobs = viewJobResourceManager.readAll(new OnlyOwnersFilteringStrategy(username));
    for (Job job : jobs) {
      if (null != startTime && null != endTime && null != job.getDateSubmitted()
        && (job.getDateSubmitted() < startTime || job.getDateSubmitted() >= endTime)
        ) {
        continue; // don't include this in the result
      }

      List<StoredOperationHandle> operationHandles = operationHandleResourceManager.readJobRelatedHandles(job);

      if (operationHandles.size() > 0) {
        StoredOperationHandle operationHandle = operationHandles.get(0);

        if (!operationIdVsHiveId.containsKey(hexStringToUrlSafeBase64(operationHandle.getGuid()))) {
          //e.g. query without hadoop job: select * from table
          dbOnlyJobs.add(job);
        }
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
          Job viewJob = getJobFromDbByOperationId(urlSafeBase64ToHexString(atsHiveQuery.operationId));
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
    TOperationHandle operationHandle = operationHandleResourceManager.getHandleForJob(viewJob).toTOperationHandle();

    String hexGuid = Hex.encodeHexString(operationHandle.getOperationId().getGuid());
    HiveQueryId atsHiveQuery = ats.getHiveQueryIdByOperationId(hexStringToUrlSafeBase64(hexGuid));

    TezDagId atsTezDag = getTezDagFromHiveQueryId(atsHiveQuery);

    saveJobInfoIfNeeded(atsHiveQuery, atsTezDag, viewJob);
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
    boolean shouldUpdate = false;
    if (viewJob.getDagName() == null || viewJob.getDagName().isEmpty()) {
      if (hiveQueryId.dagNames != null && hiveQueryId.dagNames.size() > 0) {
        viewJob.setDagName(hiveQueryId.dagNames.get(0));
        shouldUpdate = true;
      }
    }
    if (tezDagId.status != null && (tezDagId.status.compareToIgnoreCase(Job.JOB_STATE_UNKNOWN) != 0) &&
      !viewJob.getStatus().equals(tezDagId.status)) {
      viewJob.setDagId(tezDagId.entity);
      viewJob.setStatus(tezDagId.status);
      shouldUpdate = true;
    }

    if (shouldUpdate) {
      viewJobResourceManager.update(viewJob, viewJob.getId());
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
    if (atsTezDag.status != null && !atsTezDag.status.equals(TezDagId.STATUS_UNKNOWN))
      atsJob.setStatus(atsTezDag.status);
    if (atsHiveQuery.starttime != 0)
      atsJob.setDateSubmitted(atsHiveQuery.starttime);
    atsJob.setDuration(atsHiveQuery.duration);
    return atsJob;
  }

  protected static String urlSafeBase64ToHexString(String urlsafeBase64) {
    byte[] decoded = Base64.decodeBase64(urlsafeBase64);

    StringBuilder sb = new StringBuilder();
    for (byte b : decoded) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  protected static String hexStringToUrlSafeBase64(String hexString) {
    byte[] decoded = new byte[hexString.length() / 2];

    for (int i = 0; i < hexString.length(); i += 2) {
      decoded[i / 2] = (byte) Integer.parseInt(String.format("%c%c", hexString.charAt(i), hexString.charAt(i + 1)), 16);
    }
    return Base64.encodeBase64URLSafeString(decoded);
  }
}
