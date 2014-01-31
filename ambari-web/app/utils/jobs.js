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
module.exports = {

  /**
   * Refreshes the latest information for a given job
   *
   * @param {App.AbstractJob}
   *          job
   */
  refreshJobDetails : function(job) {
    if (job) {
      switch (job.get('jobType')) {
      case App.JobType.HIVE:
        this.refreshHiveJobDetails(job);
        break;
      default:
        break;
      }
    }
  },

  /**
   * Refreshes latest information of a Hive Job.
   *
   * @param {App.HiveJob}
   *          hiveJob
   */
  refreshHiveJobDetails : function(hiveJob) {
    var self = this;
    // TODO - to be changed to history server when implemented in stack.
    var historyServerHostName = App.YARNService.find().objectAt(0).get('resourceManagerNode.hostName')
    var hiveJobId = hiveJob.get('id');
    // First refresh query
    var hiveQueriesUrl = App.testMode ? "/data/jobs/hive-query-2.json" :
    App.apiPrefix + "/proxy?url=http://"+historyServerHostName+":8188/ws/v1/apptimeline/HIVE_QUERY_ID/" + hiveJob.get('id')+"?fields=otherinfo";
    App.HttpClient.get(hiveQueriesUrl, App.hiveJobMapper, {
      complete : function(jqXHR, textStatus) {
        // Now get the Tez DAG ID from the DAG name
        var hiveRecord = App.HiveJob.find(hiveJobId);
        var tezDagName = hiveRecord.get('tezDag.name');
        if (tezDagName != null) {
          var sender = {
            dagNameToIdSuccess : function(data) {
              if (data && data.entities && data.entities.length > 0) {
                var dagId = data.entities[0].entity;
                App.TezDag.find(tezDagName).set('instanceId', dagId);
                self.refreshTezDagDetails(tezDagName);
              }
            },
            dagNameToIdError : function(jqXHR, url, method, showStatus) {
              App.ajax.defaultErrorHandler(jqXHR, url, method, showStatus);
            }
          }
          App.ajax.send({
            name : 'jobs.tezDag.NametoID',
            sender : sender,
            data : {
              historyServerHostName : historyServerHostName,
              tezDagName : tezDagName
            },
            success : 'dagNameToIdSuccess',
            error : 'dagNameToIdError'
          });
        } else {
          // TODO
        }

      }
    });
  },

  /**
   * Refreshes runtime information of a Tez DAG based on events generated. The
   * instance ID of the Tez DAG should be set.
   *
   * @param {string}
   *          tezDagId ID of the Tez DAG. Example: 'HIVE-Q2:1'
   */
  refreshTezDagDetails : function(tezDagId) {
    var self = this;
    var historyServerHostName = App.YARNService.find().objectAt(0).get('resourceManagerNode.hostName');
    var tezDag = App.TezDag.find(tezDagId);
    if (tezDag) {
      var tezDagInstanceId = tezDag.get('instanceId');
      var sender = {
        loadTezDagSuccess : function(data) {
          if (data && data.relatedentities && data.relatedentities.TEZ_VERTEX_ID != null) {
            data.relatedentities.TEZ_VERTEX_ID.forEach(function(v) {
              self.refreshTezDagVertex(tezDagId, v);
            });
          }
        },
        loadTezDagError : function(jqXHR, url, method, showStatus) {
          App.ajax.defaultErrorHandler(jqXHR, url, method, showStatus);
        }
      }
      App.ajax.send({
        name : 'jobs.tezDag.tezDagId',
        sender : sender,
        data : {
          historyServerHostName : historyServerHostName,
          tezDagId : tezDagInstanceId
        },
        success : 'loadTezDagSuccess',
        error : 'loadTezDagError'
      });
    }
  },

  /**
   * Refreshes runtime information of the given vertex.
   *
   * @param {string}
   *          tezDagId ID of the Tez DAG. Exmaple: 'HIVE-Q2:1'
   * @param {string}
   *          tezVertexInstanceID Instance ID of the vertex to refresh. Example
   *          'vertex_1390516007863_0001_1_00'
   */
  refreshTezDagVertex : function(tezDagId, tezVertexInstanceId) {
    var historyServerHostName = App.YARNService.find().objectAt(0).get('resourceManagerNode.hostName');
    var sender = {
      loadTezDagVertexSuccess : function(data) {
        if (data && data.otherinfo) {
          var vertexRecord = App.TezDagVertex.find(tezDagId + "/" + data.otherinfo.vertexName);
          if (vertexRecord != null) {
            vertexRecord.set('startTime', data.otherinfo.startTime);
            vertexRecord.set('endTime', data.otherinfo.startTime + +data.otherinfo.timeTaken);
            vertexRecord.set('tasksCount', data.otherinfo.numTasks);
            vertexRecord.set('state', data.otherinfo.status);
            // TODO Need additional vertex metrics
            vertexRecord.set('fileReadBytes', 0);
            vertexRecord.set('fileReadOps', 0);
            vertexRecord.set('fileWriteOps', 0);
            vertexRecord.set('fileWriteBytes', 0);
            vertexRecord.set('hdfsReadOps', 0);
            vertexRecord.set('hdfsReadBytes', 0);
            vertexRecord.set('hdfsWriteOps', 0);
            vertexRecord.set('hdfsWriteBytes', 0);
            vertexRecord.set('recordReadCount', 0);
            vertexRecord.set('recordWriteCount', 0);
          }
        }
      },
      loadTezDagVertexError : function(jqXHR, url, method, showStatus) {
        App.ajax.defaultErrorHandler(jqXHR, url, method, showStatus);
      }
    }
    App.ajax.send({
      name : 'jobs.tezDag.tezDagVertexId',
      sender : sender,
      data : {
        historyServerHostName : historyServerHostName,
        tezDagVertexId : tezVertexInstanceId
      },
      success : 'loadTezDagVertexSuccess',
      error : 'loadTezDagVertexError'
    });
  }
};
