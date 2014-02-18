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
require('views/main/host/details/host_component_views/decommissionable');

App.DataNodeComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'NAMENODE',

  /**
   * load Recommission/Decommission status from adminState of each live node
   */
  loadComponentDecommissionStatus: function () {
    var hostName = this.get('content.host.hostName');
    var dfd = $.Deferred();
    var self = this;
    this.getDecommissionStatus().done(function () {
      var curObj = self.get('decommissionedStatusObject');
      self.set('decommissionedStatusObject', null);
      // HDP-2 stack
      if (App.get('isHadoop2Stack')) {
        if (curObj) {
          var liveNodesJson = App.parseJSON(curObj.LiveNodes);
          if (liveNodesJson && liveNodesJson[hostName] ) {
            switch(liveNodesJson[hostName].adminState) {
              case "In Service":
                self.set('isComponentRecommissionAvailable', false);
                self.set('isComponentDecommissioning', false);
                self.set('isComponentDecommissionAvailable', self.get('isStart'));
                break;
              case "Decommission In Progress":
                self.set('isComponentRecommissionAvailable', true);
                self.set('isComponentDecommissioning', true);
                self.set('isComponentDecommissionAvailable', false);
                break;
              case "Decommissioned":
                self.set('isComponentRecommissionAvailable', true);
                self.set('isComponentDecommissioning', false);
                self.set('isComponentDecommissionAvailable', false);
                break;
            }
          } else {
            // if namenode is down, get desired_admin_state to decide if the user had issued a decommission
            var deferred = $.Deferred();
            self.getDesiredAdminState().done(function () {
              var desired_admin_state = self.get('desiredAdminState');
              self.set('desiredAdminState', null);
              switch(desired_admin_state) {
                case "INSERVICE":
                  self.set('isComponentRecommissionAvailable', false);
                  self.set('isComponentDecommissioning', false);
                  self.set('isComponentDecommissionAvailable', self.get('isStart'));
                  break;
                case "DECOMMISSIONED":
                  self.set('isComponentRecommissionAvailable', true);
                  self.set('isComponentDecommissioning', false);
                  self.set('isComponentDecommissionAvailable', false);
                  break;
              }
              deferred.resolve(desired_admin_state);
            });
          }
        }
      }
      else {
        if (curObj) {
          var liveNodesJson = App.parseJSON(curObj.LiveNodes);
          var decomNodesJson = App.parseJSON(curObj.DecomNodes);
          var deadNodesJson = App.parseJSON(curObj.DeadNodes);
          if (decomNodesJson && decomNodesJson[hostName] ) {
            self.set('isComponentRecommissionAvailable', true);
            self.set('isComponentDecommissioning', true);
            self.set('isComponentDecommissionAvailable', false);
          } else if (deadNodesJson && deadNodesJson[hostName] ) {
            self.set('isComponentRecommissionAvailable', true);
            self.set('isComponentDecommissioning', false);
            self.set('isComponentDecommissionAvailable', false);
          } else if (liveNodesJson && liveNodesJson[hostName] ) {
            self.set('isComponentRecommissionAvailable', false);
            self.set('isComponentDecommissioning', false);
            self.set('isComponentDecommissionAvailable', self.get('isStart'));
          } else {
            // if namenode is down, get desired_admin_state to decide if the user had issued a decommission
            var deferred = $.Deferred();
            self.getDesiredAdminState().done( function () {
              var desired_admin_state = self.get('desiredAdminState');
              self.set('desiredAdminState', null);
              switch(desired_admin_state) {
                case "INSERVICE":
                  self.set('isComponentRecommissionAvailable', false);
                  self.set('isComponentDecommissioning', false);
                  self.set('isComponentDecommissionAvailable', self.get('isStart'));
                  break;
                case "DECOMMISSIONED":
                  self.set('isComponentRecommissionAvailable', true);
                  self.set('isComponentDecommissioning', false);
                  self.set('isComponentDecommissionAvailable', false);
                  break;
              }
              deferred.resolve(desired_admin_state);
            });
          }
        }
      }
      dfd.resolve(curObj);
    });
    return dfd.promise();
  }


});
