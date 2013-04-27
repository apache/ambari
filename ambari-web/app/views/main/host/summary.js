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

App.MainHostSummaryView = Em.View.extend({
  templateName: require('templates/main/host/summary'),

  content: function () {
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  showGangliaCharts: function () {
    var name = this.get('content.hostName');
    var gangliaMobileUrl = App.router.get('clusterController.gangliaUrl') + "/mobile_helper.php?show_host_metrics=1&h=" + name + "&c=HDPNameNode&r=hour&cs=&ce=";
    window.open(gangliaMobileUrl);
  },

  /**
   * @type: [{String}]
   */
  decommissionDataNodeHostNames: null,

  loadDecommissionNodesList: function () {
    var self = this;
    var clusterName = App.router.get('clusterController.clusterName');
    var persistUrl = App.apiPrefix + '/persist';
    var clusterUrl = App.apiPrefix + '/clusters/' + clusterName;
    var getConfigAjax = {
      type: 'GET',
      url: persistUrl,
      dataType: 'json',
      timeout: App.timeout,
      success: function (data) {
        if (data && data.decommissionDataNodesTag) {
          // We know the tag which contains the decommisioned nodes.
          var configsUrl = clusterUrl + '/configurations?type=hdfs-exclude-file&tag=' + data.decommissionDataNodesTag;
          var decomNodesAjax = {
            type: 'GET',
            url: configsUrl,
            dataType: 'json',
            timeout: App.timeout,
            success: function (data) {
              if (data && data.items) {
                var csv = data.items[0].properties.datanodes;
                self.set('decommissionDataNodeHostNames', csv.split(','));
              }
            },
            error: function (xhr, textStatus, errorThrown) {
              console.log(textStatus);
              console.log(errorThrown);
            }
          };
          jQuery.ajax(decomNodesAjax);
        }
      },
      error: function (xhr, textStatus, errorThrown) {
        // No tag pointer in persist. Rely on service's decomNodes.
        var hdfsSvcs = App.HDFSService.find();
        if (hdfsSvcs && hdfsSvcs.get('length') > 0) {
          var hdfsSvc = hdfsSvcs.objectAt(0);
          if (hdfsSvc) {
            var hostNames = [];
            var decomNodes = hdfsSvc.get('decommissionDataNodes');
            decomNodes.forEach(function (decomNode) {
              hostNames.push(decomNode.get('hostName'));
            });
            self.set('decommissionDataNodeHostNames', hostNames);
          }
        }
      }
    }
    jQuery.ajax(getConfigAjax);
  },
  didInsertElement: function () {
    this.loadDecommissionNodesList();
  },
  sortedComponents: function() {
    var slaveComponents = [];
    var masterComponents = [];
    this.get('content.hostComponents').forEach(function(component){
      if(component.get('workStatus') != 'INSTALLING'){
        if(component.get('isMaster')){
          masterComponents.push(component);
        } else if(component.get('isSlave')) {
          slaveComponents.push(component);
        }
      }

    }, this);
    return masterComponents.concat(slaveComponents);
  }.property('content', 'content.hostComponents.length'),
  clients: function(){
    var clients = [];
    this.get('content.hostComponents').forEach(function(component){
      if(!component.get('componentName')){
        //temporary fix because of different data in hostComponents and serviceComponents
        return;
      }
      if (!component.get('isSlave') && !component.get('isMaster')) {
        if (clients.length) {
          clients[clients.length-1].set('isLast', false);
        }
        component.set('isLast', true);
        clients.push(component);
      }
    }, this);
    return clients;
  }.property('content'),

  addableComponentObject: Em.Object.extend({
    componentName: '',
    displayName: function(){
      return App.format.role(this.get('componentName'));
    }.property('componentName')
  }),
  isAddComponent: function(){
    return this.get('content.healthClass') !== 'health-status-DEAD-YELLOW';
  }.property('content.healthClass'),
  addableComponents:function(){
    var components = [];
    var services = App.Service.find();
    var dataNodeExists = false;
    var taskTrackerExists = false;
    var regionServerExists = false;

    this.get('content.hostComponents').forEach(function(component) {
      switch (component.get('componentName')) {
        case 'DATANODE':
          dataNodeExists = true;
          break;
        case 'TASKTRACKER':
          taskTrackerExists = true;
          break;
        case 'HBASE_REGIONSERVER':
          regionServerExists = true;
          break;
      }
    }, this);

    if (!dataNodeExists) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'DATANODE' }));
    }
    if (!taskTrackerExists && services.findProperty('serviceName', 'MAPREDUCE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'TASKTRACKER' }));
    }
    if (!regionServerExists && services.findProperty('serviceName', 'HBASE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'HBASE_REGIONSERVER' }));
    }
    return components;
  }.property('content', 'content.hostComponents.length'),

  ComponentView: Em.View.extend({
    content: null,
    didInsertElement: function () {
      if (this.get('isInProgress')) {
        this.doBlinking();
      }
    },
    hostComponent: function(){
      var hostComponent = null;
      var serviceComponent = this.get('content');
      var host = App.router.get('mainHostDetailsController.content');
      if(host){
        var hostComponent = host.get('hostComponents').findProperty('componentName', serviceComponent.get('componentName'));
      }
      return hostComponent;
    }.property('content', 'App.router.mainHostDetailsController.content'),
    workStatus: function(){
      var workStatus = this.get('content.workStatus');
      var hostComponent = this.get('hostComponent');
      if(hostComponent){
        workStatus = hostComponent.get('workStatus');
      }
      return workStatus;
    }.property('content.workStatus','hostComponent.workStatus'),
    statusClass: function(){
      var statusClass = null;
      if(this.get('isDataNode')){
        if(this.get('isDataNodeRecommissionAvailable') && this.get('isStart')){
          // Orange is shown only when service is started/starting and it is decommissioned.
          return 'health-status-DEAD-ORANGE';
        }
      }
      return 'health-status-' + App.HostComponentStatus.getKeyName(this.get('workStatus'));
    }.property('workStatus', 'isDataNodeRecommissionAvailable'),
    /**
     * For Upgrade failed state
     */
    isUpgradeFailed:function(){
      if(App.HostComponentStatus.getKeyName(this.get('workStatus')) == "upgrade_failed"){
        return true;
      }else{
        return false;
      }
    }.property("workStatus"),
    /**
     * Disable element while component is starting/stopping
     */
    disabledClass:function(){
      var workStatus = this.get('workStatus');
      if([App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown].contains(workStatus) ){
        return 'disabled';
      } else {
        return '';
      }
    }.property('workStatus'),
    /**
     * Do blinking for 1 minute
     */
    doBlinking : function(){
      var workStatus = this.get('workStatus');
      var self = this;
      var pulsate = [ App.HostComponentStatus.starting, App.HostComponentStatus.stopping ].contains(workStatus);
      if (!pulsate && this.get('isDataNode')) {
        var dataNodeComponent = this.get('content');
        if (dataNodeComponent)
          pulsate = dataNodeComponent.get('isDecommissioning');
      }
      if (pulsate) {
        this.$('.components-health').effect("pulsate", null, 1000, function () {
          self.doBlinking();
        });
      }
    },
    /**
     * Start blinking when host component is starting/stopping
     */
    startBlinking:function(){
      this.$('.components-health').stop(true, true);
      this.$('.components-health').css({opacity: 1.0});
      this.doBlinking();
    }.observes('workStatus'),

    isStart : function() {
      return (this.get('workStatus') === App.HostComponentStatus.started || this.get('workStatus') === App.HostComponentStatus.starting);
    }.property('workStatus'),

    isInProgress : function() {
      return (this.get('workStatus') === App.HostComponentStatus.stopping || this.get('workStatus') === App.HostComponentStatus.starting);
    }.property('workStatus'),
    /**
     * Shows whether we need to show Decommision/Recomission buttons
     */
    isDataNode: function() {
      return this.get('content.componentName') === 'DATANODE';
    }.property('content'),

    /**
     * Set in template via binding from parent view
     */
    decommissionDataNodeHostNames: null,
    /**
     * Decommission is available whenever the service is started.
     */
    isDataNodeDecommissionAvailable: function () {
      return this.get('isStart') && !this.get('isDataNodeRecommissionAvailable');
    }.property('isStart', 'isDataNodeRecommissionAvailable'),

    /**
     * Recommission is available only when this hostname shows up in the
     * 'decommissionDataNodeHostNames'
     */
    isDataNodeRecommissionAvailable: function () {
      var decommissionHostNames = this.get('decommissionDataNodeHostNames');
      var hostName = App.router.get('mainHostDetailsController.content.hostName');
      return decommissionHostNames!=null && decommissionHostNames.contains(hostName);
    }.property('App.router.mainHostDetailsController.content', 'decommissionDataNodeHostNames')

  }),
  timeSinceHeartBeat: function () {
    var d = this.get('content.lastHeartBeatTime');
    if (d) {
      return $.timeago(d);
    }
    return "";
  }.property('content.lastHeartBeatTime')
});