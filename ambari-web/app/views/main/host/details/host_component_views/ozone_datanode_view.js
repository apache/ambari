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

App.OzoneDataNodeComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'OZONE_MANAGER',

  /**
   * Get component decommission status from server
   * @returns {$.ajax}
   */
  getOzoneDataNodeDecommissionStatus: function () {
    // Get decommission status from Ozone Manager
    var ozoneService = App.Service.find().findProperty('serviceName', 'OZONE');
    var ozoneManagerComponent = ozoneService ? ozoneService.get('hostComponents').findProperty('componentName', 'OZONE_MANAGER') : null;
    var ozoneManagerHostName = ozoneManagerComponent ? ozoneManagerComponent.get('hostName') : null;
    
    if (!ozoneManagerHostName) {
      this.set('decommissionedStatusObject', null);
      return null;
    }

    return App.ajax.send({
      name: 'host.host_component.decommission_status_ozone_datanode',
      sender: this,
      data: {
        hostName: ozoneManagerHostName,
        componentName: this.get('componentForCheckDecommission')
      },
      success: 'getOzoneDataNodeDecommissionStatusSuccessCallback',
      error: 'getOzoneDataNodeDecommissionStatusErrorCallback'
    });
  },

  /**
   * Set received value or null to <code>decommissionedStatusObject</code>
   * @param {Object} response
   * @returns {Object|null}
   */
  getOzoneDataNodeDecommissionStatusSuccessCallback: function (response) {
    var statusObject = Em.get(response, 'metrics.ozone.manager');
    if (!Em.isNone(statusObject)) {
      this.computeStatus(statusObject);
      return statusObject;
    }
    return null;
  },

  /**
   * Set null to <code>decommissionedStatusObject</code> if server returns error
   * @returns {null}
   */
  getOzoneDataNodeDecommissionStatusErrorCallback: function () {
    this.set('decommissionedStatusObject', null);
    return null;
  },

  /**
   * load Recommission/Decommission status from adminState of each live node
   */
  loadComponentDecommissionStatus: function () {
    return this.getOzoneDataNodeDecommissionStatus();
  },

  setDesiredAdminState: function (desired_admin_state) {
    this.setStatusAs(desired_admin_state);
  },

  /**
   * compute and set decommission state by ozone manager metrics
   * @param curObj
   */
  computeStatus: function (curObj) {
    var hostName = this.get('content.hostName');

    if (curObj) {
      var liveNodesJson = App.parseJSON(curObj.LiveNodes);
      // Check for Ozone DataNode status
      for (var hostPort in liveNodesJson) {
        if(hostPort.indexOf(hostName) == 0) {
          switch (liveNodesJson[hostPort].adminState) {
            case "In Service":
              this.setStatusAs('INSERVICE');
              break;
            case "Decommission In Progress":
              this.setStatusAs('DECOMMISSIONING');
              break;
            case "Decommissioned":
              this.setStatusAs('DECOMMISSIONED');
              break;
          }
          return;
        }
      }
      // if ozone manager is down, get desired_admin_state to decide if the user had issued a decommission
      this.getDesiredAdminState();
    }
  }
});
