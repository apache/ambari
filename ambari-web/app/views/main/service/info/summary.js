/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.AlertItemView = Em.View.extend({
  tagName:"li",
  templateName: require('templates/main/service/info/summary_alert'),
  classNameBindings: ["status"],
  status: function () {
    return "status-" + this.get("content.status");
  }.property('content'),
  didInsertElement: function () {
    // Tooltips for alerts need to be enabled.
    $("div[rel=tooltip]").tooltip();
    $(".tooltip").remove();
  }
})

App.MainServiceInfoSummaryView = Em.View.extend({
  templateName: require('templates/main/service/info/summary'),
  attributes:null,
  serviceStatus:{
    hdfs:false,
    mapreduce:false,
    hbase:false,
    zookeeper:false,
    oozie:false,
    hive:false,
    ganglia:false,
    nagios:false
  },

  clients: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") == "OOZIE" || service.get("id") == "ZOOKEEPER") {
      var clients = service.get('hostComponents').filterProperty('isClient');
      if (clients.length > 0) {
        result = [{
          'displayName': clients[0].get('displayName'),
          'isComma': false,
          'isAnd': false
        }];
      }
      if (clients.length > 1) {
        result[0].isComma = true;
        result.push({
          'displayName': clients[1].get('displayName'),
          'isComma': false,
          'isAnd': false
        });
      }
      if (clients.length > 2) {
        result[1].isAnd = true;
        result.push({
          'displayName': clients.length - 2 + ' more',
          'isComma': false,
          'isAnd': false
        });
      }
    }
    return result;
  }.property('controller.content'),

  hasManyServers: function () {
    if (this.get('servers').length > 1) {
      return true;
    }
    return false;
  }.property('servers'),

  hasManyClients: function () {
    if (this.get('clients').length > 1) {
      return true;
    }
    return false;
  }.property('clients'),

  servers: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        result = [{
          'host': servers[0].get('displayName'),
          'isComma': false,
          'isAnd': false
        }];
      }
      if (servers.length > 1) {
        result[0].isComma = true;
        result.push({
          'host': servers[1].get('displayName'),
          'isComma': false,
          'isAnd': false
        });
      }
      if (servers.length > 2) {
        result[1].isAnd = true;
        result.push({
          'host': servers.length - 2 + ' more',
          'isComma': false,
          'isAnd': false
        });
      }
    }
    return result;
  }.property('controller.content'),

  monitors: function () {
    var result = '';
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('hostComponents').filterProperty('isMaster', false);
      if (monitors.length) {
        result = monitors.length - 1 ? monitors.length + ' hosts running monitor' : '1 host running monitor';
      }
    }
    return result;
  }.property('controller.content'),

  /**
   * Property related to GANGLIA service, is unused for other services
   * @return {Object}
   */
  monitorsObj: function(){
    var service = this.get('controller.content');
    if (service.get("id") == "GANGLIA") {
      var monitors = service.get('components').filterProperty('isMaster', false);
      if (monitors.length) {
        return monitors[0];
      }
    }
    return {};
  }.property('controller.content'),

  /**
   * Property related to ZOOKEEPER service, is unused for other services
   * @return {Object}
   */
  serversHost: function() {
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER") {
      var servers = service.get('components').filterProperty('isMaster');
      if (servers.length > 0) {
        return servers[0];
      }
    }
    return {};
  }.property('controller.content'),

  /**
   * Property related to OOZIE and ZOOKEEPER services, is unused for other services
   * @return {Object}
   */
  clientObj: function() {
    var service = this.get('controller.content');
    if (service.get("id") == "OOZIE" || service.get("id") == "ZOOKEEPER") {
      var clients = service.get('components').filterProperty('isMaster', false);
      if (clients.length > 0) {
        return clients[0];
      }
    }
    return {};
  }.property('controller.content'),

  data:{
    hive:{
      "database":"PostgreSQL",
      "databaseName":"hive",
      "user":"hive"
    }
  },
  gangliaServer:function(){
    var service=this.get('controller.content');
    if(service.get("id") == "GANGLIA"){
      return service.get("components").findProperty('isMaster', true).get("host").get("publicHostName");
    }else{
      return "";
    }
  }.property('controller.content'),
  nagiosServer:function(){
    var service=this.get('controller.content');
    if(service.get("id") == "NAGIOS"){
      return service.get("components").findProperty('isMaster', true).get("host").get("publicHostName");
    }else{
      return "";
    }
  }.property('controller.content'),
  oozieServer:function(){
    var service=this.get('controller.content');
    if(service.get("id") == "OOZIE"){
      return service.get("components").findProperty('isMaster', true).get("host").get("publicHostName");
    }else{
      return "";
    }
  }.property('controller.content'),
  /**
   * Returns hive components information in 
   * the following format:
   * {
   *  label: "Component Name",
   *  host: Host,
   *  
   */
  hiveComponentsInfo: function(){
    var componentInfos = [];
    var service=this.get('controller.content');
    if(service.get("id") == "HIVE"){
      var self = this;
      var components = service.get("components");
      if(components){
        components.forEach(function(component){
          var ci = {
              label: component.get('displayName'),
              host: component.get('host')
          };
          if(component.get('id')=='MYSQL_SERVER'){
            ci.label = self.t('services.hive.databaseComponent');
          }
          componentInfos.push(ci);
        });
      }
    }
    return componentInfos;
  }.property('controller.content'),
  service:function () {
    var svc = this.get('controller.content');
    var svcName = svc.get('serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'mapreduce':
          svc = App.MapReduceService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName'),

  isHide:true,
  moreStatsView:Em.View.extend({
    tagName:"a",
    template:Ember.Handlebars.compile('{{t services.service.summary.moreStats}}'),
    attributeBindings:[ 'href' ],
    classNames:[ 'more-stats' ],
    click:function (event) {
      this._parentView._parentView.set('isHide', false);
      this.remove();
    },
    href:'javascript:void(null)'
  }),

  serviceName:function () {
    return this.get('service.serviceName');
  }.property('service'),

  oldServiceName:'',

  /**
   * Contains graphs for this particular service
   */
  serviceMetricGraphs:function () {
    var svcName = this.get('service.serviceName');
    var graphs = [];
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          graphs = [ App.ChartServiceMetricsHDFS_SpaceUtilization.extend(),
            App.ChartServiceMetricsHDFS_FileOperations.extend(),
            App.ChartServiceMetricsHDFS_BlockStatus.extend(),
            App.ChartServiceMetricsHDFS_IO.extend(),
            App.ChartServiceMetricsHDFS_RPC.extend(),
            App.ChartServiceMetricsHDFS_GC.extend(),
            App.ChartServiceMetricsHDFS_JVMHeap.extend(),
            App.ChartServiceMetricsHDFS_JVMThreads.extend()];
          break;
        case 'mapreduce':
          graphs = [ App.ChartServiceMetricsMapReduce_JobsStatus.extend(),
            App.ChartServiceMetricsMapReduce_TasksRunningWaiting.extend(),
            App.ChartServiceMetricsMapReduce_MapSlots.extend(),
            App.ChartServiceMetricsMapReduce_ReduceSlots.extend(),
            App.ChartServiceMetricsMapReduce_GC.extend(),
            App.ChartServiceMetricsMapReduce_RPC.extend(),
            App.ChartServiceMetricsMapReduce_JVMHeap.extend(),
            App.ChartServiceMetricsMapReduce_JVMThreads.extend()];
          break;
        case 'hbase':
          graphs = [  App.ChartServiceMetricsHBASE_ClusterRequests.extend(),
            App.ChartServiceMetricsHBASE_RegionServerReadWriteRequests.extend(),
            App.ChartServiceMetricsHBASE_RegionServerRegions.extend(),
            App.ChartServiceMetricsHBASE_RegionServerQueueSize.extend(),
            App.ChartServiceMetricsHBASE_HlogSplitTime.extend(),
            App.ChartServiceMetricsHBASE_HlogSplitSize.extend()];
          break;
        default:
          break;
      }
    }
    return graphs;
  }.property('service'),

  loadServiceSummary:function (serviceName) {

    var serviceName = this.get('serviceName');
    if (!serviceName) {
      return;
    }

    if (this.get('oldServiceName')) {
      // do not delete it!
      return;
    }

    var summaryView = this;
    var serviceStatus = summaryView.get('serviceStatus');
    $.each(serviceStatus, function (key, value) {
      if (key.toUpperCase() == serviceName) {
        summaryView.set('serviceStatus.' + key, true);
      } else {
        summaryView.set('serviceStatus.' + key, false);
      }
    });

    console.log('load ', serviceName, ' info');
    this.set('oldServiceName', serviceName);
    serviceName = serviceName.toLowerCase();
  }.observes('serviceName'),

  gangliaUrl:function () {
    var gangliaUrl = App.router.get('clusterController.gangliaUrl');
    var svcName = this.get('service.serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPNameNode&tab=m&vn=";
          break;
        case 'mapreduce':
          gangliaUrl += "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPJobTracker&tab=m&vn=";
          break;
        case 'hbase':
          gangliaUrl += "?r=hour&cs=&ce=&m=&s=by+name&c=HDPHBaseMaster&tab=m&vn=";
          break;
        default:
          break;
      }
    }
    return gangliaUrl;
  }.property('App.router.clusterController.gangliaUrl', 'service.serviceName'),

  didInsertElement:function () {
    // We have to make the height of the Alerts section
    // match the height of the Summary section.
    var summaryTable = document.getElementById('summary-info');
    var alertsList = document.getElementById('summary-alerts-list');
    if (summaryTable && alertsList) {
      var rows = $(summaryTable).find('tr');
      if (rows != null && rows.length > 0) {
        $(alertsList).attr('style', "height:" + summaryTable.clientHeight + "px;");
      } else if (alertsList.clientHeight > 0) {
        $(summaryTable).append('<tr><td></td></tr>');
        $(summaryTable).attr('style', "height:" + alertsList.clientHeight + "px;");
      }
    }
  },

  clientHosts:App.Host.find(),

  clientHostsLength:function () {
    var text = this.t('services.service.summary.clientCount');
    var self = this;
    return text.format(self.get('clientHosts.length'));
  }.property('clientHosts'),

  clientComponents:function () {
    return App.HostComponent.find().filterProperty('isClient', true);
  }.property(),

  clientComponentsString:function () {
    var components = this.get('clientComponents');
    var names = [];
    components.forEach(function (component) {
      if (names.indexOf(component.get('displayName')) == -1) {
        names.push(component.get('displayName'));
      }
    });

    return names.length ? names.join(', ') : false;
  }.property('clientComponents'),
  hasAlertsBox: function(){
    var services = [
      'NAGIOS'
    ];
    return -1 === services.indexOf(this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName')
});