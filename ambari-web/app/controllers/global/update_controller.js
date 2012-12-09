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

  clusterName:function () {
    return (this.get('cluster')) ? this.get('cluster').Clusters.cluster_name : null;
  }.property('cluster'),

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
      },
      statusCode:require('data/statusCodes')
    });
  },

  getUrl:function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + this.get('clusterName') + url;
  },

  updateAll:function(){
    this.updateHost();
    this.updateServiceMetric();
    this.graphsUpdate();
  },

  updateHost:function(){

      var hostsUrl = this.getUrl('/data/hosts/hosts.json', '/hosts?fields=*');
      App.HttpClient.get(hostsUrl, App.hostsMapper, {
        complete:function (jqXHR, textStatus) {

        }
      });

  },
  graphs: [],
  graphsUpdate: function () {
      var existedGraphs = [];
      this.get('graphs').forEach(function (_graph) {
        var view = Em.View.views[_graph.id];
        if (view) {
          existedGraphs.push(_graph);
          console.log('updated graph', _graph.name);
          view.loadData();
          //if graph opened as modal popup update it to
          if($(".modal-graph-line .modal-body #" + _graph.popupId + "-container-popup").length) {
            view.loadData();
          }
        }
      });
      this.set('graphs', existedGraphs);
  },
  updateServiceMetric:function(callback){
    var self = this;
    self.set('isUpdated', false);
    var servicesUrl = this.getUrl('/data/dashboard/services.json', '/services?ServiceInfo/service_name!=MISCELLANEOUS&ServiceInfo/service_name!=DASHBOARD&fields=*,components/host_components/*,components/ServiceComponentInfo');
    var callback = callback || function(jqXHR, textStatus){
      self.set('isUpdated', true);
    };

    App.HttpClient.get(servicesUrl, App.servicesMapper, {
      complete: callback
    });
  }


});
