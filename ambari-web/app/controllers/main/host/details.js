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

App.MainHostDetailsController = Em.Controller.extend({
  name: 'mainHostDetailsController',
  content: null,
  isFromHosts: false,

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

  /**
   * set new value to isFromHosts property
   * @param isFromHosts new value
   */
  setBack: function(isFromHosts){
    this.set('isFromHosts', isFromHosts);
  },

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
  sendStartComponentCommand: function(component, context) {
    var url = component !== null ? 
        '/hosts/' + this.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase() : 
        '/hosts/' + this.get('content.hostName') + '/host_components';
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
    if (component === null) {
      var allComponents = this.get('content.hostComponents');
      var startable = [];
      allComponents.forEach(function (c) {
        if (c.get('isMaster') || c.get('isSlave')) {
          startable.push(c.get('componentName'));
        }
      });
      dataToSend.RequestInfo.query = "HostRoles/component_name.in(" + startable.join(',') + ")";
    }
    this.sendCommandToServer(url, dataToSend, 'PUT',
      function(requestId){

      if(!requestId){
        return;
      }

      console.log('Send request for STARTING successfully');

      if (App.testMode) {
        if(component === null){
          var allComponents = this.get('content.hostComponents');
          allComponents.forEach(function(component){
            component.set('workStatus', App.HostComponentStatus.stopping);
            setTimeout(function(){
              component.set('workStatus', App.HostComponentStatus.stopped);
            },App.testModeDelayForActions);
          });
        } else {
          component.set('workStatus', App.HostComponentStatus.starting);
          setTimeout(function(){
            component.set('workStatus', App.HostComponentStatus.started);
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
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/host/details/deleteComponentPopup')
      }),
      enablePrimary: false,
      lastComponent: function() {
        if (isLastComponent) {
          this.set('enablePrimary',false);
          return true;
        } else {
          this.set('enablePrimary',true);
          return false;
        }
      }.property(),
      lastComponentError:  Em.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.warning').format(displayName))
      }),
      deleteComponentMsg: function() {
        return Em.I18n.t('hosts.host.deleteComponent.popup.msg').format(displayName);
      }.property(),
      onPrimary: function () {
        if (!this.get('enablePrimary')) return;
        self._doDeleteHostComponent(component);
        this.hide();
      }
    });

  },
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
  sendStopComponentCommand: function(component, context){
    var url = component !== null ? 
        '/hosts/' + this.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase() : 
        '/hosts/' + this.get('content.hostName') + '/host_components';
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
    if (component === null) {
      var allComponents = this.get('content.hostComponents');
      var startable = [];
      allComponents.forEach(function (c) {
        if (c.get('isMaster') || c.get('isSlave')) {
          startable.push(c.get('componentName'));
        }
      });
      dataToSend.RequestInfo.query = "HostRoles/component_name.in(" + startable.join(',') + ")";
    }
    this.sendCommandToServer( url, dataToSend, 'PUT',
      function(requestId){
      if(!requestId){
        return;
      }

      console.log('Send request for STOPPING successfully');

      if (App.testMode) {
        if(component === null){
          var allComponents = this.get('content.hostComponents');
          allComponents.forEach(function(component){
            component.set('workStatus', App.HostComponentStatus.stopping);
            setTimeout(function(){
              component.set('workStatus', App.HostComponentStatus.stopped);
            },App.testModeDelayForActions);
          });
        } else {
          component.set('workStatus', App.HostComponentStatus.stopping);
          setTimeout(function(){
            component.set('workStatus', App.HostComponentStatus.stopped);
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
   * send command to server to install selected host component
   * @param event
   */
  addComponent: function (event, context) {
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
          primary: Em.I18n.t('yes'),
          secondary: Em.I18n.t('no'),
          header: Em.I18n.t('popup.confirmation.commonHeader'),
          addComponentMsg: function() {
            return Em.I18n.t('hosts.host.addComponent.msg').format(dn);
          }.property(),
          bodyClass: Ember.View.extend({
            templateName: require('templates/main/host/details/addComponentPopup')
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
            });
            if (componentName === 'ZOOKEEPER_SERVER') {
              self.checkZkConfigs();
            }
          });
      });
  },
  /**
   * Load tags
   */
  checkZkConfigs: function() {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'checkZkConfigsSuccessCallback'
    });
  },
  /**
   * Load needed configs
   * @param data
   */
  checkZkConfigsSuccessCallback: function(data) {
    var urlParams = [];
    urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
    if (App.Service.find().someProperty('serviceName', 'HBASE')) {
      urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
    }
    if (App.Service.find().someProperty('serviceName', 'HIVE')) {
      urlParams.push('(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')');
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
   * Set new values for some configs
   * @param data
   */
  setNewZkConfigs: function(data) {
    var configs = [];
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
    }, this);
    if (App.isHadoop2Stack) {
      if (!App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE')) {
        var zks = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('host.hostName');
        var value = '';
        zks.forEach(function(zk) {
          value += zk + ':2181,';
        });
        value.slice(0,-1);
        configs['core-site']['ha.zookeeper.quorum'] = value;
      }
    }
    var oneZk = App.HostComponent.find().findProperty('componentName', 'ZOOKEEPER_SERVER').get('host.hostName');
    if (configs['hbase-site']) {
      configs['hbase-site']['hbase.zookeeper.quorum'] = oneZk;
    }
    if (configs['webhcat-site']) {
      configs['webhcat-site']['templeton.zookeeper.hosts'] = oneZk;
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
      primary: Em.I18n.t('yes'),
      secondary: Em.I18n.t('no'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      installComponentMessage: function(){
        return Em.I18n.t('hosts.host.installComponent.msg').format(displayName);
      }.property(),
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
   * send command to server to run decommission on DATANODE
   * @param event
   */
  decommission: function(event){
    var self = this;
    var decommissionHostNames = this.get('view.decommissionDataNodeHostNames');
    if (decommissionHostNames == null) {
      decommissionHostNames = [];
    }
    App.showConfirmationPopup(function(){
      var component = event.context;
      // Only HDFS service as of now
      var svcName = component.get('service.serviceName');
      if (svcName === "HDFS") {
        var hostName = self.get('content.hostName');
        var index = decommissionHostNames.indexOf(hostName);
        if (index < 0) {
          decommissionHostNames.push(hostName);
        }
        self.doDatanodeDecommission(decommissionHostNames, true);
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
   * Performs either Decommission or Recommission by updating the hosts list on
   * server.
   * @param decommission defines context for request (true for decommission and false for recommission)
   */
  doDatanodeDecommission: function(decommissionHostNames, decommission){
    var self = this;
    if (decommissionHostNames == null) {
      decommissionHostNames = [];
    }
    var invocationTag = String(new Date().getTime());
    var context = decommission ? Em.I18n.t('hosts.host.datanode.decommission') : Em.I18n.t('hosts.host.datanode.recommission');
    var clusterName = App.router.get('clusterController.clusterName');
    var clusterUrl = App.apiPrefix + '/clusters/' + clusterName;
    var configsUrl = clusterUrl + '/configurations';
    var configsData = {
      type: "hdfs-exclude-file",
      tag: invocationTag,
      properties: {
        datanodes: decommissionHostNames.join(',')
      }
    };
    var configsAjax = {
      type: 'POST',
      url: configsUrl,
      dataType: 'json',
      data: JSON.stringify(configsData),
      timeout: App.timeout,
      success: function(){
        var actionsUrl = clusterUrl + '/requests';
        var actionsData = {
          RequestInfo: {
            context: context,
            command: 'DECOMMISSION_DATANODE',
            service_name: 'HDFS',
            parameters: {
              excludeFileTag: invocationTag
            }
          }
        };
        var actionsAjax = {
          type: 'POST',
          url: actionsUrl,
          dataType: 'json',
          data: JSON.stringify(actionsData),
          timeout: App.timeout,
          success: function(){
            var persistUrl = App.apiPrefix + '/persist';
            var persistData = {
              "decommissionDataNodesTag": invocationTag
            };
            var persistPutAjax = {
              type: 'POST',
              url: persistUrl,
              dataType: 'json',
              data: JSON.stringify(persistData),
              timeout: App.timeout,
              success: function(){
                var view = self.get('view');
                view.loadDecommissionNodesList();
              }
            };
            jQuery.ajax(persistPutAjax);
          },
          error: function(xhr, textStatus, errorThrown){
            console.log(textStatus);
            console.log(errorThrown);
          }
        };
        jQuery.ajax(actionsAjax);
      },
      error: function(xhr, textStatus, errorThrown){
        console.log(textStatus);
        console.log(errorThrown);
      }
    };
    jQuery.ajax(configsAjax);
  },

  /**
   * send command to server to run recommission on DATANODE
   * @param event
   */
  recommission: function(event){
    var self = this;
    var decommissionHostNames = this.get('view.decommissionDataNodeHostNames');
    if (decommissionHostNames == null) {
      decommissionHostNames = [];
    }
    App.showConfirmationPopup(function(){
      var component = event.context;
      // Only HDFS service as of now
      var svcName = component.get('service.serviceName');
      if (svcName === "HDFS") {
        var hostName = self.get('content.hostName');
        var index = decommissionHostNames.indexOf(hostName);
        decommissionHostNames.splice(index, 1);
        self.doDatanodeDecommission(decommissionHostNames, false);
      }
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },
  
  doAction: function(option) {
    switch (option.context.action) {
      case "deleteHost":
        this.validateAndDeleteHost();
        break;
      case "startAllComponents":
        this.doStartAllComponents();
        break;
      case "stopAllComponents":
        this.doStopAllComponents();
        break;
      default:
        break;
    }
  },
  
  doStartAllComponents: function() {
    var self = this;
    var components = this.get('content.hostComponents');
    var componentsLength = components == null ? 0 : components.get('length');
    if (componentsLength > 0) {
      App.showConfirmationPopup(function() {
        self.sendStartComponentCommand(null, 
            Em.I18n.t('hosts.host.maintainance.startAllComponents.context'));
      });
    }
  },
  
  doStopAllComponents: function() {
    var self = this;
    var components = this.get('content.hostComponents');
    var componentsLength = components == null ? 0 : components.get('length');
    if (componentsLength > 0) {
      App.showConfirmationPopup(function() {
        self.sendStopComponentCommand(null, 
            Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'));
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
           this.set('enablePrimary',false);
           return true;
         } else {
           this.set('enablePrimary',true);
           return false;
         }
      }.property(),
      enablePrimary: false,
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
        if (!this.get('enablePrimary')) return;
        var dialogSelf = this;
        var allComponents = self.get('content.hostComponents');
        var deleteError = null;
        allComponents.forEach(function(component){
          if (!deleteError) {
            deleteError = self._doDeleteHostComponent(component);
          }
        });
        if (!deleteError) {
          var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/hosts/' + self.get('content.hostName');
          $.ajax({
            type: 'DELETE',
            url: url,
            timeout: App.timeout,
            async: false,
            success: function (data) {
              dialogSelf.hide();
              App.router.get('updateController').updateAll();
              App.router.transitionTo('hosts.index');
            },
            error: function (xhr, textStatus, errorThrown) {
              console.log('Error deleting host component');
              console.log(textStatus);
              console.log(errorThrown);
              dialogSelf.hide();
              xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
              App.ajax.defaultErrorHandler(xhr, url, 'DELETE', xhr.status);
            },
            statusCode: require('data/statusCodes')
          });
        } else {
          dialogSelf.hide();
          deleteError.xhr.responseText = "{\"message\": \"" + deleteError.xhr.statusText + "\"}";
          App.ajax.defaultErrorHandler(deleteError.xhr, deleteError.url, deleteError.method, deleteError.xhr.status);
        }
      },
      onSecondary: function() {
        this.hide();
      }
    })
  },

  restartComponents: function(e) {
    var staleComponents = this.get('content.hostComponents').filterProperty('staleConfigs', true);
    var commandName = "stop_component";
    if(e.context) {
      if(!staleComponents.findProperty('workStatus','STARTED')){
        return;
      }
    } else {
      commandName = "start_component";
      if(!staleComponents.findProperty('workStatus','INSTALLED')){
        return;
      }
    };
    var content = this;
    return App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      body: Em.I18n.t('question.sure'),
      content: content,
      onPrimary: function () {
        var hostComponents = this.content.get('content.hostComponents').filterProperty('staleConfigs', true);
        hostComponents.forEach(function(item){
          if (item.get('isClient') && commandName === 'start_component') {
            return false;
          }
          var componentName = item.get('componentName');
          var hostName = item.get('host.hostName');
          App.ajax.send({
            name: 'config.stale.'+commandName,
            sender: this,
            data: {
              hostName: hostName,
              componentName: componentName,
              displayName: App.format.role(componentName)
            }
          });
        })
        this.hide();
        App.router.get('backgroundOperationsController').showPopup();
      },
      onSecondary: function () {
        this.hide();
      }
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
      App.router.transitionTo('services.reassign');
    });
  }

});
