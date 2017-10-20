/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['require',
  'utils/Globals',
  'models/BaseModel'
], function(require, Globals, vBaseModel) {
  'use strict';
  var VTopology = vBaseModel.extend({
    urlRoot: Globals.baseURL + '/api/v1/topology',

    defaults: {},

    serverSchema: {},

    idAttribute: 'id',

    initialize: function() {
      this.modelName = 'VTopology';
      this.bindErrorEvents();
    },
    toString: function() {
      return this.get('name');
    },
    getData: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '?window='+options.window + '&sys=' + options.sys, 'GET', options);
    },
    getGraphData: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/visualization?window='+options.window, 'GET', options);
    },
    getLogConfig: function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/logconfig', 'GET', options);
    },
    saveLogConfig:function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/logconfig', 'POST', options);
    },
    activateTopology: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/activate', 'POST', options);
    },
    deactivateTopology: function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/deactivate', 'POST', options);
    },
    rebalanceTopology: function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/rebalance/' + options.waitTime, 'POST', options);
    },
    killTopology: function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/kill/' + options.waitTime, 'POST', options);
    },
    getComponent: function(options) {
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/component/' + options.name + '?window='+options.window + '&sys=' + options.sys, 'GET', options);
    },
    debugTopology: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id + '/debug/' + options.debugType + '/' + options.percent, 'POST', options);
    },
    debugComponent: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id  + '/component/' + options.name + '/debug/' + options.debugType + '/' + options.percent, 'POST', options);
    },
    profileJStack: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id  + '/profiling/dumpjstack/' + options.hostPort, 'GET', options);
    },
    profileRestartWorker: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id  + '/profiling/restartworker/' + options.hostPort, 'GET', options);
    },
    profileHeap: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id  + '/profiling/dumpheap/' + options.hostPort, 'GET', options);
    },
    getTopologyLag: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology/' + options.id  + '/lag', 'GET', options);
    },
    getWorkerHost: function(options){
      return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/topology-workers/' + options.id, 'GET', options);
    },
  }, {});
  return VTopology;
});