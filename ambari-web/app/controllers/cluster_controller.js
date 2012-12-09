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
  cluster: null,
  isLoaded: function(){
    return true;
    var loadList = this.get('dataLoadList');
    var loaded = true;
    for(var i in loadList){
      if(loadList.hasOwnProperty(i) && !loadList[i]){
        loaded = false;
      }
    }
    return loaded;
  }.property('dataLoadList'),
  dataLoadList: Em.Object.create({
    'hosts': false,
    'jobs': false,
    'runs': false,
    'services': false,
    'components': false,
    'cluster' : false
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
        console.log('failed on loading cluster name')
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
    var clusterUrl = (App.testMode) ? '/data/clusters/cluster.json': '/api/clusters/mycluster?fields=Clusters';
    var jobsUrl = (App.testMode) ? "/data/apps/jobs.json" : "/api/jobs?fields=*";
    var runsUrl = (App.testMode) ? "/data/apps/runs.json" : "/api/runs?fields=*";
    var hostsUrl = (App.testMode) ? "/data/hosts/hosts.json" : "/api/hosts?fields=*";
    var servicesUrl = (App.testMode) ?
        "/data/dashboard/services.json" :
        "/api/clusters/mycluster/services?ServiceInfo/service_name!=MISCELLANEOUS&ServiceInfo/service_name!=DASHBOARD&fields=components/host_components/*";
    App.HttpClient.get(clusterUrl, App.clusterMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.cluster', true);
      }
    });
     App.HttpClient.get(jobsUrl, App.jobsMapper,{
       complete:function(jqXHR, textStatus) {
         self.set('dataLoadList.jobs', true);
       }
     });
     App.HttpClient.get(runsUrl, App.runsMapper,{
       complete:function(jqXHR, textStatus) {
         self.set('dataLoadList.runs', true);
       }
     });
    App.HttpClient.get(hostsUrl, App.hostsMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.hosts', true);
      }
    });
    App.HttpClient.get(servicesUrl, App.servicesMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.services', true);
      }
    });
    /*App.HttpClient.get(servicesUrl, App.componentsMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.components', true);
      }
    });*/
  }.observes('clusterName'),
  clusterName: function(){
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : 'mycluster';
  }.property('cluster')
})
