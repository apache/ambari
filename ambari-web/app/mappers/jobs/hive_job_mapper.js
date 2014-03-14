/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * Maps a Hive job response from server into an existing Hive Job.
 *
 * This will only update an existing Hive Job and not create a new one. It will
 * populate all fields (stages, Tez DAG, etc.) except runtime information like
 * <ul>
 * <li>tezDag.id
 * <li>tezDag.vertices.state
 * <li>tezDag.vertices.startTime
 * <li>tezDag.vertices.endTime
 * <li>tezDag.vertices.tasksCount
 * <li>tezDag.vertices.file*
 * <li>tezDag.vertices.hdfs*
 * <li>tezDag.vertices.recordReadCount
 * <li>tezDag.vertices.recordWriteCount
 * </ul>
 */
App.hiveJobMapper = App.QuickDataMapper.create({
  model : App.HiveJob,
  map : function(json) {
    var model = this.get('model');
    if (!model) {
      return;
    }
    var hiveJob = {};
    if (json && json.entity) {
      var hiveJob = {};
      hiveJob.id = json.entity;
      hiveJob.name = hiveJob.id;
      hiveJob.startTime = json.starttime;
      hiveJob.endTime = json.endtime;
      json.otherinfo.query = $.parseJSON(json.otherinfo.query);
      if (json.otherinfo.query && json.otherinfo.query.queryText) {
        hiveJob.query_text = json.otherinfo.query.queryText;
      }
      hiveJob.stages = [];
      var stagePlans = json.otherinfo.query.queryPlan["STAGE PLANS"];
      for ( var stage in stagePlans) {
        var stageValue = stagePlans[stage];
        var stageItem = {};
        stageItem.id = stage;
        stageItem.description = '. ';
        for (var item in stageValue) {
          stageItem.description += item;
        };
        hiveJob.stages.push(stageItem);
        if (stageValue.Tez != null && hiveJob.tezDag == null) {
          var dagName = stageValue.Tez['DagName:'];
          // Vertices
          var vertices = [];
          var vertexIds = [];
          var vertexIdMap = {};
          for ( var vertexName in stageValue.Tez["Vertices:"]) {
            var vertex = stageValue.Tez["Vertices:"][vertexName];
            var vertexObj = {
              id : dagName + "/" + vertexName,
              name : vertexName,
              incoming_edges : [],
              outgoing_edges : []
            };
            vertexIds.push(vertexObj.id);
            var operatorExtractor = function(obj) {
              var ops = [];
              if ($.isArray(obj)) {
                obj.forEach(function(o) {
                  ops = ops.concat(operatorExtractor(o));
                });
              } else {
                for ( var key in obj) {
                  ops.push(key);
                  if (obj[key].children != null) {
                    ops = ops.concat(operatorExtractor(obj[key].children));
                  }
                }
              }
              return ops;
            }
            if (vertex["Map Operator Tree:"] != null) {
              vertexObj.type = App.TezDagVertexType.MAP;
              vertexObj.operations = operatorExtractor(vertex["Map Operator Tree:"]);
              vertexObj.operation_plan = JSON.stringify(vertex["Map Operator Tree:"], undefined, "  ");
            } else if (vertex["Reduce Operator Tree:"] != null) {
              vertexObj.type = App.TezDagVertexType.REDUCE;
              vertexObj.operations = operatorExtractor(vertex["Reduce Operator Tree:"]);
              vertexObj.operation_plan = JSON.stringify(vertex["Reduce Operator Tree:"], undefined, "  ");
            } else if (vertex["Vertex:"] != null && vertexName==vertex['Vertex:']) {
              vertexObj.type = App.TezDagVertexType.UNION;
            }
            vertexIdMap[vertexObj.id] = vertexObj;
            vertices.push(vertexObj);
          }
          // Edges
          var edges = [];
          var edgeIds = [];
          for ( var childVertex in stageValue.Tez["Edges:"]) {
            var childVertices = stageValue.Tez["Edges:"][childVertex];
            if (!$.isArray(childVertices)) {
              // Single edge given as object instead of array
              childVertices = [ childVertices ];
            }
            childVertices.forEach(function(e) {
              var parentVertex = e.parent;
              if (e.type == 'CONTAINS') {
                var parentVertexNode = vertexIdMap[dagName + "/" + parentVertex];
                if (parentVertexNode != null && parentVertexNode.type == App.TezDagVertexType.UNION) {
                  // We flip the edges for Union vertices
                  var tmp = childVertex;
                  childVertex = parentVertex;
                  parentVertex = tmp;
                }
              }
              var edgeObj = {
                id : dagName + "/" + parentVertex + "-" + childVertex,
                from_vertex_id : dagName + "/" + parentVertex,
                to_vertex_id : dagName + "/" + childVertex
              };
              vertexIdMap[edgeObj.from_vertex_id].outgoing_edges.push(edgeObj.id);
              vertexIdMap[edgeObj.to_vertex_id].incoming_edges.push(edgeObj.id);
              edgeIds.push(edgeObj.id);
              switch (e.type) {
              case "BROADCAST_EDGE":
                edgeObj.edge_type = App.TezDagEdgeType.BROADCAST;
                break;
              case "SIMPLE_EDGE":
                edgeObj.edge_type = App.TezDagEdgeType.SCATTER_GATHER;
                break;
              case "CONTAINS":
                edgeObj.edge_type = App.TezDagEdgeType.CONTAINS;
                break;
              default:
                break;
              }
              edges.push(edgeObj);
            });
          }
          // Create records
          var tezDag = {
            id : dagName,
            name : dagName,
            stage : stage,
            vertices : vertexIds,
            edges : edgeIds
          }
          // Once the DAG is loaded, we do not need to
          // reload as the structure does not change. Reloading
          // here causes missing data (got from other calls)
          // to propagate into UI - causing flashing.
          var newVertices = [];
          var newEdges = [];
          vertices.forEach(function(v) {
            var vertexRecord = App.TezDagVertex.find(v.id);
            if (!vertexRecord.get('isLoaded')) {
              newVertices.push(v);
            }
          });
          edges.forEach(function(e) {
            var edgeRecord = App.TezDagEdge.find(e.id);
            if (!edgeRecord.get('isLoaded')) {
              newEdges.push(e);
            }
          });
          App.store.loadMany(App.TezDagVertex, newVertices);
          App.store.loadMany(App.TezDagEdge, newEdges);
          var dagRecord = App.TezDag.find(tezDag.id);
          if (!dagRecord.get('isLoaded')) {
            App.store.load(App.TezDag, tezDag);
          }
          hiveJob.tezDag = tezDag.id;
        }
      }
      if(App.HiveJob.find().get('content').length == 0){
        App.store.load(model, hiveJob);
      }
      var hiveJobRecord = App.HiveJob.find(hiveJob.id);
      if (hiveJobRecord != null) {
        hiveJobRecord.set('stages', hiveJob.stages.sortProperty('id'));
        hiveJobRecord.set('startTime', hiveJob.startTime);
        hiveJobRecord.set('endTime', hiveJob.endTime);
        if (hiveJob.tezDag != null) {
          // Some hive queries dont use Tez
          hiveJobRecord.set('tezDag', App.TezDag.find(hiveJob.tezDag));
        }
      }
    }
  },
  config : {}
});