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
   * @param {Function}
   *          successCallback
   * @param {Funtion}
   *          errorCallback(errorId) Called in error cases where there is no
   *          data from server. 'errorId' can be one of
   *          <ul>
   *            <li>job.dag.noId</li>
   *            <li>job.dag.noname</li>
   *            <li>job.dag.id.noDag</li>
   *            <li>job.dag.id.loaderror</li>
   *            <li>job.dag.name.loaderror</li>
   *          </ul>
   */
  refreshJobDetails : function(job, successCallback, errorCallback) {
    this.refreshHiveJobDetails(job, successCallback, errorCallback);
  },

  /**
   * Refreshes latest information of a Hive Job.
   *
   * @param {App.HiveJob}
   *          hiveJob
   * @param {Function}
   *          successCallback
   * @param {Function}
   *          errorCallback @see #refreshJobDetails()
   */
  refreshHiveJobDetails : function(hiveJob, successCallback, errorCallback) {
    var self = this;
    // TODO - to be changed to history server when implemented in stack.
    var yarnService = App.YARNService.find().objectAt(0);
    var historyServerHostName = yarnService.get('appTimelineServerNode.hostName');
    var ahsWebPort = yarnService.get('ahsWebPort');
    var hiveJobId = hiveJob.get('id');
    // First refresh query
    var hiveQueriesUrl = App.testMode ? "/data/jobs/hive-query-2.json" : "/proxy?url=http://" + historyServerHostName
        + ":" + ahsWebPort + "/ws/v1/timeline/HIVE_QUERY_ID/" + hiveJob.get('id') + "?fields=otherinfo";
    App.HttpClient.get(hiveQueriesUrl, App.hiveJobMapper, {
      complete : function(jqXHR, textStatus) {
        // Now get the Tez DAG ID from the DAG name
        var hiveRecord = App.HiveJob.find(hiveJobId);
        App.router.get('mainHiveJobDetailsController').set('job', hiveRecord);
        var tezDagName = hiveRecord.get('tezDag.name');
        if (tezDagName != null) {
          var sender = {
            dagNameToIdSuccess : function(data) {
              if (data && data.entities && data.entities.length > 0) {
                var dagId = data.entities[0].entity;
                App.TezDag.find(tezDagName).set('instanceId', dagId);
                self.refreshTezDagDetails(tezDagName, successCallback, errorCallback);
              }else{
                console.log('No Tez DAG was found for DAG name of ' + tezDagName);
                errorCallback('job.dag.noId');
              }
            },
            dagNameToIdError : function(jqXHR, url, method, showStatus) {
              App.ajax.defaultErrorHandler(jqXHR, url, method, showStatus);
              errorCallback('job.dag.name.loaderror');
            }
          }
          App.ajax.send({
            name : 'jobs.tezDag.NametoID',
            sender : sender,
            data : {
              historyServerHostName : historyServerHostName,
              tezDagName : tezDagName,
              ahsWebPort: ahsWebPort
            },
            success : 'dagNameToIdSuccess',
            error : 'dagNameToIdError'
          });
        } else {
          console.log('No Tez DAG name to ask ID for. Job ID: ' + hiveJobId);
          errorCallback('job.dag.noname');
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
   * @param {Function}
   *          successCallback
   * @param {Function}
   *          errorCallback @see #refreshJobDetails()
   */
  refreshTezDagDetails : function(tezDagId, successCallback, errorCallback) {
    var self = this;
    var yarnService = App.YARNService.find().objectAt(0);
    var ahsWebPort = yarnService.get('ahsWebPort');
    var historyServerHostName = yarnService.get('appTimelineServerNode.hostName');
    var tezDag = App.TezDag.find(tezDagId);
    if (tezDag) {
      var tezDagInstanceId = tezDag.get('instanceId');
      var sender = {
        loadTezDagSuccess : function(data) {
          if (data && data.relatedentities && data.relatedentities.TEZ_VERTEX_ID != null) {
            var count = data.relatedentities.TEZ_VERTEX_ID.length;
            data.relatedentities.TEZ_VERTEX_ID.forEach(function(v) {
              self.refreshTezDagVertex(tezDagId, v, function() {
                if (--count <= 0) {
                  // all vertices succeeded
                  successCallback();
                }
              });
            });
          }
        },
        loadTezDagError : function(jqXHR, url, method, showStatus) {
          App.ajax.defaultErrorHandler(jqXHR, url, method, showStatus);
          errorCallback('job.dag.id.loaderror');
        }
      }
      App.ajax.send({
        name : 'jobs.tezDag.tezDagId',
        sender : sender,
        data : {
          historyServerHostName : historyServerHostName,
          tezDagId : tezDagInstanceId,
          ahsWebPort: ahsWebPort
        },
        success : 'loadTezDagSuccess',
        error : 'loadTezDagError'
      });
    } else {
      console.log('Unable to find DAG for ID: ' + tezDagId);
      errorCallback('job.dag.id.noDag');
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
   * @param {Function}
   *          successCallback
   */
  refreshTezDagVertex : function(tezDagId, tezVertexInstanceId, successCallback) {
    var yarnService = App.YARNService.find().objectAt(0);
    var ahsWebPort = yarnService.get('ahsWebPort');
    var historyServerHostName = yarnService.get('appTimelineServerNode.hostName');
    var sender = {
      loadTezDagVertexSuccess : function(data) {
        if (data && data.otherinfo) {
          var vertexRecord = App.TezDagVertex.find(tezDagId + "/" + data.otherinfo.vertexName);
          if (vertexRecord != null) {
            vertexRecord.set('startTime', data.otherinfo.startTime);
            vertexRecord.set('endTime', data.otherinfo.endTime);
            vertexRecord.set('tasksCount', data.otherinfo.numTasks);
            vertexRecord.set('state', data.otherinfo.status);
            if (data.otherinfo.counters && data.otherinfo.counters.counterGroups) {
              data.otherinfo.counters.counterGroups.forEach(function(cGroup) {
                var cNameToPropetyMap = {};
                switch (cGroup.counterGroupName) {
                case 'org.apache.tez.common.counters.FileSystemCounter':
                  cNameToPropetyMap = {
                    'FILE_BYTES_READ' : 'fileReadBytes',
                    'FILE_BYTES_WRITTEN' : 'fileWriteBytes',
                    'FILE_READ_OPS' : 'fileReadOps',
                    'FILE_WRITE_OPS' : 'fileWriteOps',
                    'HDFS_BYTES_READ' : 'hdfsReadBytes',
                    'HDFS_BYTES_WRITTEN' : 'hdfsWriteBytes',
                    'HDFS_READ_OPS' : 'hdfsReadOps',
                    'HDFS_WRITE_OPS' : 'hdfsWriteOps'
                  };
                  break;
                case 'org.apache.tez.common.counters.TaskCounter':
                  cNameToPropetyMap = {
                     'SPILLED_RECORDS' : 'spilledRecords'
                    };
                  break;
                case 'HIVE':
                  cNameToPropetyMap = {
                    'RECORDS_READ' : 'recordReadCount',
                    'RECORDS_WRITE' : 'recordWriteCount'
                  };
                  break;
                default:
                  break;
                }
                if (cGroup.counters) {
                  cGroup.counters.forEach(function(counter) {
                    var prop = cNameToPropetyMap[counter.counterName];
                    if (prop != null) {
                      vertexRecord.set(prop, counter.counterValue);
                    }
                  });
                }
              });
            }
            successCallback();
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
        tezDagVertexId : tezVertexInstanceId,
        ahsWebPort: ahsWebPort
      },
      success : 'loadTezDagVertexSuccess',
      error : 'loadTezDagVertexError'
    });
  }
};
