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

App.TezDag = DS.Model.extend({

  /**
   * When DAG is actually running on server, a unique ID is assigned.
   */
  instanceId: DS.attr('string'),

  name: DS.attr('string'),

  yarnApplicationId: DS.attr('string'),

  yarnApplicationLink: DS.attr('string'),

  stage: DS.attr('string'),

  vertices: DS.hasMany('tezDagVertex'),

  edges: DS.hasMany('tezDagEdge'),

  hiveJob: DS.belongsTo('hiveJob')

});

App.TezDagEdge = DS.Model.extend({

  instanceId: DS.attr('string'),

  fromVertex: DS.belongsTo('tezDagVertex'),

  toVertex: DS.belongsTo('tezDagVertex'),

  /**
   * Type of this edge connecting vertices. Should be one of constants defined
   * in 'App.TezDagEdgeType'.
   */
  edgeType: DS.attr('string'),

  tezDag: DS.belongsTo('tezDag')

});

App.TezDagVertex = DS.Model.extend({

  /**
   * When DAG vertex is actually running on server, a unique ID is assigned.
   */
  instanceId: DS.attr('string'),

  name: DS.attr('string'),

  tezDag: DS.belongsTo('tezDag'),

  /**
   * State of this vertex. Should be one of constants defined in
   * App.TezDagVertexState.
   */
  state: DS.attr('string'),

  /**
   * Vertex type has to be one of the types defined in 'App.TezDagVertexType'
   * @return {string}
   */
  type: DS.attr('string'),

  /**
   * A vertex can have multiple incoming edges.
   */
  incomingEdges: DS.hasMany('tezDagEdge'),

  /**
   * This vertex can have multiple outgoing edges.
   */
  outgoingEdges: DS.hasMany('tezDagEdge'),

  startTime: DS.attr('number'),

  endTime: DS.attr('number'),

  /**
   * Provides the duration of this job. If the job has not started, duration
   * will be given as 0. If the job has not ended, duration will be till now.
   *
   * @return {Number} Duration in milliseconds.
   */
  duration: function () {
    return App.Helpers.date.duration(this.get('startTime'), this.get('endTime'))
  }.property('startTime', 'endTime'),

  /**
   * Each Tez vertex can perform arbitrary application specific computations
   * inside. The application can provide a list of operations it has provided in
   * this vertex.
   *
   * Array of strings. [{string}]
   */
  operations: DS.attr('array'),

  /**
   * Provides additional information about the 'operations' performed in this
   * vertex. This is shown directly to the user.
   */
  operationPlan: DS.attr('string'),

  /**
   * Number of actual Map/Reduce tasks in this vertex
   */
  tasksCount: DS.attr('number'),

  tasksNumber: function () {
    return this.getWithDefault('tasksCount', 0);
  }.property('tasksCount'),

  /**
   * Local filesystem usage metrics for this vertex
   */
  fileReadBytes: DS.attr('number'),

  fileWriteBytes: DS.attr('number'),

  fileReadOps: DS.attr('number'),

  fileWriteOps: DS.attr('number'),

  /**
   * Spilled records
   */
  spilledRecords: DS.attr('number'),

  /**
   * HDFS usage metrics for this vertex
   */
  hdfsReadBytes: DS.attr('number'),

  hdfsWriteBytes: DS.attr('number'),

  hdfsReadOps: DS.attr('number'),

  hdfsWriteOps: DS.attr('number'),

  /**
   * Record metrics for this vertex
   */
  recordReadCount: DS.attr('number'),

  recordWriteCount: DS.attr('number'),

  totalReadBytes: function () {
    return (this.get('fileReadBytes') || 0) + (this.get('hdfsReadBytes') || 0);
  }.property('fileReadBytes', 'hdfsReadBytes'),

  totalWriteBytes: function () {
    return (this.get('fileWriteBytes') || 0) + (this.get('hdfsWriteBytes') || 0);
  }.property('fileWriteBytes', 'hdfsWriteBytes'),

  totalReadBytesDisplay: function () {
    return  App.Helpers.number.bytesToSize(this.get('totalReadBytes'));
  }.property('totalReadBytes'),

  totalWriteBytesDisplay: function () {
    return  App.Helpers.number.bytesToSize(this.get('totalWriteBytes'));
  }.property('totalWriteBytes'),

  durationDisplay: function () {
    return App.Helpers.date.timingFormat(this.get('duration'), true);
  }.property('duration')

});

App.TezDagVertexState = {
  NEW: "NEW",
  INITIALIZING: "INITIALIZING",
  INITED: "INITED",
  RUNNING: "RUNNING",
  SUCCEEDED: "SUCCEEDED",
  FAILED: "FAILED",
  KILLED: "KILLED",
  ERROR: "ERROR",
  TERMINATING: "TERMINATING",
  JOBFAILED: "JOB FAILED"
};

App.TezDagVertexType = {
  MAP: 'MAP',
  REDUCE: 'REDUCE',
  UNION: 'UNION'
};

App.TezDagEdgeType = {
  SCATTER_GATHER: "SCATTER_GATHER",
  BROADCAST: "BROADCAST",
  CONTAINS: "CONTAINS"
};

App.TezDag.FIXTURES = [];
App.TezDagEdge.FIXTURES = [];
App.TezDagVertex.FIXTURES = [];