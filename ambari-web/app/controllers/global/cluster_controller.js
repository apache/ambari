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
  /**
   * Whether we need to update statuses automatically or not
   */
  isWorking: false,
  updateLoadStatus:function (item) {
    var loadList = this.get('dataLoadList');
    var loaded = true;
    loadList.set(item, true);
    for (var i in loadList) {
      if (loadList.hasOwnProperty(i) && !loadList[i] && loaded) {
        loaded = false;
      }
    }
    this.set('isLoaded', loaded);
  },

  dataLoadList:Em.Object.create({
    'hosts':false,
    'services':false,
    'cluster':false,
    'racks':false,
    'alerts':false,
    'users':false
  }),

  postLoadList:{
    'runs':false
  },
  /**
   * load cluster name
   */
  loadClusterName:function (reload) {
    if (this.get('clusterName') && !reload) {
      return;
    }
    var self = this;
    var url = (App.testMode) ? '/data/clusters/info.json' : App.apiPrefix + '/clusters';
    $.ajax({
      async:false,
      type:"GET",
      url:url,
      dataType:'json',
      timeout:App.timeout,
      success:function (data) {
        self.set('cluster', data.items[0]);
      },
      error:function (request, ajaxOptions, error) {
        console.log('failed on loading cluster name');
        self.set('isLoaded', true);
      },
      statusCode:require('data/statusCodes')
    });
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
        var svcComponents = gangliaSvc.get('components');
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
        var svcComponents = nagiosSvc.get('components');
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
  }.property('App.router.updateController.isUpdated','dataLoadList.services'),

  isNagiosInstalled:function () {
    if (App.testMode) {
      return true;
    } else {
      var svcs = App.Service.find();
      var nagiosSvc = svcs.findProperty("serviceName", "NAGIOS");
      return nagiosSvc != null;
    }
  }.property('App.router.updateController.isUpdated'),

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
  loadRuns:function () {
    if (this.get('postLoadList.runs')) {
      return;
    }

    var self = this;
    var runsUrl = App.testMode ? "/data/apps/runs.json" : App.apiPrefix + "/jobhistory/workflow";

    App.HttpClient.get(runsUrl, App.runsMapper, {
      complete:function (jqXHR, textStatus) {
        self.set('postLoadList.runs', true);
      }
    }, function () {
      self.set('postLoadList.runs', true);
    });
  },

  /**
   * This method automatically loads alerts when Nagios URL
   * changes. Once done it will trigger dataLoadList.alerts
   * property, which will trigger the alerts property.
   */
  loadAlerts:function () {
    if(App.router.get('updateController.isUpdated')){
      return;
    }
    var nagiosUrl = this.get('nagiosUrl');
    if (nagiosUrl) {
      var lastSlash = nagiosUrl.lastIndexOf('/');
      if (lastSlash > -1) {
        nagiosUrl = nagiosUrl.substring(0, lastSlash);
      }
      var dataUrl;
      var ajaxOptions = {
        dataType:"jsonp",
        jsonp:"jsonp",
        context:this,
        complete:function (jqXHR, textStatus) {
          this.updateLoadStatus('alerts');
          this.updateAlerts();
        },
        error: function(jqXHR, testStatus, error) {
          // this.showMessage(Em.I18n.t('nagios.alerts.unavailable'));
          console.log('Nagios $.ajax() response:', error);
        }
      };
      if (App.testMode) {
        dataUrl = "/data/alerts/alerts.jsonp";
        ajaxOptions.jsonpCallback = "jQuery172040994187095202506_1352498338217";
      } else {
        dataUrl = nagiosUrl + "/hdp/nagios/nagios_alerts.php?q1=alerts&alert_type=all";
      }
      App.HttpClient.get(dataUrl, App.alertsMapper, ajaxOptions);
    } else {
      this.updateLoadStatus('alerts');
      console.log("No Nagios URL provided.")
    }
  }.observes('nagiosUrl'),

  /**
   * Show message in UI
   */
  showMessage: function(message){
    App.ModalPopup.show({
      header: 'Message',
      body: message,
      onPrimary: function() {
        this.hide();
      },
      secondary : null
    });
  },

  statusTimeoutId: null,

  loadUpdatedStatusDelayed: function(delay){
    delay = delay || App.componentsUpdateInterval;
    var self = this;

    this.set('statusTimeoutId',
      setTimeout(function(){
        self.loadUpdatedStatus();
      }, delay)
    );
  },

  loadUpdatedStatus: function(){

    var timeoutId = this.get('statusTimeoutId');
    if(timeoutId){
      clearTimeout(timeoutId);
      this.set('statusTimeoutId', null);
    }

    if(!this.get('isWorking')){
      return false;
    }

    if(!this.get('clusterName')){
      this.loadUpdatedStatusDelayed(this.get('componentsUpdateInterval')/2, 'error:clusterName');
      return;
    }
    
    var servicesUrl = this.getUrl('/data/dashboard/services.json', '/services?fields=components/ServiceComponentInfo,components/host_components,components/host_components/HostRoles');

    var self = this;
    App.HttpClient.get(servicesUrl, App.statusMapper, {
      complete:function (jqXHR, textStatus) {
        //console.log('Cluster Controller: Updated components statuses successfully!!!')
        self.loadUpdatedStatusDelayed();
      }
    }, function(){
      self.loadUpdatedStatusDelayed(null, 'error:response error');
    });

  },
  startLoadUpdatedStatus: function(){
    var self = this;
    this.set('isWorking', true);
    setTimeout(function(){
      self.loadUpdatedStatus();
    }, App.componentsUpdateInterval*2);
  },
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
    var hostsUrl = this.getUrl('/data/hosts/hosts.json', '/hosts?fields=Hosts,host_components');
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

  },

  clusterName:function () {
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : null;
  }.property('cluster')
})
