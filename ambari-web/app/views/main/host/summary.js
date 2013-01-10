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

  loadDecommisionNodesList: function () {
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
    this.loadDecommisionNodesList();
  },
  sortedComponents: function() {
    var slaveComponents = [];
    var masterComponents = [];
    this.get('content.components').forEach(function(component){
      if(!component.get('componentName')){
        //temporary fix because of different data in hostComponents and serviceComponents
        return;
      }
      if(component.get('isMaster')){
        masterComponents.push(component);
      } else if(!component.get('isClient')) {
        slaveComponents.push(component);
      }
    }, this);
    return masterComponents.concat(slaveComponents);
  }.property('content'),
  clients: function(){
    var clients = [];
    this.get('content.components').forEach(function(component){
      if(component.get('isClient')) {
        if(clients.length){
          clients[clients.length-1].set('isLast', false);
        }
        component.set('isLast', true);
        clients.push(component);
      }
    }, this);
    return clients;
  }.property('content'),

  ComponentView: Em.View.extend({
    content: null,

    statusClass: function(){
      var statusClass = null;
      if(this.get('isDataNode')){
        if(this.get('isDataNodeRecommissionAvailable') && this.get('isStart')){
          // Orange is shown only when service is started/starting and it is decommissioned.
          return 'health-status-DEAD-ORANGE';
        }
      }
      return 'health-status-' + App.Component.Status.getKeyName(this.get('content.workStatus'));
    }.property('content'),
    /**
     * Disable element while component is starting/stopping
     */
    disabledClass:function(){
      var workStatus = this.get('content.workStatus');
      if([App.Component.Status.starting, App.Component.Status.stopping].contains(workStatus) ){
        return 'disabled';
      } else {
        return '';
      }
    }.property('content.workStatus'),
    /**
     * Do blinking for 1 minute
     */
    doBlinking : function(){
      var workStatus = this.get('content.workStatus');
      var self = this;
      var pulsate = [ App.Component.Status.starting, App.Component.Status.stopping ].contains(workStatus);
      if (!pulsate && this.get('isDataNode')) {
        var dataNodeComponent = this.get('hostComponent');
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
      this.doBlinking();
    }.observes('content.workStatus', 'content'),

    isStart : function() {
      return (this.get('content.workStatus') === App.Component.Status.started || this.get('content.workStatus') === App.Component.Status.starting);
    }.property('content.workStatus'),
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