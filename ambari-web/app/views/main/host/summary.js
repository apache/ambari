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
  decommissionDatanodeHostnames: null,

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
                self.set('decommissionDatanodeHostnames', csv.split(','));
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
            self.set('decommissionDatanodeHostnames', hostNames);
          }
        }
      }
    }
    jQuery.ajax(getConfigAjax);
  },

  didInsertElement: function () {
    this.loadDecommisionNodesList();
  },

  ComponentButtonView: Em.View.extend({
    content: null,
    /**
     * Set in template via binding from parent view
     */
    decommissionDatanodeHostnames: null,

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

    adjustedIndex: function() {
      return this.get('_parentView.contentIndex') + 1;
    }.property(),

    positionButton: function() {
      return (this.get("adjustedIndex")%2 == 0) ? true : false;
    }.property('content.id') ,

    indicatorClass: function(){
      var indicatorClass = null;
      if(this.get('isDataNode')){
        if(this.get('isDataNodeRecommissionAvailable') && this.get('componentCheckStatus')){
          // Orange is shown only when service is started/starting and it is decommissioned.
          return 'health-status-DEAD-ORANGE';
        }
      }
      return 'health-status-' + App.Component.Status.getKeyName(this.get('hostComponent.workStatus'));
    }.property('componentCheckStatus', 'hostComponent.workStatus', 'isDataNode', 'isDataNodeRecommissionAvailable'),

    componentCheckStatus : function() {
      return (this.get('hostComponent.workStatus') === App.Component.Status.started || this.get('hostComponent.workStatus') === App.Component.Status.starting);
    }.property('hostComponent.workStatus'),

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
    }.observes('content.workStatus', 'hostComponent'),

    /**
     * Shows whether we need to show Decommision/Recomission buttons
     */
    isDataNode: function() {
      return this.get('content.componentName') === 'DATANODE';
    }.property('content'),

    /**
     * Decommission is available whenever the service is started.
     */
    isDataNodeDecommissionAvailable: function () {
      return this.get('componentCheckStatus') && !this.get('isDataNodeRecommissionAvailable');
    }.property('componentCheckStatus', 'isDataNodeRecommissionAvailable'),

    /**
     * Recommission is available only when this hostname shows up in the
     * 'decommissionDatanodeHostnames'
     */
    isDataNodeRecommissionAvailable: function () {
      var decommissionHostNames = this.get('decommissionDatanodeHostnames');
      var hostName = App.router.get('mainHostDetailsController.content.hostName');
      return decommissionHostNames!=null && decommissionHostNames.contains(hostName);
    }.property('App.router.mainHostDetailsController.content', 'decommissionDatanodeHostnames'),

    /**
     * Provides the host_component for the service_component associated with
     * this host.
     */
    hostComponent: function(){
      var hostComponent = null;
      var host = App.router.get('mainHostDetailsController.content');
      var componentName = this.get('content.componentName');
      if (host && componentName) {
        var hComponents = host.get('hostComponents');
        hostComponent = hComponents.findProperty("componentName", componentName);
      }
      return hostComponent;
    }.property('App.router.mainHostDetailsController.content', 'content.componentName'),
    
    /**
     * Shows whether we need to show health status
     */
    isClient: function() {
      var componentName = this.get('content.componentName');
      return componentName.substr(-7) === '_CLIENT' ||
        componentName === 'PIG' ||
        componentName === 'SQOOP';
    }.property('content')
  }),

  timeSinceHeartBeat: function () {
    var d = this.get('content.lastHeartBeatTime');
    if (d) {
      return $.timeago(d);
    }
    return "";
  }.property('content.lastHeartBeatTime')
});