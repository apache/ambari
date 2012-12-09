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
  name: 'clusterController',
  cluster:null,
  isLoaded: false,
  updateLoadStatus: function(item){
    var loadList = this.get('dataLoadList');
    var loaded = true;
    loadList.set(item, true);
    for(var i in loadList){
      if(loadList.hasOwnProperty(i) && !loadList[i] && loaded){
        loaded = false;
      }
    }
    this.set('isLoaded', loaded);
  },
  dataLoadList: Em.Object.create({
    'hosts': false,
    'jobs': false,
    'runs': false,
    'services': false,
    'cluster' : false,
    'racks' : false,
    'alerts' : false
  }),
  /**
   * load cluster name
   */
  loadClusterName: function(){
    var self = this;
    var url = (App.testMode) ? '/data/clusters/info.json' : '/api/clusters';
    $.ajax({
      type: "GET",
      url: url,
      dataType: 'json',
      timeout: 5000,
      success: function (data) {
        self.set('cluster', data.items[0]);
      },
      error: function (request, ajaxOptions, error) {
        //do something
        console.log('failed on loading cluster name');
        //hack skip loading when data ain't received
        if(!App.testMode) self.set('isLoaded', true);
      },
      statusCode: require('data/statusCodes')
    });
  },

  getUrl: function(testUrl, url){
    return (App.testMode) ? testUrl: '/api/clusters/' + this.get('clusterName') + url;
  },
   /**
   *
   *  load all data and update load status
   */
  loadClusterData: function(){
    var self = this;
    if(!this.get('clusterName')){
        return;
    }

     var alertsUrl = "/data/alerts/alerts.json";
     var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
     var hostsUrl = this.getUrl('/data/hosts/hosts.json', '/hosts?fields=*');
     var servicesUrl = this.getUrl('/data/dashboard/services.json', '/services?ServiceInfo/service_name!=MISCELLANEOUS&ServiceInfo/service_name!=DASHBOARD&fields=components/host_components/*');

     var jobsUrl = "/data/apps/jobs.json";
     var runsUrl = "/data/apps/runs.json";

     var racksUrl = "/data/racks/racks.json";

    App.HttpClient.get(alertsUrl, App.alertsMapper,{
      complete:function(jqXHR, textStatus){
        self.updateLoadStatus('alerts');
      }
    });
    App.HttpClient.get(racksUrl, App.racksMapper,{
      complete:function(jqXHR, textStatus){
        self.updateLoadStatus('racks');
      }
    });
    App.HttpClient.get(clusterUrl, App.clusterMapper,{
      complete:function(jqXHR, textStatus){
        self.updateLoadStatus('cluster');
      }
    });
     App.HttpClient.get(jobsUrl, App.jobsMapper,{
       complete:function(jqXHR, textStatus) {
         self.updateLoadStatus('jobs');
         App.HttpClient.get(runsUrl, App.runsMapper,{
           complete:function(jqXHR, textStatus) {
             self.updateLoadStatus('runs');
           }
         });
       }
     });
    App.HttpClient.get(hostsUrl, App.hostsMapper,{
      complete:function(jqXHR, textStatus){
        self.updateLoadStatus('hosts');
      }
    });
    App.HttpClient.get(servicesUrl, App.servicesMapper,{
      complete:function(jqXHR, textStatus){
        self.updateLoadStatus('services');
      }
    });
  }.observes('clusterName'),
  clusterName: function(){
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : 'mycluster';
  }.property('cluster')
})
