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

package org.apache.ambari.view.hive2.resources.jobs;

import akka.actor.ActorRef;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.hive2.actor.message.job.SaveDagInformation;
import org.apache.ambari.view.hive2.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive2.persistence.utils.Indexed;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.resources.IResourceManager;
import org.apache.ambari.view.hive2.resources.files.FileService;
import org.apache.ambari.view.hive2.resources.jobs.atsJobs.HiveQueryId;
import org.apache.ambari.view.hive2.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive2.resources.jobs.atsJobs.TezDagId;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobInfo;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
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
  public List<Job> readAllForUserByTime(String username, Long startTime, Long endTime) {
    List<Job> jobs = readDBJobs(username, startTime, endTime);
    return jobs;
  }

  /**
   * fetches the new state of jobs from ATS and from DB. Does merging/updating as required.
   * @param jobInfos: infos of job to get
   * @return: list of updated Job
   */
  public List<Job> readJobsByIds(final List<JobInfo> jobInfos) {
    List<String> jobIds = FluentIterable.from(jobInfos).filter(new Predicate<JobInfo>() {
      @Override
      public boolean apply(@Nullable JobInfo input) {
        return !Strings.isNullOrEmpty(input.getJobId());
      }
    }).transform(new Function<JobInfo, String>() {
      @Nullable
      @Override
      public String apply(@Nullable JobInfo input) {
        return input.getJobId();
      }
    }).toList();
    List<Job> dbJobs = readJobsFromDbByJobId(jobIds);
    LOG.debug("readJobsByIds: dbJobs : {}", dbJobs);
    return dbJobs;
  }

  /**
   * gets the jobs from the Database given their id
   * @param jobsIds: list of ids of jobs
   * @return: list of all the jobs found
   */
  private List<Job> readJobsFromDbByJobId(final List<String> jobsIds) {
    LOG.info("Reading jobs from db with ids : {} ", jobsIds);
    List<Job> jobs = viewJobResourceManager.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        JobImpl job = (JobImpl) item;
        return jobsIds.contains(job.getId());
      }

      @Override
      public String whereStatement() {
        String query = " id in ( " + Joiner.on(",").join(jobsIds) + " ) ";
        LOG.debug("where clause for jobsIds : {}", query);
        return query;
      }
    });

    LOG.debug("jobs returned from DB : {}" , jobs);
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
    return readAllForUserByTime(username, null, null);
  }

  /**
   * reads all the jobs from DB for username and excludes the jobs mentioned in queries list
   * @param username : username for which the jobs are to be read.
   * @param startTime: can be null, if not then the window start time for job
   * @param endTime: can be null, if not then the window end time for job
   * @return : the jobs in db that are not in the queries
   */
  private List<Job> readDBJobs(final String username, final Long startTime, final Long endTime) {
    List<Job> jobs = viewJobResourceManager.readAll( new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        JobImpl job = (JobImpl) item;
        return job.getOwner().compareTo(username) == 0 &&
          ( (null == startTime || job.getDateSubmitted() >= startTime ) &&
            ( null == endTime || job.getDateSubmitted() < endTime )
          );
      }
      @Override
      public String whereStatement() {
        StringBuilder sb = new StringBuilder( "owner = '" ).append( username ).append( "'" );
        if( null != startTime || null != endTime ) {
          sb.append(" AND ( " );
          if( null != startTime ) {
            sb.append( " dateSubmitted >= " ).append( startTime );
          }
          if( null != endTime ){
            if(null != startTime){
              sb.append(" AND ");
            }
            sb.append(" dateSubmitted < ").append(endTime);
          }
          sb.append( " ) " );
        }
        String where = sb.toString();
        LOG.debug("where statement : {}", where);
        return where;
      }
    });
    LOG.debug("returning jobs: {}", jobs);
    return jobs;
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
