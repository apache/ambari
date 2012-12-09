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
    'hosts': true,
    'services': false
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
        self.loadClusterData();
      },
      error: function (request, ajaxOptions, error) {
        //do something
        console.log('failed on loading cluster name')
      },
      statusCode: require('data/statusCodes')
    });
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
    // TODO: load all models
    /*App.HttpClient.get("/data/hosts/hosts.json", App.hostsMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.hosts', true);
      }
    });*/
    App.HttpClient.get("/data/dashboard/services.json", App.servicesMapper,{
      complete:function(jqXHR, textStatus){
        self.set('dataLoadList.services', true);
      }
    });
  }.observes('clusterName'),
  clusterName: function(){
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : 'mycluster';
  }.property('cluster')
})
