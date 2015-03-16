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

App.Helpers.jobs = {

  /**
   * Refreshes the latest information for a given job
   *
   * @param {App.AbstractJob} job
   * @param {Function} successCallback
   * @param {Function} errorCallback (errorId) Called in error cases where there is no
   *          data from server. 'errorId' can be one of
   *          <ul>
   *            <li>job.dag.noId</li>
   *            <li>job.dag.noname</li>
   *            <li>job.dag.id.noDag</li>
   *            <li>job.dag.id.loaderror</li>
   *            <li>job.dag.name.loaderror</li>
   *          </ul>
   */
  refreshJobDetails: function (job, successCallback, errorCallback) {
    this.refreshHiveJobDetails(job, successCallback, errorCallback);
  },

  /**
   * Refreshes latest information of a Hive Job.
   *
   * @param {App.HiveJob} hiveJob
   * @param {Function} successCallback
   * @param {Function} errorCallback @see #refreshJobDetails()
   * @method refreshHiveJobDetails
   */
  refreshHiveJobDetails: function (hiveJob, successCallback, errorCallback) {
    var  atsURL = App.get('atsURL') || 'http://' + App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER').get('hostName') +
        ':' + App.HiveJob.store.getById('service', 'YARN').get('ahsWebPort');

    return App.ajax.send({
      name: 'job_details',
      sender: this,
      data: {
        atsURL: atsURL,
        job_id: hiveJob.get('id'),
        view: App.get("view"),
        version: App.get("version"),
        instanceName: App.get("instanceName"),
        successCallback: successCallback,
        errorCallback: errorCallback
      },
      success: 'refreshHiveJobDetailsSuccessCallback'
    });

  },

  refreshHiveJobDetailsSuccessCallback: function (data, opt, params) {
    App.hiveJobMapper.map(data);
    var hiveRecord = App.HiveJob.store.getById('hiveJob', params.job_id),
      tezDagName = hiveRecord.get('tezDag.name'),
      self = this;
    if (!Em.isNone(tezDagName)) {
      var sender = {
        dagNameToIdSuccess: function (data) {
          if (data && data.entities && data.entities.length > 0) {
            var dagId = data.entities[0].entity;
            hiveRecord.get('tezDag').set('instanceId', dagId);
            self.refreshTezDagDetails(tezDagName, params.successCallback, params.errorCallback);
          }
          else {
            params.errorCallback('job.dag.noId');
          }
        },
        dagNameToIdError: function () {
          params.errorCallback('job.dag.name.loaderror');
        }
      };
      App.ajax.send({
        name: 'jobs.tezDag.NametoID',
        sender: sender,
        data: {
          atsURL: params.atsURL,
          tezDagName: tezDagName,
          view: App.get("view"),
          version: App.get("version"),
          instanceName: App.get("instanceName")
        },
        success: 'dagNameToIdSuccess',
        error: 'dagNameToIdError'
      });
    }
    else {
      params.errorCallback('job.dag.noname');
    }
  },

  /**
   * Refreshes runtime information of a Tez DAG based on events generated.
   * The instance ID of the Tez DAG should be set.
   *
   * @param {string} tezDagId ID of the Tez DAG. Example: 'HIVE-Q2:1'
   * @param {Function} successCallback
   * @param {Function} errorCallback @see #refreshJobDetails()
   * @method refreshTezDagDetails
   */
  refreshTezDagDetails: function (tezDagId, successCallback, errorCallback) {
    var self = this,
        atsURL = App.get('atsURL') || 'http://' + App.HiveJob.store.getById('component', 'RESOURCEMANAGER').get('hostName') + ':' + App.HiveJob.store.getById('service', 'YARN').get('ahsWebPort'),
        resourceManager = App.HiveJob.store.getById('component', 'RESOURCEMANAGER'),
        resourceManagerHostName = App.get('resourceManagerURL') || (resourceManager && 'http://' + resourceManager.get('hostName') + ':8088') || '',
        tezDag = App.HiveJob.store.getById('tezDag', tezDagId);
    if (tezDag) {
      var tezDagInstanceId = tezDag.get('instanceId'),
          sender = {
            loadTezDagSuccess: function (data) {
              if (data) {
                var app_id = Em.get(data, 'otherinfo.applicationId');
                if (app_id && resourceManagerHostName) {
                  tezDag.set('yarnApplicationId', app_id);
                  tezDag.set('yarnApplicationLink', resourceManagerHostName + '/cluster/app/' + app_id);
                }
                if (data.relatedentities && data.relatedentities.TEZ_VERTEX_ID != null) {
                  var count = data.relatedentities.TEZ_VERTEX_ID.length;
                  data.relatedentities.TEZ_VERTEX_ID.forEach(function (v) {
                    self.refreshTezDagVertex(tezDagId, v, function () {
                      if (--count <= 0) {
                        // all vertices succeeded
                        successCallback();
                      }
                    });
                  });
                }
              }
            },
            loadTezDagError: function () {
              errorCallback('job.dag.id.loaderror');
            }
          };
      App.ajax.send({
        name: 'jobs.tezDag.tezDagId',
        sender: sender,
        data: {
          tezDagId: tezDagInstanceId,
          atsURL: atsURL,
          view: App.get("view"),
          version: App.get("version"),
          instanceName: App.get("instanceName")
        },
        success: 'loadTezDagSuccess',
        error: 'loadTezDagError'
      });
    }
    else {
      errorCallback('job.dag.id.noDag');
    }
  },

  /**
   * Refreshes runtime information of the given vertex.
   *
   * @param {string} tezDagId ID of the Tez DAG. Exmaple: 'HIVE-Q2:1'
   * @param {string} tezVertexInstanceId Instance ID of the vertex to refresh. Example 'vertex_1390516007863_0001_1_00'
   * @param {Function} successCallback
   * @method refreshTezDagVertex
   */
  refreshTezDagVertex: function (tezDagId, tezVertexInstanceId, successCallback) {
    var atsURL = App.get('atsURL') || 'http://' + App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER').get('hostName') + ':' + App.HiveJob.store.getById('service', 'YARN').get('ahsWebPort'),
      tezDag = App.HiveJob.store.getById('tezDag', tezDagId),
      hiveJob = App.HiveJob.store.all('hiveJob').findBy('tezDag', tezDag),
      hiveJobFailed = hiveJob.get('failed'),
      hiveJobEndTime = hiveJob.get('endTime'),
      sender = {
        loadTezDagVertexSuccess: function (data) {
          if (data && data.otherinfo) {
            var vertexRecord = App.HiveJob.store.getById('tezDagVertex', tezDagId + "/" + data.otherinfo.vertexName);
            if (vertexRecord != null) {
              vertexRecord.set('startTime', data.otherinfo.startTime);
              if (data.otherinfo.endTime == undefined && hiveJobFailed) {
                vertexRecord.set('endTime', hiveJobEndTime);
              }
              else {
                vertexRecord.set('endTime', data.otherinfo.endTime);
              }
              vertexRecord.set('tasksCount', data.otherinfo.numTasks);
              if (data.otherinfo.status == null && hiveJobFailed) {
                vertexRecord.set('state', Em.I18n.t('jobs.hive.failed'));
              }
              else {
                vertexRecord.set('state', data.otherinfo.status);
              }
              if (data.otherinfo.counters && data.otherinfo.counters.counterGroups) {
                data.otherinfo.counters.counterGroups.forEach(function (cGroup) {
                  var cNameToPropetyMap = {};
                  switch (cGroup.counterGroupName) {
                    case 'org.apache.tez.common.counters.FileSystemCounter':
                      cNameToPropetyMap = {
                        'FILE_BYTES_READ': 'fileReadBytes',
                        'FILE_BYTES_WRITTEN': 'fileWriteBytes',
                        'FILE_READ_OPS': 'fileReadOps',
                        'FILE_WRITE_OPS': 'fileWriteOps',
                        'HDFS_BYTES_READ': 'hdfsReadBytes',
                        'HDFS_BYTES_WRITTEN': 'hdfsWriteBytes',
                        'HDFS_READ_OPS': 'hdfsReadOps',
                        'HDFS_WRITE_OPS': 'hdfsWriteOps'
                      };
                      break;
                    case 'org.apache.tez.common.counters.TaskCounter':
                      cNameToPropetyMap = {
                        'SPILLED_RECORDS': 'spilledRecords'
                      };
                      break;
                    case 'HIVE':
                      var vertexNameFormatted = App.Helpers.string.convertSpacesToUnderscores(data.otherinfo.vertexName);
                      cNameToPropetyMap = {};
                      cNameToPropetyMap['RECORDS_IN_' + vertexNameFormatted] = 'recordReadCount';
                      cNameToPropetyMap['RECORDS_OUT_' + vertexNameFormatted] = 'recordWriteCount';
                      break;
                    default:
                      break;
                  }
                  if (cGroup.counters) {
                    cGroup.counters.forEach(function (counter) {
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
        loadTezDagVertexError: function (jqXHR, url, method, showStatus) {}
      };
    App.ajax.send({
      name: 'jobs.tezDag.tezDagVertexId',
      sender: sender,
      data: {
        atsURL: atsURL,
        tezDagVertexId: tezVertexInstanceId,
        view: App.get("view"),
        version: App.get("version"),
        instanceName: App.get("instanceName")
      },
      success: 'loadTezDagVertexSuccess',
      error: 'loadTezDagVertexError'
    });
  }
};
