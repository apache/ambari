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
import org.apache.ambari.view.hive.resources.jobs.atsJobs.HiveQueryId;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.IATSParser;
import org.apache.ambari.view.hive.resources.jobs.atsJobs.TezDagId;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * View Jobs and ATS Jobs aggregator
 * Not all ViewJobs create ATS job
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
    for (HiveQueryId atsHiveQuery : ats.getHiveQuieryIdsList(username)) {

      TezDagId atsTezDag;
      if (atsHiveQuery.dagNames != null && atsHiveQuery.dagNames.size() > 0) {
        String dagName = atsHiveQuery.dagNames.get(0);

        atsTezDag = ats.getTezDAGByName(dagName);
      } else {
        atsTezDag = new TezDagId();
      }

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
    if (viewJob.getDagName() == null) {
      viewJob.setDagName(tezDagId.dagName);
      viewJobResourceManager.update(viewJob, viewJob.getId());
    }
    if (viewJob.getStatus().equals(tezDagId.status)) {
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

    atsJob.setQueryFile("fakefile://" + Base64.encodeBase64URLSafeString(query.getBytes()));  // fake queryFile
    return atsJob;
  }

  protected JobImpl fillAtsJobFields(JobImpl atsJob, HiveQueryId atsHiveQuery, TezDagId atsTezDag) {
    atsJob.setApplicationId(atsTezDag.applicationId);

    atsJob.setDagName(atsTezDag.dagName);
    if (!atsTezDag.status.equals(TezDagId.STATUS_UNKNOWN))
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
