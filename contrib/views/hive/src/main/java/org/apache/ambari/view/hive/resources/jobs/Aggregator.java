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
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

  public List<Job> readAll(String username) {
      Set<String> addedOperationIds = new HashSet<String>();

    List<Job> allJobs = new LinkedList<Job>();
    for (HiveQueryId atsHiveQuery : ats.getHiveQueryIdsList(username)) {

      TezDagId atsTezDag = getTezDagFromHiveQueryId(atsHiveQuery);

      JobImpl atsJob;
      if (hasOperationId(atsHiveQuery)) {
        try {
          Job viewJob = getJobByOperationId(urlSafeBase64ToHexString(atsHiveQuery.operationId));
          saveJobInfoIfNeeded(atsHiveQuery, atsTezDag, viewJob);

          atsJob = mergeAtsJobWithViewJob(atsHiveQuery, atsTezDag, viewJob);
        } catch (ItemNotFound itemNotFound) {
          // Executed from HS2, but outside of Hive View
          atsJob = atsOnlyJob(atsHiveQuery, atsTezDag);
        }
      } else {
        atsJob = atsOnlyJob(atsHiveQuery, atsTezDag);
      }
      allJobs.add(atsJob);

      addedOperationIds.add(atsHiveQuery.operationId);
    }

    //cover case when operationId is present, but not exists in ATS
    //e.g. optimized queries without executing jobs, like "SELECT * FROM TABLE"
    for (Job job : viewJobResourceManager.readAll(new OnlyOwnersFilteringStrategy(username))) {
      List<StoredOperationHandle> operationHandles = operationHandleResourceManager.readJobRelatedHandles(job);
      assert operationHandles.size() <= 1;

      if (operationHandles.size() > 0) {
        StoredOperationHandle operationHandle = operationHandles.get(0);

        if (!addedOperationIds.contains(hexStringToUrlSafeBase64(operationHandle.getGuid()))) {
          //e.g. query without hadoop job: select * from table
          allJobs.add(job);
        }
      }
    }

    return allJobs;
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
    }catch(IllegalAccessException e){
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    }catch(InvocationTargetException e){
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    }catch(NoSuchMethodException e){
      LOG.error("Can't instantiate JobImpl", e);
      return null;
    }
    fillAtsJobFields(atsJob, atsHiveQuery, atsTezDag);
    return atsJob;
  }

  protected void saveJobInfoIfNeeded(HiveQueryId hiveQueryId, TezDagId tezDagId, Job viewJob) throws ItemNotFound {
    if (viewJob.getDagName() == null || viewJob.getDagName().isEmpty()) {
      if (hiveQueryId.dagNames != null && hiveQueryId.dagNames.size() > 0) {
        viewJob.setDagName(hiveQueryId.dagNames.get(0));
        viewJobResourceManager.update(viewJob, viewJob.getId());
      }
    }
    if (tezDagId.status != null && (tezDagId.status.compareToIgnoreCase(Job.JOB_STATE_UNKNOWN) != 0) &&
        !viewJob.getStatus().equals(tezDagId.status)) {
      viewJob.setDagId(tezDagId.entity);
      viewJob.setStatus(tezDagId.status);
      viewJobResourceManager.update(viewJob, viewJob.getId());
    }
  }

  protected JobImpl atsOnlyJob(HiveQueryId atsHiveQuery, TezDagId atsTezDag) {
    JobImpl atsJob = new JobImpl();
    atsJob.setId(atsHiveQuery.entity);
    fillAtsJobFields(atsJob, atsHiveQuery, atsTezDag);

    String query = atsHiveQuery.query;
    atsJob.setTitle(query.substring(0, (query.length() > 42)?42:query.length()));

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

  protected Job getJobByOperationId(final String opId) throws ItemNotFound {
    List<StoredOperationHandle> operationHandles = operationHandleResourceManager.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        StoredOperationHandle opHandle = (StoredOperationHandle) item;
        return opHandle.getGuid().equals(opId);
      }

      @Override
      public String whereStatement() {
        return "guid='" + opId + "'";
      }
    });

    if (operationHandles.size() != 1)
      throw new ItemNotFound();

    return viewJobResourceManager.read(operationHandles.get(0).getJobId());
  }

  protected static String urlSafeBase64ToHexString(String urlsafeBase64){
    byte[] decoded = Base64.decodeBase64(urlsafeBase64);

    StringBuilder sb = new StringBuilder();
    for(byte b : decoded){
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  protected static String hexStringToUrlSafeBase64(String hexString){
    byte[] decoded = new byte[hexString.length() / 2];

    for(int i=0; i<hexString.length(); i+=2) {
       decoded[i / 2] = (byte) Integer.parseInt(String.format("%c%c", hexString.charAt(i), hexString.charAt(i+1)), 16);
    }
    return Base64.encodeBase64URLSafeString(decoded);
  }
}
