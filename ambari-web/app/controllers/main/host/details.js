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
   * @param data Object to send
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
        callback(null);
        console.log('error on change component host status')
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

      self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(),{
        RequestInfo : {
          "context" : Em.I18n.t('requestInfo.startHostComponent') + " " + component.get('componentName').toUpperCase()
        },
        Body:{
          HostRoles:{
            state: 'STARTED'
          }
        }
      }, 'PUT',
        function(requestId){

        if(!requestId){
          return;
        }

        console.log('Send request for STARTING successfully');

        if (App.testMode) {
          component.set('workStatus', App.HostComponentStatus.starting);
          setTimeout(function(){
            component.set('workStatus', App.HostComponentStatus.started);
          },App.testModeDelayForActions);
        } else {
          App.router.get('clusterController').loadUpdatedStatusDelayed(500);
        }

        App.router.get('backgroundOperationsController').showPopup();

      });
    });
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
              "context" : Em.I18n.t('requestInfo.upgradeHostComponent') + " " + component.get('componentName').toUpperCase()
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

            App.router.get('backgroundOperationsController').showPopup();

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
      self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(),{
        RequestInfo : {
          "context" : Em.I18n.t('requestInfo.stopHostComponent')+ " " + component.get('componentName').toUpperCase()
        },
        Body:{
          HostRoles:{
            state: 'INSTALLED'
          }
        }
      }, 'PUT',
        function(requestId){
        if(!requestId){
          return;
        }

        console.log('Send request for STOPPING successfully');

        if (App.testMode) {
          component.set('workStatus', App.HostComponentStatus.stopping);
          setTimeout(function(){
            component.set('workStatus', App.HostComponentStatus.stopped);
          },App.testModeDelayForActions);
        } else {
          App.router.get('clusterController').loadUpdatedStatusDelayed(500);
        }

        App.router.get('backgroundOperationsController').showPopup();

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

    App.ModalPopup.show({
      primary: Em.I18n.t('yes'),
      secondary: Em.I18n.t('no'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile([
          '{{t hosts.delete.popup.body}}<br><br>',
          '{{t hosts.host.addComponent.note}}'
        ].join(''))
      }),
      onPrimary: function () {
        this.hide();
        self.sendCommandToServer('/hosts?Hosts/host_name=' + self.get('content.hostName'), {
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + componentName
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
                  "context": Em.I18n.t('requestInfo.installNewHostComponent') + " " + componentName
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

                App.router.get('backgroundOperationsController').showPopup();

              });
            return;
          });
      }
    });
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

    App.ModalPopup.show({
      primary: Em.I18n.t('yes'),
      secondary: Em.I18n.t('no'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile([
          '{{t hosts.delete.popup.body}}<br /><br />',
          '{{t hosts.host.addComponent.note}}'
        ].join(''))
      }),
      onPrimary: function () {
        this.hide();
        self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(), {
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + componentName
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

            App.router.get('backgroundOperationsController').showPopup();

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
      App.router.get('backgroundOperationsController').showPopup();
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
        var actionsUrl = clusterUrl + '/services/HDFS/actions/DECOMMISSION_DATANODE';
        var actionsData = {
          RequestInfo: {
            context: context},
          Body: {
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
    }
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
      App.router.get('backgroundOperationsController').showPopup();
    });
  },

  /**
   * Deletion of hosts not supported for this version
   *
   * validateDeletion: function () { var slaveComponents = [ 'DataNode',
   * 'TaskTracker', 'RegionServer' ]; var masterComponents = []; var
   * workingComponents = [];
   *
   * var components = this.get('content.components');
   * components.forEach(function (cInstance) { var cName =
   * cInstance.get('componentName'); if (slaveComponents.contains(cName)) { if
   * (cInstance.get('workStatus') === App.HostComponentStatus.stopped &&
   * !cInstance.get('decommissioned')) { workingComponents.push(cName); } } else {
   * masterComponents.push(cName); } }); // debugger; if
   * (workingComponents.length || masterComponents.length) {
   * this.raiseWarning(workingComponents, masterComponents); } else {
   * this.deleteButtonPopup(); } },
   */

  raiseWarning: function (workingComponents, masterComponents) {
    var self = this;
    var masterString = '';
    var workingString = '';
    if(masterComponents && masterComponents.length) {
      var masterList = masterComponents.join(', ');
      var ml_text = Em.I18n.t('hosts.cant.do.popup.masterList.body');
      masterString = ml_text.format(masterList);
    }
    if(workingComponents && workingComponents.length) {
      var workingList = workingComponents.join(', ');
      var wl_text = Em.I18n.t('hosts.cant.do.popup.workingList.body');
      workingString = wl_text.format(workingList);
    }
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.cant.do.popup.header'),
      html: true,
      body: masterString + workingString,
      primary: Em.I18n.t('ok'),
      secondary: null,
      onPrimary: function() {
        this.hide();
      }
    })
  },

  /**
   * show confirmation popup to delete host
   */
  deleteButtonPopup: function() {
    var self = this;
    App.showConfirmationPopup(function(){
      self.removeHost();
    });
  },

  /**
   * remove host and open hosts page
   */
  removeHost: function () {
    App.router.get('mainHostController').checkRemoved(this.get('content.id'));
    App.router.transitionTo('hosts');
  }

})