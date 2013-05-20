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

App.ClusterController = Em.Controller.extend({
  name:'clusterController',
  cluster:null,
  isLoaded:false,
  clusterDataLoadedPercent: 'width:0', // 0 to 1
  /**
   * Whether we need to update statuses automatically or not
   */
  isWorking: false,
  updateLoadStatus:function (item) {
    var loadList = this.get('dataLoadList');
    var loaded = true;
    var numLoaded = 0;
    var loadListLength = 0;
    loadList.set(item, true);
    for (var i in loadList) {
      if (loadList.hasOwnProperty(i)) {
        loadListLength++;
        if(!loadList[i] && loaded){
          loaded = false;
        }
      }
      // calculate the number of true
      if (loadList.hasOwnProperty(i) && loadList[i]){
        numLoaded++;
      }
    }
    this.set('isLoaded', loaded);
    this.set('clusterDataLoadedPercent', 'width:' + (Math.floor(numLoaded / loadListLength * 100)).toString() + '%');
  },

  dataLoadList:Em.Object.create({
    'hosts':false,
    'services':false,
    'cluster':false,
    'racks':false,
    'alerts':false,
    'users':false,
    'datasets':false,
    'targetclusters':false

  }),

  /**
   * load cluster name
   */
  loadClusterName:function (reload) {
    if (this.get('clusterName') && !reload) {
      return;
    }

    App.ajax.send({
      name: 'cluster.load_cluster_name',
      sender: this,
      success: 'loadClusterNameSuccessCallback',
      error: 'loadClusterNameErrorCallback'
    });

    if(!App.get('currentStackVersion')){
      App.set('currentStackVersion', App.defaultStackVersion);
    }
  },

  loadClusterNameSuccessCallback: function (data) {
    this.set('cluster', data.items[0]);
    App.set('clusterName', data.items[0].Clusters.cluster_name);
    App.set('currentStackVersion', data.items[0].Clusters.version);
  },

  loadClusterNameErrorCallback: function (request, ajaxOptions, error) {
    console.log('failed on loading cluster name');
    this.set('isLoaded', true);
  },

  getUrl:function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + this.get('clusterName') + url;
  },

  /**
   * Provides the URL to use for Ganglia server. This URL
   * is helpful in populating links in UI.
   *
   * If null is returned, it means GANGLIA service is not installed.
   */
  gangliaUrl: function () {
    if (App.testMode) {
      return 'http://gangliaserver/ganglia/?t=yes';
    } else {
      // We want live data here
      var svcs = App.Service.find();
      var gangliaSvc = svcs.findProperty("serviceName", "GANGLIA");
      if (gangliaSvc) {
        var svcComponents = gangliaSvc.get('hostComponents');
        if (svcComponents) {
          var gangliaSvcComponent = svcComponents.findProperty("componentName", "GANGLIA_SERVER");
          if (gangliaSvcComponent) {
            var hostName = gangliaSvcComponent.get('host.hostName');
            if (hostName) {
              var host = App.Host.find(hostName);
              if (host) {
                hostName = host.get('publicHostName');
              }
              return "http://" + hostName + "/ganglia";
            }
          }
        }
      }
      return null;
    }
  }.property('App.router.updateController.isUpdated', 'dataLoadList.hosts'),

  /**
   * Provides the URL to use for NAGIOS server. This URL
   * is helpful in getting alerts data from server and also
   * in populating links in UI.
   *
   * If null is returned, it means NAGIOS service is not installed.
   */
  nagiosUrl:function () {
    if (App.testMode) {
      return 'http://nagiosserver/nagios';
    } else {
      // We want live data here
      var svcs = App.Service.find();
      var nagiosSvc = svcs.findProperty("serviceName", "NAGIOS");
      if (nagiosSvc) {
        var svcComponents = nagiosSvc.get('hostComponents');
        if (svcComponents) {
          var nagiosSvcComponent = svcComponents.findProperty("componentName", "NAGIOS_SERVER");
          if (nagiosSvcComponent) {
            var hostName = nagiosSvcComponent.get('host.hostName');
            if (hostName) {
              var host = App.Host.find(hostName);
              if (host) {
                hostName = host.get('publicHostName');
              }
              return "http://" + hostName + "/nagios";
            }
          }
        }
      }
      return null;
    }
  }.property('App.router.updateController.isUpdated', 'dataLoadList.services', 'dataLoadList.hosts'),

  isNagiosInstalled:function () {
    if (App.testMode) {
      return true;
    } else {
      var svcs = App.Service.find();
      var nagiosSvc = svcs.findProperty("serviceName", "NAGIOS");
      return nagiosSvc != null;
    }
  }.property('App.router.updateController.isUpdated', 'dataLoadList.services'),

  /**
   * Sorted list of alerts.
   * Changes whenever alerts are loaded.
   */
  alerts:[],
  updateAlerts: function(){
    var alerts = App.Alert.find();
    var alertsArray = alerts.toArray();
    var sortedArray = alertsArray.sort(function (left, right) {
      var statusDiff = right.get('status') - left.get('status');
      if (statusDiff == 0) { // same error severity - sort by time
        var rightTime = right.get('date');
        var leftTime = left.get('date');
        rightTime = rightTime ? rightTime.getTime() : 0;
        leftTime = leftTime ? leftTime.getTime() : 0;
        statusDiff = rightTime - leftTime;
      }
      return statusDiff;
    });
    this.set('alerts', sortedArray);
  },

  /**
   * Load alerts from server
   * @param callback Slave function, should be called to fire delayed update.
   * Look at <code>App.updater.run</code> for more information.
   * Also used to set <code>dataLoadList.alerts</code> status during app loading
   */
  loadAlerts:function (callback) {
    if (this.get('isNagiosInstalled')) {
      var dataUrl = this.getUrl('/data/alerts/alerts.json', '/host_components?fields=HostRoles/nagios_alerts&HostRoles/component_name=NAGIOS_SERVER');
      var self = this;
      var ajaxOptions = {
        dataType:"json",
        complete:function () {
          self.updateAlerts();
          callback();
        },
        error: function(jqXHR, testStatus, error) {
          console.log('Nagios $.ajax() response:', error);
        }
      };
      App.HttpClient.get(dataUrl, App.alertsMapper, ajaxOptions);
    } else {
      console.log("No Nagios URL provided.");
      callback();
    }
  },

  /**
   * Determination of Nagios presence is known only after App.Service is
   * loaded from server. When that is done, no one tells alerts to load,
   * due to which alerts are not loaded & shown till the next polling cycle.
   * This method immediately loads alerts once Nagios presence is known.
   */
  isNagiosInstalledListener: function () {
    var self = this;
    self.loadAlerts(function () {
      self.updateLoadStatus('alerts');
    });
  }.observes('isNagiosInstalled'),

  /**
   * Send request to server to load components updated statuses
   * @param callback Slave function, should be called to fire delayed update.
   * Look at <code>App.updater.run</code> for more information
   * @return {Boolean} Whether we have errors
   */
  loadUpdatedStatus: function(callback){

    if(!this.get('clusterName')){
      callback();
      return false;
    }
    
    var servicesUrl = this.getUrl('/data/dashboard/services.json', '/services?fields=ServiceInfo,components/host_components/HostRoles/desired_state,components/host_components/HostRoles/state');

    App.HttpClient.get(servicesUrl, App.statusMapper, {
      complete: callback
    });
    return true;
  },

  /**
   * Run <code>loadUpdatedStatus</code> with delay
   * @param delay
   */
  loadUpdatedStatusDelayed: function(delay){
    setTimeout(function(){
      App.updater.immediateRun('loadUpdatedStatus');
    }, delay);
  },

  /**
   * Start polling, when <code>isWorking</code> become true
   */
  startPolling: function(){
    if(!this.get('isWorking')){
      return false;
    }
    App.updater.run(this, 'loadUpdatedStatus', 'isWorking'); //update will not run it immediately
    App.updater.run(this, 'loadAlerts', 'isWorking'); //update will not run it immediately
    return true;
  }.observes('isWorking'),
  /**
   *
   *  load all data and update load status
   */
  loadClusterData:function () {
    var self = this;
    if (!this.get('clusterName')) {
      return;
    }

    if(this.get('isLoaded')) { // do not load data repeatedly
      return;
    }

    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    var hostsUrl = this.getUrl('/data/hosts/hosts.json', '/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/disk_info,Hosts/cpu_count,Hosts/total_mem,Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/load/load_one');
    var usersUrl = App.testMode ? '/data/users/users.json' : App.apiPrefix + '/users/?fields=*';
    var racksUrl = "/data/racks/racks.json";
    var dataSetUrl = "/data/mirroring/all_datasets.json";
    var targetClusterUrl = "/data/mirroring/target_clusters.json";

    App.HttpClient.get(targetClusterUrl, App.targetClusterMapper, {
      complete: function (jqXHR, textStatus) {
        self.updateLoadStatus('targetclusters');
      }
    }, function (jqXHR, textStatus) {
      self.updateLoadStatus('targetclusters');
    });


    App.HttpClient.get(dataSetUrl, App.dataSetMapper, {
      complete: function (jqXHR, textStatus) {
        self.updateLoadStatus('datasets');
      }
    }, function (jqXHR, textStatus) {
      self.updateLoadStatus('datasets');
    });

    App.HttpClient.get(racksUrl, App.racksMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('racks');
      }
    }, function (jqXHR, textStatus) {
      self.updateLoadStatus('racks');
    });

    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('cluster');
      }
    }, function (jqXHR, textStatus) {
        self.updateLoadStatus('cluster');
    });

    App.HttpClient.get(hostsUrl, App.hostsMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('hosts');
      }
    }, function (jqXHR, textStatus) {
        self.updateLoadStatus('hosts');
    });

    App.HttpClient.get(usersUrl, App.usersMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
      }
    }, function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
    });

    App.router.get('updateController').updateServiceMetric(function(){
        self.updateLoadStatus('services');
    }, true);

    this.loadAlerts(function(){
        self.updateLoadStatus('alerts');
    });

  },

  clusterName:function () {
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : null;
  }.property('cluster'),
  
  updateClusterData: function () {
    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete:function(){}
    });
  }
});
