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

package org.apache.ambari.view.hive.resources.jobs.atsJobs;

import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class ATSParser implements IATSParser {
  protected final static Logger LOG =
      LoggerFactory.getLogger(ATSParser.class);

  private ATSRequestsDelegate delegate;

  private static final long MillisInSecond = 1000L;

  public ATSParser(ATSRequestsDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<HiveQueryId> getHiveQuieryIdsList(String username) {
    JSONObject entities = delegate.hiveQueryIdList(username);
    JSONArray jobs = (JSONArray) entities.get("entities");

    List<HiveQueryId> parsedJobs = new LinkedList<HiveQueryId>();
    for(Object job : jobs) {
      try {
        HiveQueryId parsedJob = parseAtsHiveJob((JSONObject) job);
        parsedJobs.add(parsedJob);
      } catch (Exception ex) {
        LOG.error("Error while parsing ATS job", ex);
      }
    }

    return parsedJobs;
  }

  @Override
  public HiveQueryId getHiveQuieryIdByOperationId(byte[] guid) {
    String guidString = new String(guid);
    JSONObject entities = delegate.hiveQueryIdByOperationId(guidString);
    JSONArray jobs = (JSONArray) entities.get("entities");

    assert jobs.size() <= 1;
    if (jobs.size() == 0) {
      //TODO: throw appropriate exception
      throw new ServiceFormattedException("HIVE_QUERY_ID with operationid=" + guidString + " not found");
    }

    return parseAtsHiveJob((JSONObject) jobs.get(0));
  }

  @Override
  public TezDagId getTezDAGByName(String name) {
    JSONArray tezDagEntities = (JSONArray) delegate.tezDagByName(name).get("entities");
    assert tezDagEntities.size() <= 1;
    if (tezDagEntities.size() == 0) {
      return new TezDagId();
    }
    JSONObject tezDagEntity = (JSONObject) tezDagEntities.get(0);

    TezDagId parsedDag = new TezDagId();
    JSONArray applicationIds = (JSONArray) ((JSONObject) tezDagEntity.get("primaryfilters")).get("applicationId");
    parsedDag.applicationId = (String) applicationIds.get(0);
    parsedDag.status = (String) ((JSONObject) tezDagEntity.get("otherinfo")).get("status");
    return parsedDag;
  }

  private HiveQueryId parseAtsHiveJob(JSONObject job) {
    HiveQueryId parsedJob = new HiveQueryId();

    parsedJob.entity = (String) job.get("entity");
    parsedJob.starttime = ((Long) job.get("starttime")) / MillisInSecond;

    JSONObject primaryfilters = (JSONObject) job.get("primaryfilters");
    JSONArray operationIds = (JSONArray) primaryfilters.get("operationid");
    if (operationIds != null) {
      parsedJob.operationId = (String) (operationIds).get(0);
    }
    JSONArray users = (JSONArray) primaryfilters.get("user");
    if (users != null) {
      parsedJob.user = (String) (users).get(0);
    }

    JSONObject lastEvent = getLastEvent(job);
    long lastEventTimestamp = ((Long) lastEvent.get("timestamp")) / MillisInSecond;

    parsedJob.duration = lastEventTimestamp - parsedJob.starttime;

    JSONObject otherinfo = (JSONObject) job.get("otherinfo");
    JSONObject query = (JSONObject) JSONValue.parse((String) otherinfo.get("QUERY"));

    parsedJob.query = (String) query.get("queryText");
    JSONObject stages = (JSONObject) ((JSONObject) query.get("queryPlan")).get("STAGE PLANS");

    List<String> dagIds = new LinkedList<String>();
    List<JSONObject> stagesList = new LinkedList<JSONObject>();

    for (Object key : stages.keySet()) {
      JSONObject stage = (JSONObject) stages.get(key);
      if (stage.get("Tez") != null) {
        String dagId = (String) ((JSONObject) stage.get("Tez")).get("DagName:");
        dagIds.add(dagId);
      }
      stagesList.add(stage);
    }
    parsedJob.dagNames = dagIds;
    parsedJob.stages = stagesList;
    return parsedJob;
  }

  private JSONObject getLastEvent(JSONObject atsEntity) {
    JSONArray events = (JSONArray) atsEntity.get("events");
    return (JSONObject) events.get(0);
  }
}
