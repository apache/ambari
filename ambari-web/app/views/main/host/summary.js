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
var uiEffects = require('utils/ui_effects');

App.MainHostSummaryView = Em.View.extend({
  templateName: require('templates/main/host/summary'),

  isStopCommand:true,

  content: function () {
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  showGangliaCharts: function () {
    var name = this.get('content.hostName');
    var gangliaMobileUrl = App.router.get('clusterController.gangliaUrl') + "/mobile_helper.php?show_host_metrics=1&h=" + name + "&c=HDPNameNode&r=hour&cs=&ce=";
    window.open(gangliaMobileUrl);
  },

  needToRestartComponentsCount: function() {
    return this.get('content.hostComponents').filterProperty('staleConfigs', true).length;
  }.property('content.hostComponents.@each.staleConfigs'),

  stopComponentsIsDisabled: function () {
    var staleComponents = this.get('sortedComponents').filterProperty('staleConfigs', true);
    if(!staleComponents.findProperty('workStatus','STARTED')){
      return true;
    } else {
      return false;
    }
  }.property('sortedComponents.@each.workStatus'),

  startComponentsIsDisabled:function () {
    var staleComponents = this.get('sortedComponents').filterProperty('staleConfigs', true);
    if(!staleComponents.findProperty('workStatus','INSTALLED')){
      return true;
    } else {
      return false;
    }
  }.property('sortedComponents.@each.workStatus'),

  needToRestartMessage: function() {
    return Em.I18n.t('hosts.host.details.needToRestart').format(this.get('needToRestartComponentsCount'));
  }.property('needToRestartComponentsCount'),

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
                if (csv!==null && csv.length>0) {
                  self.set('decommissionDataNodeHostNames', csv.split(','));  
                } else {
                  self.set('decommissionDataNodeHostNames', null);  
                }
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
    this.addToolTip();
  },
  addToolTip: function() {
    if (this.get('addComponentDisabled')) {
      $('#add_component').tooltip({title: Em.I18n.t('services.nothingToAdd')});
    }
  }.observes('addComponentDisabled'),
  sortedComponents: function () {
    var slaveComponents = [];
    var masterComponents = [];
    this.get('content.hostComponents').forEach(function (component) {
      if (component.get('isMaster')) {
        masterComponents.push(component);
      } else if (component.get('isSlave')) {
        slaveComponents.push(component);
      }
    }, this);
    return masterComponents.concat(slaveComponents);
  }.property('content', 'content.hostComponents.length'),
  clients: function () {
    var clients = [];
    this.get('content.hostComponents').forEach(function (component) {
      if (!component.get('componentName')) {
        //temporary fix because of different data in hostComponents and serviceComponents
        return;
      }
      if (!component.get('isSlave') && !component.get('isMaster')) {
        if (clients.length) {
          clients[clients.length - 1].set('isLast', false);
        }
        component.set('isLast', true);
        clients.push(component);
      }
    }, this);
    return clients;
  }.property('content'),

  addableComponentObject: Em.Object.extend({
    componentName: '',
    subComponentNames: null,
    displayName: function () {
      if (this.get('componentName') === 'CLIENTS') {
        return this.t('common.clients');
      }
      return App.format.role(this.get('componentName'));
    }.property('componentName')
  }),
  isAddComponent: function () {
    return this.get('content.healthClass') !== 'health-status-DEAD-YELLOW';
  }.property('content.healthClass'),

  addComponentDisabled: function() {
    return (!this.get('isAddComponent')) || (this.get('addableComponents.length') == 0);
  }.property('isAddComponent', 'addableComponents.length'),

  installableClientComponents: function() {
    var installableClients = [];
    if (!App.supports.deleteHost) {
      return installableClients;
    }
    App.Service.find().forEach(function(svc){
      switch(svc.get('serviceName')){
        case 'PIG':
          installableClients.push('PIG');
          break;
        case 'SQOOP':
          installableClients.push('SQOOP');
          break;
        case 'HCATALOG':
          installableClients.push('HCAT');
          break;
        case 'HDFS':
          installableClients.push('HDFS_CLIENT');
          break;
        case 'OOZIE':
          installableClients.push('OOZIE_CLIENT');
          break;
        case 'ZOOKEEPER':
          installableClients.push('ZOOKEEPER_CLIENT');
          break;
        case 'HIVE':
          installableClients.push('HIVE_CLIENT');
          break;
        case 'HBASE':
          installableClients.push('HBASE_CLIENT');
          break;
        case 'YARN':
          installableClients.push('YARN_CLIENT');
          break;
        case 'MAPREDUCE':
          installableClients.push('MAPREDUCE_CLIENT');
          break;
        case 'MAPREDUCE2':
          installableClients.push('MAPREDUCE2_CLIENT');
          break;
      }
    });
    this.get('content.hostComponents').forEach(function (component) {
      var index = installableClients.indexOf(component.get('componentName'));
      if (index > -1) {
        installableClients.splice(index, 1);
      }
    }, this);
    return installableClients;
  }.property('content', 'content.hostComponents.length', 'App.Service', 'App.supports.deleteHost'),
  
  addableComponents: function () {
    var components = [];
    var services = App.Service.find();
    var dataNodeExists = false;
    var taskTrackerExists = false;
    var regionServerExists = false;
    var zookeeperServerExists = false;
    var nodeManagerExists = false;
    var hbaseMasterExists = false;
    
    var installableClients = this.get('installableClientComponents');
    
    this.get('content.hostComponents').forEach(function (component) {
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
        case 'ZOOKEEPER_SERVER':
          zookeeperServerExists = true;
          break;
        case 'NODEMANAGER':
          nodeManagerExists = true;
          break;
        case 'HBASE_MASTER':
          hbaseMasterExists = true;
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
    if (!hbaseMasterExists && services.findProperty('serviceName', 'HBASE')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'HBASE_MASTER' }));
    }
    if (!zookeeperServerExists && services.findProperty('serviceName', 'ZOOKEEPER')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'ZOOKEEPER_SERVER' }));
    }
    if (!nodeManagerExists && services.findProperty('serviceName', 'YARN')) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'NODEMANAGER' }));
    }
    if (installableClients.length > 0) {
      components.pushObject(this.addableComponentObject.create({ 'componentName': 'CLIENTS', subComponentNames: installableClients }));
    }
    return components;
  }.property('content', 'content.hostComponents.length', 'installableClientComponents'),

  ComponentView: Em.View.extend({
    content: null,
    didInsertElement: function () {
      if (this.get('isInProgress')) {
        this.doBlinking();
      }
    },
    hostComponent: function () {
      var hostComponent = null;
      var serviceComponent = this.get('content');
      var host = App.router.get('mainHostDetailsController.content');
      if (host) {
        hostComponent = host.get('hostComponents').findProperty('componentName', serviceComponent.get('componentName'));
      }
      return hostComponent;
    }.property('content', 'App.router.mainHostDetailsController.content'),
    workStatus: function () {
      var workStatus = this.get('content.workStatus');
      var hostComponent = this.get('hostComponent');
      if (hostComponent) {
        workStatus = hostComponent.get('workStatus');
      }
      return workStatus;
    }.property('content.workStatus', 'hostComponent.workStatus'),

    /**
     * Return host component text status
     */
    componentTextStatus: function () {
      var workStatus = this.get("workStatus");
      var componentTextStatus = this.get('content.componentTextStatus');
      var hostComponent = this.get('hostComponent');
      if (hostComponent) {
        componentTextStatus = hostComponent.get('componentTextStatus');
        if(this.get("isDataNode"))
          if(this.get('isDataNodeRecommissionAvailable')){
            if(hostComponent.get('isDecommissioning')){
              componentTextStatus = "Decommissioning...";
            } else {
              componentTextStatus = "Decommissioned";
            }
          }
      }
      return componentTextStatus;
    }.property('workStatus','isDataNodeRecommissionAvailable'),

    statusClass: function () {
      //If the component is DataNode
      if (this.get('isDataNode')) {
        if (this.get('isDataNodeRecommissionAvailable') && (this.get('isStart') || this.get('workStatus') == 'INSTALLED')) {
          return 'health-status-DEAD-ORANGE';
        }
      }

      //Class when install failed
      if (this.get('workStatus') === App.HostComponentStatus.install_failed) {
        return 'health-status-color-red icon-cog';
      }

      //Class when installing
      if (this.get('workStatus') === App.HostComponentStatus.installing) {
        return 'health-status-color-blue icon-cog';
      }

      //For all other cases
      return 'health-status-' + App.HostComponentStatus.getKeyName(this.get('workStatus'));
    }.property('workStatus', 'isDataNodeRecommissionAvailable', 'this.content.isDecommissioning'),

    disabled: function() {
      return this.get('parentView.content.isNotHeartBeating')?'disabled':'';
    }.property('parentView.content.isNotHeartBeating'),
    /**
     * For Upgrade failed state
     */
    isUpgradeFailed: function () {
      return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "upgrade_failed";
    }.property("workStatus"),
    /**
     * For Install failed state
     */
    isInstallFailed: function () {
      return App.HostComponentStatus.getKeyName(this.get('workStatus')) === "install_failed";
    }.property("workStatus"),
    /**
     * Do blinking for 1 minute
     */
    doBlinking: function () {
      var workStatus = this.get('workStatus');
      var self = this;
      var pulsate = [ App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.installing].contains(workStatus);
      if (!pulsate && this.get('isDataNode')) {
        var dataNodeComponent = this.get('content');
        if (dataNodeComponent && workStatus != "INSTALLED") {
          pulsate = this.get('isDecommissioning');
        }
      }
      if (pulsate && !self.get('isBlinking')) {
        self.set('isBlinking', true);
        uiEffects.pulsate(self.$('.components-health'), 1000, function () {
          self.set('isBlinking', false);
          self.doBlinking();
        });
      }
    },
    /**
     * Start blinking when host component is starting/stopping
     */
    startBlinking: function () {
      this.$('.components-health').stop(true, true);
      this.$('.components-health').css({opacity: 1.0});
      this.doBlinking();
    }.observes('workStatus','isDataNodeRecommissionAvailable'),

    isStart: function () {
      return (this.get('workStatus') == App.HostComponentStatus.started || this.get('workStatus') == App.HostComponentStatus.starting);
    }.property('workStatus'),

    isInstalling: function () {
      return (this.get('workStatus') == App.HostComponentStatus.installing);
    }.property('workStatus'),
    /**
     * No action available while component is starting/stopping/unknown
     */
    noActionAvailable: function () {
      var workStatus = this.get('workStatus');
      if ([App.HostComponentStatus.starting, App.HostComponentStatus.stopping, App.HostComponentStatus.unknown].contains(workStatus)) {
        return "hidden";
      }else{
        return "";
      }
    }.property('workStatus'),

    isInProgress: function () {
      return (this.get('workStatus') === App.HostComponentStatus.stopping || 
          this.get('workStatus') === App.HostComponentStatus.starting) || 
          this.get('isDecommissioning');
    }.property('workStatus', 'isDataNodeRecommissionAvailable'),
    /**
     * Shows whether we need to show Decommision/Recomission buttons
     */
    isDataNode: function () {
      return this.get('content.componentName') === 'DATANODE';
    }.property('content'),

    isDecommissioning: function () {
      var hostComponentDecommissioning = this.get('hostComponent.isDecommissioning');
      return this.get('isDataNode') &&  this.get("isDataNodeRecommissionAvailable") && hostComponentDecommissioning;
    }.property("workStatus", "isDataNodeRecommissionAvailable", "hostComponent.isDecommissioning"),

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
      return decommissionHostNames != null && decommissionHostNames.contains(hostName);
    }.property('App.router.mainHostDetailsController.content', 'decommissionDataNodeHostNames'),

    /**
     * Shows whether we need to show Delete button
     */
    isHBaseMaster: function () {
      return this.get('content.componentName') === 'HBASE_MASTER';
    }.property('content'),
    isDeleteHBaseMasterDisabled: function () {
      return !(this.get('workStatus') == App.HostComponentStatus.stopped || this.get('workStatus') == App.HostComponentStatus.unknown ||
        this.get('workStatus') == App.HostComponentStatus.install_failed || this.get('workStatus') == App.HostComponentStatus.upgrade_failed);
    }.property('workStatus'),

    isReassignable: function () {
      return App.supports.reassignMaster && App.reassignableComponents.contains(this.get('content.componentName')) && App.Host.find().content.length > 1;
    }.property('content.componentName')

  }),
  timeSinceHeartBeat: function () {
    var d = this.get('content.lastHeartBeatTime');
    if (d) {
      return $.timeago(d);
    }
    return "";
  }.property('content.lastHeartBeatTime')
});