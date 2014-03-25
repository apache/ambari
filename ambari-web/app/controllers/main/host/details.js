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
var batchUtils = require('utils/batch_scheduled_requests');

App.MainHostDetailsController = Em.Controller.extend({
  name: 'mainHostDetailsController',
  content: null,
  isFromHosts: false,

  /**
   * path to page visited before
   */
  referer: '',
  /**
   * open dashboard page
   */
  routeHome: function () {
    App.router.transitionTo('main.dashboard');
  },

  /**
   * open summary page of the selected service
   * @param event
   */
  routeToService: function(event){
    var service = event.context;
    App.router.transitionTo('main.services.service.summary',service);
  },

  serviceActiveComponents: function() {
    return this.get('content.hostComponents').filterProperty('service.isInPassive',false);
  }.property('content.hostComponents'),

  serviceNonClientActiveComponents: function() {
    return this.get('serviceActiveComponents').filterProperty('isClient',false);
  }.property('serviceActiveComponents'),


  /**
   * Send specific command to server
   * @param url
   * @param _method
   * @param postData
   * @param callback
   */
  sendCommandToServer : function(url, postData, _method, callback){
    var url =  (App.testMode) ?
      '/data/wizard/deploy/poll_1.json' : //content is the same as ours
      App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;

    var method = App.testMode ? 'GET' : _method;

    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(postData),
      dataType: 'json',
      timeout: App.timeout,
      success: function(data){
        if(data && data.Requests){
          callback(data.Requests.id);
        } else{
          callback(null);
          console.log('cannot get request id from ', data);
        }
      },

      error: function (request, ajaxOptions, error) {
        //do something
        console.log('error on change component host status');
        App.ajax.defaultErrorHandler(request, url, method);
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * send command to server to start selected host component
   * @param event
   */
  startComponent: function (event) {
    var self = this;
    App.showConfirmationPopup(function() {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.startHostComponent') + " " + component.get('displayName');
      self.sendStartComponentCommand(component, context);
    });
  },
  
  /**
   * PUTs a command to server to start a component. If no 
   * specific component is provided, all components are started.
   * @param component  When <code>null</code> all startable components are started. 
   * @param context  Context under which this command is beign sent. 
   */
  sendStartComponentCommand: function(components, context) {
    var url = Em.isArray(components) ?
        '/hosts/' + this.get('content.hostName') + '/host_components' :
        '/hosts/' + this.get('content.hostName') + '/host_components/' + components.get('componentName').toUpperCase();
    var dataToSend = {
      RequestInfo : {
        "context" : context
      },
      Body:{
        HostRoles:{
          state: 'STARTED'
        }
      }
    };
    if (Em.isArray(components)) {
      dataToSend.RequestInfo.query = "HostRoles/component_name.in(" + components.mapProperty('componentName').join(',') + ")";
    }
    this.sendCommandToServer(url, dataToSend, 'PUT',
      function(requestId){

      if(!requestId){
        return;
      }

      console.log('Send request for STARTING successfully');

      if (App.testMode) {
        if(Em.isArray(components)){
          var allComponents = this.get('content.hostComponents');
          allComponents.forEach(function(component){
            component.set('workStatus', App.HostComponentStatus.stopping);
            setTimeout(function(){
              component.set('workStatus', App.HostComponentStatus.stopped);
            },App.testModeDelayForActions);
          });
        } else {
          components.set('workStatus', App.HostComponentStatus.starting);
          setTimeout(function(){
            components.set('workStatus', App.HostComponentStatus.started);
          },App.testModeDelayForActions);
        }
      } else {
        App.router.get('clusterController').loadUpdatedStatusDelayed(500);
      }
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });

    });
  },

  /**
   * send command to server to delete selected host component
   *
   */
  deleteComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName');
    var displayName = component.get('displayName');
    var isLastComponent = (App.HostComponent.find().filterProperty('componentName', componentName).get('length') === 1);
    App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      primary: Em.I18n.t('hosts.host.deleteComponent.popup.confirm'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/host/details/deleteComponentPopup')
      }),
      isChecked: false,
      disablePrimary: function () {
        return !this.get('isChecked');
      }.property('isChecked'),
      lastComponent: function() {
        if (isLastComponent) {
          this.set('isChecked', false);
          return true;
        } else {
          this.set('isChecked', true);
          return false;
        }
      }.property(),
      lastComponentError:  Em.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.warning').format(displayName))
      }),
      restartNagiosMsg: Em.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.msg2').format(displayName))
      }),
      deleteComponentMsg: function() {
        return Em.I18n.t('hosts.host.deleteComponent.popup.msg1').format(displayName);
      }.property(),
      onPrimary: function () {
        self._doDeleteHostComponent(component);
        self.set('redrawComponents', true);
        this.hide();
      }
    });

  },

  /**
   * Trigger to reset list of master/slaves components on the view
   * @type {bool}
   */
  redrawComponents: false,

  /**
   * Deletes the given host component, or all host components.
   * 
   * @param component  When <code>null</code> all host components are deleted.
   * @return  <code>null</code> when components get deleted.
   *          <code>{xhr: XhrObj, url: "http://", method: "DELETE"}</code> 
   *          when components failed to get deleted. 
   */
  _doDeleteHostComponent: function(component) {
    var url = component !== null ? 
        '/hosts/' + this.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase() : 
        '/hosts/' + this.get('content.hostName') + '/host_components';
    url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;
    var deleted = null;
    $.ajax({
      type: 'DELETE',
      url: url,
      timeout: App.timeout,
      async: false,
      success: function (data) {
        deleted = null;
      },
      error: function (xhr, textStatus, errorThrown) {
        console.log('Error deleting host component');
        console.log(textStatus);
        console.log(errorThrown);
        deleted = {xhr: xhr, url: url, method: 'DELETE'};
      },
      statusCode: require('data/statusCodes')
      });
    return deleted;
  },

  /**
   * send command to server to upgrade selected host component
   * @param event
   */
  upgradeComponent: function (event) {
    var self = this;
    var component = event.context;
    App.showConfirmationPopup(function() {
      self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(),{
            RequestInfo : {
              "context" : Em.I18n.t('requestInfo.upgradeHostComponent') + " " + component.get('displayName')
            },
            Body:{
              HostRoles:{
                stack_id: 'HDP-1.2.2',
                state: 'INSTALLED'
              }
            }
          }, 'PUT',
          function(requestId){
            if(!requestId){
              return;
            }

            console.log('Send request for UPGRADE successfully');

            if (App.testMode) {
              component.set('workStatus', App.HostComponentStatus.starting);
              setTimeout(function(){
                component.set('workStatus', App.HostComponentStatus.started);
              },App.testModeDelayForActions);
            } else {
              App.router.get('clusterController').loadUpdatedStatusDelayed(500);
            }

            // load data (if we need to show this background operations popup) from persist
            App.router.get('applicationController').dataLoading().done(function (initValue) {
              if (initValue) {
                App.router.get('backgroundOperationsController').showPopup();
              }
            });

          });
    });
  },
  /**
   * send command to server to stop selected host component
   * @param event
   */
  stopComponent: function (event) {
    var self = this;
    App.showConfirmationPopup(function() {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.stopHostComponent')+ " " + component.get('displayName');
      self.sendStopComponentCommand(component, context);
    });
  },
  
  /**
   * PUTs a command to server to stop a component. If no 
   * specific component is provided, all components are stopped.
   * @param component  When <code>null</code> all components are stopped. 
   * @param context  Context under which this command is beign sent. 
   */
  sendStopComponentCommand: function(components, context){
    var url = Em.isArray(components) ?
        '/hosts/' + this.get('content.hostName') + '/host_components' :
        '/hosts/' + this.get('content.hostName') + '/host_components/' + components.get('componentName').toUpperCase();
    var dataToSend = {
      RequestInfo : {
        "context" : context
      },
      Body:{
        HostRoles:{
          state: 'INSTALLED'
        }
      }
    };
    if (Em.isArray(components)) {
      dataToSend.RequestInfo.query = "HostRoles/component_name.in(" + components.mapProperty('componentName').join(',') + ")";
    }
    this.sendCommandToServer( url, dataToSend, 'PUT',
      function(requestId){
      if(!requestId){
        return;
      }

      console.log('Send request for STOPPING successfully');

      if (App.testMode) {
        if(Em.isArray(components)){
          components.forEach(function(component){
            component.set('workStatus', App.HostComponentStatus.stopping);
            setTimeout(function(){
              component.set('workStatus', App.HostComponentStatus.stopped);
            },App.testModeDelayForActions);
          });
        } else {
          components.set('workStatus', App.HostComponentStatus.stopping);
          setTimeout(function(){
            components.set('workStatus', App.HostComponentStatus.stopped);
          },App.testModeDelayForActions);
        }

      } else {
        App.router.get('clusterController').loadUpdatedStatusDelayed(500);
      }

      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },

  restartComponent: function(event) {
    var self = this;
    var component = event.context;
    App.showConfirmationPopup(function(){
      batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('componentName'), self.get('content.hostName')));
    });
  },
  /**
   * send command to server to install selected host component
   * @param event
   */
  addComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName').toUpperCase().toString();
    var subComponentNames = component.get('subComponentNames');
    var displayName = component.get('displayName');

    var securityEnabled = App.router.get('mainAdminSecurityController').getUpdatedSecurityStatus();

    if (componentName === 'ZOOKEEPER_SERVER') {
      App.showConfirmationPopup(function() {
        self.primary(component);
      }, Em.I18n.t('hosts.host.addComponent.addZooKeeper'));
    }
    else {
      if (securityEnabled) {
        App.showConfirmationPopup(function() {
          self.primary(component);
        }, Em.I18n.t('hosts.host.addComponent.securityNote').format(componentName,self.get('content.hostName')));
      }
      else {
        var dn = displayName;
        if (subComponentNames !== null && subComponentNames.length > 0) {
          var dns = [];
          subComponentNames.forEach(function(scn){
            dns.push(App.format.role(scn));
          });
          dn += " ("+dns.join(", ")+")";
        }
        App.ModalPopup.show({
          primary: Em.I18n.t('hosts.host.addComponent.popup.confirm'),
          header: Em.I18n.t('popup.confirmation.commonHeader'),
          addComponentMsg: function() {
            return Em.I18n.t('hosts.host.addComponent.msg').format(dn);
          }.property(),
          bodyClass: Ember.View.extend({
            templateName: require('templates/main/host/details/addComponentPopup')
          }),
          restartNagiosMsg : Em.View.extend({
            template: Ember.Handlebars.compile(Em.I18n.t('hosts.host.addComponent.note').format(dn))
          }),
          onPrimary: function () {
            this.hide();
            if (component.get('componentName') === 'CLIENTS') {
              // Clients component has many sub-components which
              // need to be installed.
              var scs = component.get('subComponentNames');
              scs.forEach(function (sc, index) {
                var c = Em.Object.create({
                  displayName: App.format.role(sc),
                  componentName: sc
                });
                self.primary(c, scs.length - index === 1);
              });
            } else {
              self.primary(component, true);
            }
          }
        });
      }
    }
  },

  primary: function(component, showPopup) {
    var self = this;
    var componentName = component.get('componentName').toUpperCase().toString();
    var displayName = component.get('displayName');

    self.sendCommandToServer('/hosts?Hosts/host_name=' + self.get('content.hostName'), {
        RequestInfo: {
          "context": Em.I18n.t('requestInfo.installHostComponent') + " " + displayName
        },
        Body: {
          host_components: [
            {
              HostRoles: {
                component_name: componentName
              }
            }
          ]
        }
      },
      'POST',
      function (requestId) {

        console.log('Send request for ADDING NEW COMPONENT successfully');

        self.sendCommandToServer('/host_components?HostRoles/host_name=' + self.get('content.hostName') + '\&HostRoles/component_name=' + componentName + '\&HostRoles/state=INIT', {
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installNewHostComponent') + " " + displayName
            },
            Body: {
              HostRoles: {
                state: 'INSTALLED'
              }
            }
          },
          'PUT',
          function (requestId) {
            if (!requestId) {
              return;
            }

            console.log('Send request for INSTALLING NEW COMPONENT successfully');

            if (App.testMode) {
              component.set('workStatus', App.HostComponentStatus.installing);
              setTimeout(function () {
                component.set('workStatus', App.HostComponentStatus.stopped);
              }, App.testModeDelayForActions);
            } else {
              App.router.get('clusterController').loadUpdatedStatusDelayed(500);
            }

            // load data (if we need to show this background operations popup) from persist
            App.router.get('applicationController').dataLoading().done(function (initValue) {
              if (initValue) {
                App.router.get('backgroundOperationsController').showPopup();
              }
              if (componentName === 'ZOOKEEPER_SERVER') {
                self.set('zkRequestId', requestId);
                self.addObserver('App.router.backgroundOperationsController.serviceTimestamp', self, self.checkZkConfigs);
                self.checkZkConfigs();
              }
            });
          });
      });
  },
  /**
   * Load tags
   */
  checkZkConfigs: function() {
    var bg = App.router.get('backgroundOperationsController.services').findProperty('id', this.get('zkRequestId'));
    if (!bg) return;
    if (!bg.get('isRunning')) {
      this.loadConfigs();
    }
  },
  loadConfigs: function() {
    this.removeObserver('App.router.backgroundOperationsController.serviceTimestamp', this, this.checkZkConfigs);
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'loadConfigsSuccessCallback'
    });
  },
  /**
   * Load needed configs
   * @param data
   */
  loadConfigsSuccessCallback: function(data) {
    var urlParams = [];
    urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
    if (App.Service.find().someProperty('serviceName', 'HBASE')) {
      urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
    }
    if (App.Service.find().someProperty('serviceName', 'HIVE')) {
      urlParams.push('(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')');
    }
    if (App.Service.find().someProperty('serviceName', 'STORM')) {
      urlParams.push('(type=storm-site&tag=' + data.Clusters.desired_configs['storm-site'].tag + ')');
    }
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'setNewZkConfigs'
    });
  },
  /**
   * Set new values for some configs (based on available ZooKeeper Servers)
   * @param data
   */
  setNewZkConfigs: function(data) {
    var configs = [];
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
    }, this);

    var zks = this.getZkServerHosts();
    var zks_with_port = '';
    zks.forEach(function(zk) {
      zks_with_port += zk + ':2181,';
    });
    zks_with_port = zks_with_port.slice(0,-1);

    if (App.get('isHaEnabled')) {
      configs['core-site']['ha.zookeeper.quorum'] = zks_with_port;
    }
    if (configs['hbase-site']) {
      configs['hbase-site']['hbase.zookeeper.quorum'] = zks.join(',');
    }
    if (configs['webhcat-site']) {
      configs['webhcat-site']['templeton.zookeeper.hosts'] = zks_with_port;
    }
    if (configs['storm-site']) {
      configs['storm-site']['storm.zookeeper.servers'] = JSON.stringify(zks).replace(/"/g, "'");
    }
    for (var site in configs) {
      if (!configs.hasOwnProperty(site)) continue;
      App.ajax.send({
        name: 'reassign.save_configs',
        sender: this,
        data: {
          siteName: site,
          properties: configs[site]
        }
      });
    }
  },

  /**
   * Is deleteHost action id fired
   */
  fromDeleteHost: false,

  getZkServerHosts: function() {
    var zks = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('host.hostName');
    if (this.get('fromDeleteHost')) {
      this.set('fromDeleteHost', false);
      return zks.without(this.get('content.hostName'));
    }
    return zks;
  },

  /**
   * send command to server to install selected host component
   * @param event
   * @param context
   */
  installComponent: function (event, context) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName').toUpperCase().toString();
    var displayName = component.get('displayName');

    App.ModalPopup.show({
      primary: Em.I18n.t('hosts.host.installComponent.popup.confirm'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      installComponentMessage: function(){
        return Em.I18n.t('hosts.host.installComponent.msg').format(displayName);
      }.property(),
      restartNagiosMsg : Em.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('hosts.host.addComponent.note').format(displayName))
      }),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/host/details/installComponentPopup')
      }),
      onPrimary: function () {
        this.hide();
        self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(), {
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + displayName
            },
            Body: {
              HostRoles: {
                state: 'INSTALLED'
              }
            }
          },
          'PUT',
          function (requestId) {
            if (!requestId) {
              return;
            }

            console.log('Send request for REINSTALL COMPONENT successfully');

            if (App.testMode) {
              component.set('workStatus', App.HostComponentStatus.installing);
              setTimeout(function () {
                component.set('workStatus', App.HostComponentStatus.stopped);
              }, App.testModeDelayForActions);
            } else {
              App.router.get('clusterController').loadUpdatedStatusDelayed(500);
            }

            // load data (if we need to show this background operations popup) from persist
            App.router.get('applicationController').dataLoading().done(function (initValue) {
              if (initValue) {
                App.router.get('backgroundOperationsController').showPopup();
              }
            });
          });
      }
    });
  },

  /**
   * send command to server to run decommission on DATANODE, TASKTRACKER, NODEMANAGER, REGIONSERVER
   * @param component
   */
  decommission: function(component){
    var self = this;
    App.showConfirmationPopup(function(){
      var svcName = component.get('service.serviceName');
      var hostName = self.get('content.hostName');
      // HDFS service, decommission DataNode
      if (svcName === "HDFS") {
        self.doDecommission(hostName, svcName, "NAMENODE", "DATANODE");
      }
      // YARN service, decommission NodeManager
      if (svcName === "YARN") {
        self.doDecommission(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
      }
      // MAPREDUCE service, decommission TaskTracker
      if (svcName === "MAPREDUCE") {
        self.doDecommission(hostName, svcName, "JOBTRACKER", "TASKTRACKER");
      }
      // HBASE service, decommission RegionServer
      if (svcName === "HBASE") {
        self.doDecommissionRegionServer(hostName, svcName, "HBASE_MASTER", "HBASE_REGIONSERVER");
      }

      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },
  
  /**
   * send command to server to run recommission on DATANODE, TASKTRACKER, NODEMANAGER
   * @param component
   */
  recommission: function(component){
    var self = this;
    App.showConfirmationPopup(function(){
      var svcName = component.get('service.serviceName');
      var hostName = self.get('content.hostName');
      // HDFS service, Recommission datanode
      if (svcName === "HDFS") {
        self.doRecommissionAndStart(hostName, svcName, "NAMENODE", "DATANODE");
      }
      // YARN service, Recommission nodeManager
      if (svcName === "YARN") {
        self.doRecommissionAndStart(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
      }
      // MAPREDUCE service, Recommission taskTracker
      if (svcName === "MAPREDUCE") {
        self.doRecommissionAndRestart(hostName, svcName, "JOBTRACKER", "TASKTRACKER");
      }
      // HBASE service, Recommission RegionServer
      if (svcName === "HBASE") {
        self.doRecommissionAndStart(hostName, svcName, "HBASE_MASTER", "HBASE_REGIONSERVER");
      }

      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },

  /**
   * Performs Decommission (for DN, TT and NM)
   */
  doDecommission: function(hostName, serviceName, componentName, slaveType){
    var contextNameString = 'hosts.host.' + slaveType.toLowerCase() + '.decommission';
    var context = Em.I18n.t(contextNameString);
    App.ajax.send({
      name: 'host.host_component.decommission_slave',
      sender: this,
      data: {
        context: context,
        command: 'DECOMMISSION',
        hostName: hostName,
        serviceName: serviceName ,
        componentName: componentName,
        slaveType: slaveType
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  /**
   * Performs Decommission (for RegionServer)
   */
  doDecommissionRegionServer: function(hostNames, serviceName, componentName, slaveType){

    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize : 0,
        batches:[
          {
            "order_id" : 1,
            "type" : "POST",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                "context" : Em.I18n.t('hosts.host.regionserver.decommission.batch1'),
                "command" : "DECOMMISSION",
                "parameters" : {
                  "slave_type": slaveType,
                  "excluded_hosts": hostNames
                }
              },
              "Requests/resource_filters": [{"service_name" : serviceName, "component_name" : componentName}]
            }
          },
          {
            "order_id": 2,
            "type": "PUT",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/host_components",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                context: Em.I18n.t('hosts.host.regionserver.decommission.batch2'),
                query: 'HostRoles/component_name=' + slaveType + '&HostRoles/host_name.in(' + hostNames + ')&HostRoles/maintenance_state=OFF'
              },
              "Body": {
                HostRoles: {
                  state: "INSTALLED"
                }
              }
            }
          },
          {
            "order_id" : 3,
            "type" : "POST",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                "context" : Em.I18n.t('hosts.host.regionserver.decommission.batch3'),
                "command" : "DECOMMISSION",
                "service_name" : serviceName,
                "component_name" : componentName,
                "parameters" : {
                  "slave_type": slaveType,
                  "excluded_hosts": hostNames,
                  "mark_draining_only": "true"
                }
              }
            }
          }
        ]
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  decommissionErrorCallback: function (request, ajaxOptions, error) {
    console.log('ERROR: '+ error);
  },
  /**
   * Success ajax response for Recommission/Decommission slaves
   * @param data
   * @param ajaxOptions
   */
  decommissionSuccessCallback: function(data, ajaxOptions) {
    if(data && (data.Requests || data.resources[0].RequestSchedule) ) {
      if (!App.testMode) {
        App.router.get('clusterController').loadUpdatedStatusDelayed(500);
      }
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
    else {
      console.log('cannot get request id from ', data);
    }
  },

  doRecommissionAndStart:  function(hostNames, serviceName, componentName, slaveType){
    var contextNameString_1 = 'hosts.host.' + slaveType.toLowerCase() + '.recommission';
    var context_1 = Em.I18n.t(contextNameString_1);
    var contextNameString_2 = 'requestInfo.startHostComponent.' + slaveType.toLowerCase();
    var startContext = Em.I18n.t(contextNameString_2);
    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize : 1,
        batches:[
          {
            "order_id" : 1,
            "type" : "POST",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                "context" : context_1,
                "command" : "DECOMMISSION",
                "parameters" : {
                  "slave_type": slaveType,
                  "included_hosts": hostNames
                }
              },
              "Requests/resource_filters": [{"service_name" : serviceName, "component_name" : componentName}]
            }
          },
          {
            "order_id": 2,
            "type": "PUT",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/host_components",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                context: startContext,
                query: 'HostRoles/component_name=' + slaveType + '&HostRoles/host_name.in(' + hostNames + ')&HostRoles/maintenance_state=OFF'
              },
              "Body": {
                HostRoles: {
                  state: "STARTED"
                }
              }
            }
          }
        ]
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },
  doRecommissionAndRestart:  function(hostNames, serviceName, componentName, slaveType){
    var contextNameString_1 = 'hosts.host.' + slaveType.toLowerCase() + '.recommission';
    var context_1 = Em.I18n.t(contextNameString_1);
    var contextNameString_2 = 'hosts.host.' + slaveType.toLowerCase() + '.restart';
    var context_2 = Em.I18n.t(contextNameString_2);
    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize : 1,
        batches:[
          {
            "order_id" : 1,
            "type" : "POST",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                "context" : context_1,
                "command" : "DECOMMISSION",
                "parameters" : {
                  "slave_type": slaveType,
                  "included_hosts": hostNames
                }
              },
              "Requests/resource_filters": [{"service_name" : serviceName, "component_name" : componentName}]
            }
          },
          {
            "order_id" : 2,
            "type" : "POST",
            "uri" : App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo" : {
              "RequestInfo" : {
                "context" : context_2,
                "command" : "RESTART",
                "service_name" : serviceName,
                "component_name" : slaveType,
                "hosts" : hostNames
              }
            }
          }
        ]
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  doAction: function(option) {
    switch (option.context.action) {
      case "deleteHost":
        this.validateAndDeleteHost();
        break;
      case "startAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStartAllComponents();
        break;
      case "stopAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStopAllComponents();
        break;
      case "restartAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doRestartAllComponents();
        break;
      case "onOffPassiveModeForHost":
        this.onOffPassiveModeForHost(option.context);
        break;
      default:
        break;
    }
  },

  onOffPassiveModeForHost: function(context) {
    var state = context.active ? 'ON' : 'OFF';
    var self = this;
    var message = context.label + ' for host';
    App.showConfirmationPopup(function() {
          self.hostPassiveModeRequest(state, message)
        },
        Em.I18n.t('hosts.passiveMode.popup').format(context.active ? 'On' : 'Off',this.get('content.hostName'))
    );
  },

  hostPassiveModeRequest: function(state,message) {
    App.ajax.send({
      name: 'bulk_request.hosts.passive_state',
      sender: this,
      data: {
        hostNames: this.get('content.hostName'),
        passive_state: state,
        requestInfo: message
      },
      success: 'updateHost'
    });
  },

  updateHost: function(data, opt, params) {
    this.set('content.passiveState', params.passive_state);
    App.router.get('clusterController').loadUpdatedStatus(function(){
      batchUtils.infoPassiveState(params.passive_state);
    });
 },

  doStartAllComponents: function() {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');
    var componentsLength = components == null ? 0 : components.get('length');
    if (componentsLength > 0) {
      App.showConfirmationPopup(function() {
        self.sendStartComponentCommand(components, Em.I18n.t('hosts.host.maintainance.startAllComponents.context'));
      });
    }
  },
  
  doStopAllComponents: function() {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');
    var componentsLength = components == null ? 0 : components.get('length');
    if (componentsLength > 0) {
      App.showConfirmationPopup(function() {
        self.sendStopComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'));
      });
    }
  },

  doRestartAllComponents: function() {
    var self = this;
    var components = this.get('serviceActiveComponents');
    var componentsLength = components == null ? 0 : components.get('length');
    if (componentsLength > 0) {
      App.showConfirmationPopup(function() {
        batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')));
      });
    }
  },

  /**
   * Deletion of hosts not supported for this version
   */
  validateAndDeleteHost: function () {
    if (!App.supports.deleteHost) {
      return;
    }
    var stoppedStates = [App.HostComponentStatus.stopped,
                         App.HostComponentStatus.install_failed,
                         App.HostComponentStatus.upgrade_failed,
                         App.HostComponentStatus.init,
                         App.HostComponentStatus.unknown];
    var masterComponents = [];
    var runningComponents = [];
    var unknownComponents = [];
    var nonDeletableComponents = [];
    var lastComponents = [];
    var componentsOnHost = this.get('content.hostComponents');
    var allComponents = App.HostComponent.find();
    var zkServerInstalled = false;
    if (componentsOnHost && componentsOnHost.get('length') > 0) {
      componentsOnHost.forEach(function (cInstance) {
        if (cInstance.get('componentName') === 'ZOOKEEPER_SERVER') {
          zkServerInstalled = true;
        }
        if (allComponents.filterProperty('componentName', cInstance.get('componentName')).get('length') === 1) {
          lastComponents.push(cInstance.get('displayName'));
        }
        var workStatus = cInstance.get('workStatus');
        if (cInstance.get('isMaster') && !cInstance.get('isDeletable')) {
          masterComponents.push(cInstance.get('displayName'));
        }
        if (stoppedStates.indexOf(workStatus) < 0) {
          runningComponents.push(cInstance.get('displayName'));
        }
        if (!cInstance.get('isDeletable')) {
          nonDeletableComponents.push(cInstance.get('displayName'));
        }
        if (workStatus === App.HostComponentStatus.unknown) {
          unknownComponents.push(cInstance.get('displayName'));
        }
      });
    }
    if (masterComponents.length > 0) {
      this.raiseDeleteComponentsError(masterComponents, 'masterList');
      return;
    } else if (nonDeletableComponents.length > 0) {
      this.raiseDeleteComponentsError(nonDeletableComponents, 'nonDeletableList');
      return;
    } else if (runningComponents.length > 0) {
      this.raiseDeleteComponentsError(runningComponents, 'runningList');
      return;
    }
    if (zkServerInstalled) {
      var self = this;
      App.showConfirmationPopup(function() {
        self._doDeleteHost(unknownComponents, lastComponents);
      }, Em.I18n.t('hosts.host.addComponent.deleteHostWithZooKeeper'));
    }
    else {
      this._doDeleteHost(unknownComponents, lastComponents);
    }
  },
  
  raiseDeleteComponentsError: function (components, type) {
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.cant.do.popup.title'),
      type: type,
      showBodyEnd: function() {
        return this.get('type') === 'runningList' || this.get('type') === 'masterList';
      }.property(),
      components: components,
      componentsStr: function() {
        return this.get('components').join(", ");
      }.property(),
      componentsBody: function() {
        return Em.I18n.t('hosts.cant.do.popup.'+type+'.body').format(this.get('components').length);
      }.property(),
      componentsBodyEnd: function() {
        if (this.get('showBodyEnd')) {
          return Em.I18n.t('hosts.cant.do.popup.'+type+'.body.end');
        }
        return '';
      }.property(),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/raiseDeleteComponentErrorPopup')
      }),
      secondary: null
    })
  },

  /**
   * show confirmation popup to delete host
   */
  _doDeleteHost: function(unknownComponents,lastComponents) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.title'),
      deletePopupBody: function() {
        return Em.I18n.t('hosts.delete.popup.body').format(self.get('content.publicHostName'));
      }.property(),
      lastComponent: function() {
         if (lastComponents && lastComponents.length) {
           this.set('isChecked', false);
           return true;
         } else {
           this.set('isChecked', true);
           return false;
         }
      }.property(),
      disablePrimary: function () {
        return !this.get('isChecked');
      }.property('isChecked'),
      isChecked: false,
      lastComponentError:  Em.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('hosts.delete.popup.body.msg4').format(lastComponents))
      }),
      unknownComponents: function() {
        if (unknownComponents && unknownComponents.length) {
          return unknownComponents.join(", ");
        }
        return '';
      }.property(),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/doDeleteHostPopup')
      }),
      onPrimary: function() {
        self.set('fromDeleteHost', true);
        var allComponents = self.get('content.hostComponents');
        var deleteError = null;
        allComponents.forEach(function(component){
          if (!deleteError) {
            deleteError = self._doDeleteHostComponent(component);
          }
        });
        if (!deleteError) {
          App.ajax.send({
            name: 'host.delete',
            sender: this,
            data: {
              hostName: self.get('content.hostName')
            },
            success: 'deleteHostSuccessCallback',
            error: 'deleteHostErrorCallback'
          });

        }
        else {
          this.hide();
          deleteError.xhr.responseText = "{\"message\": \"" + deleteError.xhr.statusText + "\"}";
          App.ajax.defaultErrorHandler(deleteError.xhr, deleteError.url, deleteError.method, deleteError.xhr.status);
        }
      },
      deleteHostSuccessCallback: function(data) {
        var dialogSelf = this;
        App.router.get('updateController').updateHost(function(){
          self.loadConfigs();
          dialogSelf.hide();
          App.router.transitionTo('hosts.index');
        });
      },
      deleteHostErrorCallback: function (xhr, textStatus, errorThrown, opt) {
        console.log('Error deleting host.');
        console.log(textStatus);
        console.log(errorThrown);
        xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
        self.loadConfigs();
        this.hide();
        App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
      }
    })
  },

  restartAllStaleConfigComponents: function() {
    var self = this;
    App.showConfirmationPopup(function () {
      var staleComponents = self.get('content.componentsWithStaleConfigs');
      batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')));
    });
  },

  /**
   * open Reassign Master Wizard with selected component
   * @param event
   */
  moveComponent: function (event) {
    App.showConfirmationPopup(function() {
      var component = event.context;
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
      reassignMasterController.getSecurityStatus();
      reassignMasterController.setCurrentStep('1');
      App.router.transitionTo('reassign');
    });
  },

  /**
   * Restart clients host components to apply config changes
   */
  refreshConfigs: function(event) {
    var self = this;
    var components = event.context.filter(function(component) {
      return component.get('staleConfigs');
    });
    if (components.get('length') > 0) {
      App.showConfirmationPopup(function() {
        batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allClientsOnSelectedHost').format(self.get('content.hostName')));
      });
    }
  }

});
