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

var App = require('app');

App.UpdateController = Em.Controller.extend({
  name:'updateController',
  isUpdated:false,
  cluster:null,
  isWorking: false,
  timeIntervalId: null,
  clusterName:function () {
    return App.router.get('clusterController.clusterName');
  }.property('App.router.clusterController.clusterName'),

  getUrl:function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + this.get('clusterName') + url;
  },

  /**
   * Wrapper for all updates
   */
  updateAll:function(){
    if(this.get('isWorking')) {
      this.update('updateHost');
      this.update('updateServiceMetric');
      this.update('graphsUpdate');
    }
  }.observes('isWorking'),

  /**
   * States for each update method (each field - method name)
   */
  states: {
    'updateHost': null,
    'updateServiceMetric': null,
    'graphsUpdate': null
  },

  /**
   * Callback for each update method
   * @param {String} name - state name
   * @return {Function}
   */
  updateCallback: function(name) {
    var self = this;
    return function() {
      self.update(name);
    }
  },

  /**
   * Common method that executes provided by name update method (name from states object)
   * @param {String} name - key in the states object
   * @return {Boolean}
   */
  update: function(name) {
    if(!this.get('isWorking')) {
      return false;
    }
    clearTimeout(this.states[name]);
    var self = this;
    this.states[name] = setTimeout(function() {
      self[name](self.updateCallback(name));
    }, App.contentUpdateInterval);
  },

  updateHost:function(callback) {
    var self = this;
      var hostsUrl = this.getUrl('/data/hosts/hosts.json', '/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/cpu_count,Hosts/total_mem,Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/load/load_one');
      App.HttpClient.get(hostsUrl, App.hostsMapper, {
        complete:function (jqXHR, textStatus) {
          callback();
        }
      });
  },
  graphs: [],
  graphsUpdate: function (callback) {
      var existedGraphs = [];
      this.get('graphs').forEach(function (_graph) {
        var view = Em.View.views[_graph.id];
        if (view) {
          existedGraphs.push(_graph);
          //console.log('updated graph', _graph.name);
          view.loadData();
          //if graph opened as modal popup update it to
          if($(".modal-graph-line .modal-body #" + _graph.popupId + "-container-popup").length) {
            view.loadData();
          }
        }
      });
    callback();
    this.set('graphs', existedGraphs);
  },

  /**
   * Updates the services information. 
   *
   * @param callback
   * @param isInitialLoad  If true, only basic information is loaded.
   */
  updateServiceMetric: function (callback, isInitialLoad) {
    var self = this;
    self.set('isUpdated', false);
    var servicesUrl = isInitialLoad ? 
        this.getUrl('/data/dashboard/services.json', '/services?fields=components/ServiceComponentInfo,components/host_components,components/host_components/HostRoles') : 
        this.getUrl('/data/dashboard/services.json', '/services?fields=components/ServiceComponentInfo,components/host_components,components/host_components/HostRoles,components/host_components/metrics/jvm/memHeapUsedM,components/host_components/metrics/jvm/memHeapCommittedM,components/host_components/metrics/mapred/jobtracker/trackers_decommissioned');
    var callback = callback || function (jqXHR, textStatus) {
      self.set('isUpdated', true);
    };
      App.HttpClient.get(servicesUrl, App.servicesMapper, {
        complete: function() {
          callback();
        }
      });
  }


});
