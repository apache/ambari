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
  ambariVersion: null,
  ambariViews: [],
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
    'serviceMetrics':false,
    'services': false,
    'cluster':false,
    'clusterStatus':false,
    'racks':false,
    'users':false,
    'componentConfigs': false
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

  /**
   * load current server clock in milli-seconds
   */
  loadClientServerClockDistance: function () {
    var dfd = $.Deferred();
    this.getServerClock().done(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },

  getServerClock: function(){
    return App.ajax.send({
      name: 'ambari.service.load_server_clock',
      sender: this,
      success: 'getServerClockSuccessCallback',
      error: 'getServerClockErrorCallback'
    });
  },
  getServerClockSuccessCallback: function (data) {
    var clientClock = new Date().getTime();
    var serverClock = (data.RootServiceComponents.server_clock).toString();
    serverClock = serverClock.length < 13? serverClock+ '000': serverClock;
    App.set('clockDistance', serverClock - clientClock);
    App.set('currentServerTime', parseInt(serverClock));
    console.log('loading ambari server clock distance');
  },
  getServerClockErrorCallback: function () {
    console.log('Cannot load ambari server clock');
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
  }.property('App.router.updateController.isUpdated', 'dataLoadList.serviceMetrics', 'dataLoadList.hosts','nagiosWebProtocol'),

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
  }.property('App.router.updateController.isUpdated', 'dataLoadList.serviceMetrics'),

  isGangliaInstalled:function () {
    return !!App.Service.find().findProperty('serviceName', 'GANGLIA');
  }.property('App.router.updateController.isUpdated', 'dataLoadList.serviceMetrics'),

  /**
   * Send request to server to load components updated statuses
   * @param callback Slave function, should be called to fire delayed update.
   * @param isInitialLoad
   * Look at <code>App.updater.run</code> for more information
   * @return {Boolean} Whether we have errors
   */
  loadUpdatedStatus: function (callback, isInitialLoad) {
    if (!this.get('clusterName')) {
      callback();
      return false;
    }
    App.set('currentServerTime', App.get('currentServerTime') + App.componentsUpdateInterval);
    var testUrl = App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hc_host_status.json' : '/data/dashboard/services.json';
    var statusUrl = '/hosts?fields=Hosts/host_status,Hosts/maintenance_state,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,alerts/summary&minimal_response=true';
    if (isInitialLoad) {
      testUrl = '/data/hosts/HDP2/hosts_init.json';
      statusUrl = '/hosts?fields=Hosts/host_name,Hosts/maintenance_state,Hosts/public_host_name,Hosts/cpu_count,Hosts/ph_cpu_count,Hosts/total_mem,' +
        'Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,' +
        'Hosts/disk_info,metrics/disk,metrics/load/load_one,metrics/cpu/cpu_system,metrics/cpu/cpu_user,' +
        'metrics/memory/mem_total,metrics/memory/mem_free,alerts/summary&minimal_response=true';
    }
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
    return true;
  }.observes('isWorking'),
  /**
   *
   *  load all data and update load status
   */
  loadClusterData:function () {
    var self = this;
    this.loadAmbariProperties();
    this.loadAmbariViews();
    if (!this.get('clusterName')) {
      return;
    }

    if(this.get('isLoaded')) { // do not load data repeatedly
      App.router.get('mainController').startPolling();
      return;
    }
    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    var usersUrl = App.testMode ? '/data/users/users.json' : App.apiPrefix + '/users/?fields=*';
    var racksUrl = "/data/racks/racks.json";

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
    
    App.HttpClient.get(usersUrl, App.usersMapper, {
      complete:function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
      }
    }, function (jqXHR, textStatus) {
        self.updateLoadStatus('users');
    });

    /**
     * Order of loading:
     * 1. request for services
     * 2. put services in cache
     * 3. request for hosts and host-components (single call)
     * 4. request for service metrics
     * 5. load host-components to model
     * 6. load hosts to model
     * 7. load services from cache with metrics to model
     * 8. update stale_configs of host-components (depends on App.supports.hostOverrides)
     */
    App.router.get('updateController').updateServices(function () {
      self.updateLoadStatus('services');
      self.loadUpdatedStatus(function () {
        self.updateLoadStatus('hosts');
        if (App.supports.hostOverrides) {
          App.router.get('updateController').updateComponentConfig(function () {
            self.updateLoadStatus('componentConfigs');
          });
        } else {
          self.updateLoadStatus('componentConfigs');
        }
      }, true);
      App.router.get('updateController').updateServiceMetric(function () {}, true);
    });
  },
  /**
   * json from serviceMetricsMapper on initial load
   */
  serviceMetricsJson: null,
  /**
   * control that services was loaded to model strictly after hosts and host-components
   * regardless which request was completed first
   * @param json
   */
  deferServiceMetricsLoad: function (json) {
    if (json) {
      if (this.get('dataLoadList.hosts')) {
        App.serviceMetricsMapper.map(json, true);
        this.updateLoadStatus('serviceMetrics');
      } else {
        this.set('serviceMetricsJson', json);
      }
    } else if (this.get('serviceMetricsJson')) {
      json = this.get('serviceMetricsJson');
      this.set('serviceMetricsJson', null);
      App.serviceMetricsMapper.map(json, true);
      this.updateLoadStatus('serviceMetrics');
    }
  },

  requestHosts: function(realUrl, callback){
    var testHostUrl =  App.get('isHadoop2Stack') ? '/data/hosts/HDP2/hosts.json':'/data/hosts/hosts.json';
    var url = this.getUrl(testHostUrl, realUrl);
    App.HttpClient.get(url, App.hostsMapper, {
      complete: callback
    }, callback)
  },

  loadAmbariViews: function() {
    App.ajax.send({
      name: 'views.info',
      sender: this,
      success: 'loadAmbariViewsSuccess'
    });
  },

  loadAmbariViewsSuccess: function(data) {
    this.set('ambariViews',[]);
    data.items.forEach(function(item){
      App.ajax.send({
        name: 'views.instances',
        data: {
          viewName: item.ViewInfo.view_name
        },
        sender: this,
        success: 'loadViewInstancesSuccess'
      });
    }, this)
  },

  loadViewInstancesSuccess: function(data) {
    data.instances.forEach(function(instance){
      var view = Em.Object.create({
        label: data.ViewInfo.label,
        viewName: instance.ViewInstanceInfo.view_name,
        instanceName: instance.ViewInstanceInfo.instance_name,
        href: "/views/" + instance.ViewInstanceInfo.view_name + "/" + instance.ViewInstanceInfo.instance_name
      });
      this.get('ambariViews').push(view);
    }, this);
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
    this.set('ambariVersion', data.RootServiceComponents.component_version);
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
