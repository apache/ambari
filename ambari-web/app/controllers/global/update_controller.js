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
   * Start polling, when <code>isWorking</code> become true
   */
  updateAll:function(){
    if(this.get('isWorking')) {
      App.updater.run(this, 'updateHostConditionally', 'isWorking');
      App.updater.run(this, 'updateServiceMetric', 'isWorking');
      App.updater.run(this, 'graphsUpdate', 'isWorking');
      if (App.supports.hostOverrides) {
        App.updater.run(this, 'updateComponentConfig', 'isWorking');
      }
    }
  }.observes('isWorking'),
  updateHostConditionally: function (callback) {
    var location = App.router.get('location.lastSetURL');
    if (/\/main\/hosts.*/.test(location)) {
      this.updateHost(callback);
    } else {
      callback();
    }
  },

  updateHost:function(callback) {
    var testUrl = App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hosts.json' : '/data/hosts/hosts.json';
    var hostsUrl = this.getUrl(testUrl, '/hosts?fields=Hosts/host_name,Hosts/last_heartbeat_time,' +
      'metrics/disk,metrics/load/load_one,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free');
    App.HttpClient.get(hostsUrl, App.hostsMapper, {
      complete: callback
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

    var conditionalFields = [];
    var initialFields = ['components/host_components/HostRoles/state'];
    var services = [
        {
            name: 'FLUME',
            urlParam: 'flume/flume'
        },
        {
            name: 'YARN',
            urlParam: 'yarn/Queue'
        },
        {
          name: 'HBASE',
          urlParam: 'hbase/master/IsActiveMaster'
        }
    ];
    services.forEach(function(service) {
        if (App.Service.find(service.name)) {
            conditionalFields.push("components/host_components/metrics/" + service.urlParam);
        }
    });
    var conditionalFieldsString = conditionalFields.length > 0 ? ',' + conditionalFields.join(',') : '';
    var initialFieldsString = initialFields.length > 0 ? ',' + initialFields.join(',') : '';
    var methodStartTs = new Date().getTime();
    var testUrl = App.get('isHadoop2Stack') ? '/data/dashboard/HDP2/services.json':'/data/dashboard/services.json';

    var realUrl = '/services?fields=components/ServiceComponentInfo,' +
      'components/host_components/metrics/jvm/memHeapUsedM,' +
      'components/host_components/metrics/jvm/memHeapCommittedM,' +
      'components/host_components/metrics/mapred/jobtracker/trackers_decommissioned,' +
      'components/host_components/metrics/cpu/cpu_wio,' +
      'components/host_components/metrics/rpc/RpcQueueTime_avg_time,' +
      'components/host_components/metrics/dfs/FSNamesystem/HAState,' +
      'components/host_components/metrics/dfs/FSNamesystem/CapacityUsed,' +
      'components/host_components/metrics/dfs/FSNamesystem/CapacityTotal,' +
      'components/host_components/metrics/dfs/FSNamesystem/CapacityRemaining,' +
      'components/host_components/metrics/dfs/FSNamesystem/BlocksTotal,' +
      'components/host_components/metrics/dfs/FSNamesystem/CorruptBlocks,' +
      'components/host_components/metrics/dfs/FSNamesystem/MissingBlocks,' +
      'components/host_components/metrics/dfs/FSNamesystem/UnderReplicatedBlocks,' +
      'components/host_components/metrics/dfs/namenode/Version,' +
      'components/host_components/metrics/dfs/namenode/LiveNodes,' +
      'components/host_components/metrics/dfs/namenode/DeadNodes,' +
      'components/host_components/metrics/dfs/namenode/DecomNodes,' +
      'components/host_components/metrics/dfs/namenode/TotalFiles,' +
      'components/host_components/metrics/dfs/namenode/UpgradeFinalized,' +
      'components/host_components/metrics/dfs/namenode/Safemode,' +
      'components/host_components/metrics/runtime/StartTime' +
      conditionalFieldsString;

    var servicesUrl = isInitialLoad ? this.getUrl(testUrl, realUrl + initialFieldsString) : this.getUrl(testUrl, realUrl);
    var callback = callback || function (jqXHR, textStatus) {
      self.set('isUpdated', true);
    };
    App.HttpClient.get(servicesUrl, App.servicesMapper, {
      complete: function(){
        console.log("UpdateServiceMetric() Finished in:"+ (new Date().getTime()-methodStartTs) + " ms");
        callback();
      }
    });
  },
  updateComponentConfig: function (callback) {
    var testUrl = '/data/services/host_component_stale_configs.json';
    var componentConfigUrl = this.getUrl(testUrl, '/host_components?fields=HostRoles/component_name&HostRoles/stale_configs=true');
    App.HttpClient.get(componentConfigUrl, App.componentConfigMapper, {
      complete: callback
    });
  }

});
