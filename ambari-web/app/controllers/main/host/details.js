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

  routeHome:function () {
    App.router.transitionTo('main.dashboard');
  },

  setBack: function(isFromHosts){
    this.set('isFromHosts', isFromHosts);
  },

  /**
   * Send specific command to server
   * @param url
   * @param data Object to send
   */
  sendCommandToServer : function(url, postData, callback){
    var url =  (App.testMode) ?
      '/data/wizard/deploy/poll_1.json' : //content is the same as ours
      '/api/clusters/' + App.router.getClusterName() + url;

    var method = App.testMode ? 'GET' : 'PUT';

    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(postData),
      dataType: 'json',
      timeout: 5000,
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

  startComponent: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;

        self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(),{
          HostRoles:{
            state: 'STARTED'
          }
        }, function(requestId){

          if(!requestId){
            return;
          }

          console.log('Send request for STARTING successfully');
          component.set('workStatus', "STARTING");

          if(App.testMode){
            setTimeout(function(){
              component.set('workStatus', "STARTED");
            },10000);
          } else{
            App.router.get('backgroundOperationsController.eventsArray').push({
              "when" : function(controller){
                var result = (controller.getOperationsForRequestId(requestId).length == 0);
                console.log('startComponent.when = ', result)
                return result;
              },
              "do" : function(){
                component.set('workStatus', "STARTED");
              }
            });
          }

          App.router.get('backgroundOperationsController').showPopup();

        });

        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  stopComponent: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;

        self.sendCommandToServer('/hosts/' + self.get('content.hostName') + '/host_components/' + component.get('componentName').toUpperCase(),{
          HostRoles:{
            state: 'INSTALLED'
          }
        }, function(requestId){
          if(!requestId){
            return
          }

          console.log('Send request for STOPPING successfully');

          component.set('workStatus', "STOPPING");

          if(App.testMode){
            setTimeout(function(){
              component.set('workStatus', "STOPPED");
            },10000);
          } else{
            App.router.get('backgroundOperationsController.eventsArray').push({
              "when" : function(controller){
                var result = (controller.getOperationsForRequestId(requestId).length == 0);
                console.log('stopComponent.when = ', result)
                return result;
              },
              "do" : function(){
                component.set('workStatus', "STOPPED");
              }
            });
          }

          App.router.get('backgroundOperationsController').showPopup();

        });

        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  decommission: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('decommissioned', true);

        //todo:call to server
        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  recommission: function(event){
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        var component = event.context;
        component.set('decommissioned', false);
        //todo: call to server
        App.router.get('backgroundOperationsController').showPopup();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },

  validateDeletion: function() {
    var slaveComponents = ['DataNode', 'TaskTracker', 'RegionServer'];
    var masterComponents = [];
    var workingComponents = [];

    var components = this.get('content.components');
    components.forEach(function(cInstance){
      var cName = cInstance.get('componentName');
      if(slaveComponents.contains(cName)) {
        if(cInstance.get('workStatus')==="STOPPED" &&
          !cInstance.get('decommissioned')){
          workingComponents.push(cName);
        }
      } else {
        masterComponents.push(cName);
      }
    });
    //debugger;
    if(workingComponents.length || masterComponents.length) {
      this.raiseWarning(workingComponents, masterComponents);
    } else {
      this.deleteButtonPopup();
    }
  },

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
      primary: "OK",
      secondary: null,
      onPrimary: function() {
        this.hide();
      }
    })
  },

  deleteButtonPopup: function() {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.header'),
      body: Em.I18n.t('hosts.delete.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        self.removeHost();
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  removeHost: function () {
    App.router.get('mainHostController').checkRemoved(this.get('content.id'));
    App.router.transitionTo('hosts');
  }

})