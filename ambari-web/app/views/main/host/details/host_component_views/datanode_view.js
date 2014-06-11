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

App.DataNodeComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'NAMENODE',

  /**
   * Get component decommission status from server
   * @returns {$.ajax}
   */
  getDNDecommissionStatus: function() {
    // always get datanode decommission statue from active namenode (if NN HA enabled)
    var hdfs = App.HDFSService.find().objectAt(0);
    var activeNNHostName = (!hdfs.get('snameNode') && hdfs.get('activeNameNode')) ? hdfs.get('activeNameNode.hostName'): hdfs.get('nameNode.hostName');
    return App.ajax.send({
      name: 'host.host_component.decommission_status_datanode',
      sender: this,
      data: {
        hostName: activeNNHostName,
        componentName: this.get('componentForCheckDecommission')
      },
      success: 'getDNDecommissionStatusSuccessCallback',
      error: 'getDNDecommissionStatusErrorCallback'
    });
  },

  /**
   * Set received value or null to <code>decommissionedStatusObject</code>
   * @param {Object} response
   * @returns {Object|null}
   */
  getDNDecommissionStatusSuccessCallback: function (response) {
    var statusObject = Em.get(response, 'metrics.dfs.namenode');
    if ( statusObject != null) {
      this.set('decommissionedStatusObject', statusObject);
      return statusObject;
    }
    return null;
  },

  /**
   * Set null to <code>decommissionedStatusObject</code> if server returns error
   * @returns {null}
   */
  getDNDecommissionStatusErrorCallback: function () {
    this.set('decommissionedStatusObject', null);
    return null;
  },

  /**
   * load Recommission/Decommission status from adminState of each live node
   */
  loadComponentDecommissionStatus: function () {
    var hostName = this.get('content.hostName');
    var dfd = $.Deferred();
    var self = this;
    this.getDNDecommissionStatus().done(function () {
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
