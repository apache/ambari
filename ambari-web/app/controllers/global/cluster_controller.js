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
  ambariProperties: null,
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
    'clusterStatus':false,
    'racks':false,
    'alerts':false,
    'users':false,
    'datasets':false,
    'targetclusters':false,
    'status': false,
    'componentConfigs': !App.supports.hostOverrides
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
              return this.get('gangliaWebProtocol') + "://" + (App.singleNodeInstall ? App.singleNodeAlias + ":42080" : hostName) + "/ganglia";
            }
          }
        }
      }
      return null;
    }
  }.property('App.router.updateController.isUpdated', 'dataLoadList.hosts','gangliaWebProtocol'),

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
              return this.get('nagiosWebProtocol') + "://" + (App.singleNodeInstall ? App.singleNodeAlias + ":42080" : hostName) + "/nagios";
            }
          }
        }
      }
      return null;
    }
  }.property('App.router.updateController.isUpdated', 'dataLoadList.services', 'dataLoadList.hosts','nagiosWebProtocol'),

  nagiosWebProtocol: function () {
    var properties = this.get('ambariProperties');
    if (properties && properties.hasOwnProperty('nagios.https') && properties['nagios.https']) {
      return "https";
    } else {
      return "http";
    }
  }.property('ambariProperties'),

  gangliaWebProtocol: function () {
    var properties = this.get('ambariProperties');
    if (properties && properties.hasOwnProperty('ganglia.https') && properties['ganglia.https']) {
      return "https";
    } else {
      return "http";
    }
  }.property('ambariProperties'),

  isNagiosInstalled:function () {
    return !!App.Service.find().findProperty('serviceName', 'NAGIOS');
  }.property('App.router.updateController.isUpdated', 'dataLoadList.services'),

  isGangliaInstalled:function () {
    return !!App.Service.find().findProperty('serviceName', 'GANGLIA');
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
      var testUrl = App.get('isHadoop2Stack') ? '/data/alerts/HDP2/alerts.json':'/data/alerts/alerts.json';
      var dataUrl = this.getUrl(testUrl, '/host_components?fields=HostRoles/nagios_alerts&HostRoles/component_name=NAGIOS_SERVER');
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
    var testUrl = App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hc_host_status.json':'/data/dashboard/services.json';
    var statusUrl = '/hosts?fields=Hosts/host_status,host_components/HostRoles/state';
    //desired_state property is eliminated since calculateState function is commented out, it become useless
    statusUrl = this.getUrl(testUrl, statusUrl);

    App.HttpClient.get(statusUrl, App.statusMapper, {
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
    App.updater.run(this, 'loadUpdatedStatus', 'isWorking', App.componentsUpdateInterval); //update will not run it immediately
    App.updater.run(this, 'loadAlerts', 'isWorking'); //update will not run it immediately
    return true;
  }.observes('isWorking'),
  /**
   *
   *  load all data and update load status
   */
  loadClusterData:function () {
    var self = this;
    this.loadAmbariProperties();
    if (!this.get('clusterName')) {
      return;
    }

    if(this.get('isLoaded')) { // do not load data repeatedly
      return;
    }
    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    var hostsRealUrl = '/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/cpu_count,Hosts/total_mem,' +
      'Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components,Hosts/disk_info,' +
      'metrics/disk,metrics/load/load_one,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free';
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

    if (App.testMode) {
      self.updateLoadStatus('clusterStatus');
    } else {
      App.clusterStatus.updateFromServer(true).complete(function() {
        self.updateLoadStatus('clusterStatus');
      });
    }
    
    this.requestHosts(hostsRealUrl, function (jqXHR, textStatus) {
      self.updateLoadStatus('hosts');
    });

    App.HttpClient.get(usersUrl, App.usersMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
      }
    }, function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
    });

    self.loadUpdatedStatus(function () {
      self.updateLoadStatus('status');
      App.router.get('updateController').updateServiceMetric(function () {
        if (App.supports.hostOverrides) {
          App.router.get('updateController').updateComponentConfig(function () {
            self.updateLoadStatus('componentConfigs');
          });
        }
        self.updateLoadStatus('services');
      }, true);
    });

    this.loadAlerts(function(){
        self.updateLoadStatus('alerts');
    });
  },

  requestHosts: function(realUrl, callback){
    var testHostUrl =  App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hosts.json':'/data/hosts/hosts.json';
    var url = this.getUrl(testHostUrl, realUrl);
    App.HttpClient.get(url, App.hostsMapper, {
      complete: callback
    }, callback)
  },

  loadAmbariProperties: function() {
    App.ajax.send({
      name: 'ambari.service',
      sender: this,
      success: 'loadAmbariPropertiesSuccess',
      error: 'loadAmbariPropertiesError'
    });
    return this.get('ambariProperties');
  },

  loadAmbariPropertiesSuccess: function(data) {
    console.log('loading ambari properties');
    this.set('ambariProperties', data.RootServiceComponents.properties);
  },

  loadAmbariPropertiesError: function() {
    console.warn('can\'t get ambari properties');
  },

  clusterName:function () {
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : null;
  }.property('cluster'),
  
  updateClusterData: function () {
    var testUrl = App.get('isHadoop2Stack') ? '/data/clusters/HDP2/cluster.json':'/data/clusters/cluster.json';
    var clusterUrl = this.getUrl(testUrl, '?fields=Clusters');
    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete:function(){}
    });
  }
});
