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

package org.apache.ambari.view.hive.resources.jobs.rm;

import org.apache.ambari.view.hive.resources.jobs.atsJobs.TezVertexId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RMParser {
  private RMRequestsDelegate delegate;

  public RMParser(RMRequestsDelegate delegate) {
    this.delegate = delegate;
  }

  public Double getDAGProgress(String appId, String dagId) {
    String dagIdx = parseDagIdIndex(dagId);
    JSONObject dagProgress = (JSONObject) delegate.dagProgress(appId, dagIdx).get("dagProgress");
    return (Double) (dagProgress.get("progress"));
  }

  public List<VertexProgress> getDAGVerticesProgress(String appId, String dagId, List<TezVertexId> vertices) {
    String dagIdx = parseDagIdIndex(dagId);

    Map<String, String> vertexIdToEntityMapping = new HashMap<String, String>();
    StringBuilder builder = new StringBuilder();
    if (vertices.size() > 0) {
      for (TezVertexId vertexId : vertices) {
        String[] parts = vertexId.entity.split("_");
        String vertexIdx = parts[parts.length - 1];
        builder.append(vertexIdx).append(",");

        vertexIdToEntityMapping.put(vertexId.entity, vertexId.vertexName);
      }
      builder.setLength(builder.length() - 1); // remove last comma
    }

    String commaSeparatedVertices = builder.toString();

    JSONArray vertexProgresses = (JSONArray) delegate.verticesProgress(
        appId, dagIdx, commaSeparatedVertices).get("vertexProgresses");

    List<VertexProgress> parsedVertexProgresses = new LinkedList<VertexProgress>();
    for (Object vertex : vertexProgresses) {
      JSONObject jsonObject = (JSONObject) vertex;

      VertexProgress vertexProgressInfo = new VertexProgress();
      vertexProgressInfo.id = (String) jsonObject.get("id");
      vertexProgressInfo.name = vertexIdToEntityMapping.get(vertexProgressInfo.id);
      vertexProgressInfo.progress = (Double) jsonObject.get("progress");

      parsedVertexProgresses.add(vertexProgressInfo);
    }
    return parsedVertexProgresses;
  }

  public String parseDagIdIndex(String dagId) {
    String[] dagIdParts = dagId.split("_");
    return dagIdParts[dagIdParts.length - 1];
  }

  public static class VertexProgress {
    public String id;
    public String name;
    public Double progress;
  }
}
