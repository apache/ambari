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

package org.apache.ambari.view.hive2.resources.jobs.atsJobs;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Parser of ATS responses
 */
public class ATSParser implements IATSParser {
  protected final static Logger LOG =
    LoggerFactory.getLogger(ATSParser.class);

  private ATSRequestsDelegate delegate;

  private static final long MillisInSecond = 1000L;

  public ATSParser(ATSRequestsDelegate delegate) {
    this.delegate = delegate;
  }

  /**
   * returns all HiveQueryIDs from ATS for the given user.
   * @param username
   * @return
   */
  @Override
  public List<HiveQueryId> getHiveQueryIdsForUser(String username) {
    JSONObject entities = delegate.hiveQueryIdsForUser(username);
    return parseHqidJsonFromATS(entities);
  }

  /**
   * parses the JSONArray or hive query IDs
   * @param entities: should contain 'entities' element as JSONArray
   * @return
   */
  private List<HiveQueryId> parseHqidJsonFromATS(JSONObject entities) {
    JSONArray jobs = (JSONArray) entities.get("entities");

    return getHqidListFromJsonArray(jobs);
  }

  /**
   * parses List of HiveQueryIds from JSON
   * @param jobs
   * @return
   */
  private List<HiveQueryId> getHqidListFromJsonArray(JSONArray jobs) {
    List<HiveQueryId> parsedJobs = new LinkedList<>();
    for (Object job : jobs) {
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
  public List<TezVertexId> getVerticesForDAGId(String dagId) {
    JSONObject entities = delegate.tezVerticesListForDAG(dagId);
    JSONArray vertices = (JSONArray) entities.get("entities");

    List<TezVertexId> parsedVertices = new LinkedList<TezVertexId>();
    for(Object vertex : vertices) {
      try {
        TezVertexId parsedVertex = parseVertex((JSONObject) vertex);
        parsedVertices.add(parsedVertex);
      } catch (Exception ex) {
        LOG.error("Error while parsing the vertex", ex);
      }
    }

    return parsedVertices;
  }

  @Override
  public HiveQueryId getHiveQueryIdByOperationId(String guidString) {
    JSONObject entities = delegate.hiveQueryIdByOperationId(guidString);
    return getHiveQueryIdFromJson(entities);
  }

  private HiveQueryId getHiveQueryIdFromJson(JSONObject entities) {
    JSONArray jobs = (JSONArray) entities.get("entities");

    if (jobs.size() == 0) {
      return new HiveQueryId();
    }

    return parseAtsHiveJob((JSONObject) jobs.get(0));
  }

  /**
   * returns the hive entity from ATS. empty object if not found.
   *
   * @param hiveId: the entityId of the hive
   * @return: empty entity if not found else HiveQueryId
   */
  @Override
  public HiveQueryId getHiveQueryIdByHiveEntityId(String hiveId) {
    JSONObject entity = delegate.hiveQueryEntityByEntityId(hiveId);
    return parseAtsHiveJob(entity);
  }

  @Override
  public TezDagId getTezDAGByName(String name) {
    JSONArray tezDagEntities = (JSONArray) delegate.tezDagByName(name).get("entities");
    return parseTezDag(tezDagEntities);
  }

  @Override
  public TezDagId getTezDAGByEntity(String entity) {
    JSONArray tezDagEntities = (JSONArray) delegate.tezDagByEntity(entity).get("entities");
    return parseTezDag(tezDagEntities);
  }

  /**
   * fetches the HIVE_QUERY_ID from ATS for given user between given time period
   *
   * @param username:  username for which to fetch hive query IDs
   * @param startTime: time in miliseconds, inclusive
   * @param endTime:   time in miliseconds, exclusive
   * @return: List of HIVE_QUERY_ID
   */
  @Override
  public List<HiveQueryId> getHiveQueryIdsForUserByTime(String username, long startTime, long endTime) {
    JSONObject entities = delegate.hiveQueryIdsForUserByTime(username, startTime, endTime);
    return parseHqidJsonFromATS(entities);
  }

  @Override
  public List<HiveQueryId> getHiveQueryIdByEntityList(List<String> hiveIds) {
    List<HiveQueryId> hiveQueryIds = new LinkedList<>();
    for (String id : hiveIds) {
      HiveQueryId hqi = this.getHiveQueryIdByHiveEntityId(id);
      if (null != hqi.entity) {
        hiveQueryIds.add(hqi);
      }
    }
    return hiveQueryIds;
  }

  private TezDagId parseTezDag(JSONArray tezDagEntities) {
    assert tezDagEntities.size() <= 1;
    if (tezDagEntities.size() == 0) {
      return new TezDagId();
    }
    JSONObject tezDagEntity = (JSONObject) tezDagEntities.get(0);

    TezDagId parsedDag = new TezDagId();
    JSONArray applicationIds = (JSONArray) ((JSONObject) tezDagEntity.get("primaryfilters")).get("applicationId");
    parsedDag.entity = (String) tezDagEntity.get("entity");
    parsedDag.applicationId = (String) applicationIds.get(0);
    parsedDag.status = (String) ((JSONObject) tezDagEntity.get("otherinfo")).get("status");
    return parsedDag;
  }

  private HiveQueryId parseAtsHiveJob(JSONObject job) {
    HiveQueryId parsedJob = new HiveQueryId();

    parsedJob.entity = (String) job.get("entity");
    parsedJob.url = delegate.hiveQueryIdDirectUrl((String) job.get("entity"));
    parsedJob.starttime = ((Long) job.get("starttime"));

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
    long lastEventTimestamp = ((Long) lastEvent.get("timestamp"));

    parsedJob.duration = (lastEventTimestamp - parsedJob.starttime) / MillisInSecond;

    JSONObject otherinfo = (JSONObject) job.get("otherinfo");
    if (otherinfo.get("QUERY") != null) {  // workaround for HIVE-10829
      JSONObject query = (JSONObject) JSONValue.parse((String) otherinfo.get("QUERY"));

      parsedJob.query = (String) query.get("queryText");
      JSONObject stages = (JSONObject) ((JSONObject) query.get("queryPlan")).get("STAGE PLANS");

      List<String> dagIds = new LinkedList<String>();
      List<JSONObject> stagesList = new LinkedList<JSONObject>();

      for (Object key : stages.keySet()) {
        JSONObject stage = (JSONObject) stages.get(key);
        if (stage.get("Tez") != null) {
          String dagId = (String) ((JSONObject) stage.get("Tez")).get("DagId:");
          dagIds.add(dagId);
        }
        stagesList.add(stage);
      }
      parsedJob.dagNames = dagIds;
      parsedJob.stages = stagesList;
    }

    if (otherinfo.get("VERSION") != null) {
      parsedJob.version = (Long) otherinfo.get("VERSION");
    }
    return parsedJob;
  }

  private TezVertexId parseVertex(JSONObject vertex) {
    TezVertexId tezVertexId = new TezVertexId();
    tezVertexId.entity = (String)vertex.get("entity");
    JSONObject otherinfo = (JSONObject)vertex.get("otherinfo");
    if (otherinfo != null)
      tezVertexId.vertexName = (String)otherinfo.get("vertexName");
    return tezVertexId;
  }

  private JSONObject getLastEvent(JSONObject atsEntity) {
    JSONArray events = (JSONArray) atsEntity.get("events");
    return (JSONObject) events.get(0);
  }
}
