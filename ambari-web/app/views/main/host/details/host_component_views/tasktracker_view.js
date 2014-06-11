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

App.TaskTrackerComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'JOBTRACKER',

  /**
   * load Recommission/Decommission status for TaskTracker from JobTracker/AliveNodes list
   */
  loadComponentDecommissionStatus: function () {
    var hostName = this.get('content.hostName');
    var dfd = $.Deferred();
    var self = this;
    this.getDesiredAdminState().done( function () {
      var desired_admin_state = self.get('desiredAdminState');
      self.set('desiredAdminState', null);
      switch(desired_admin_state) {
        case "INSERVICE":
          // can be decommissioned if already started
          self.set('isComponentRecommissionAvailable', false);
          self.set('isComponentDecommissioning', false);
          self.set('isComponentDecommissionAvailable', self.get('isStart'));
          break;
        case "DECOMMISSIONED":
          var deferred = $.Deferred();
          self.getDecommissionStatus().done( function() {
            var curObj = self.get('decommissionedStatusObject');
            self.set('decommissionedStatusObject', null);
            if (curObj) {
              var aliveNodesArray = App.parseJSON(curObj.AliveNodes);
              if (aliveNodesArray != null) {
                if (aliveNodesArray.findProperty('hostname', hostName)){
                  //decommissioning ..
                  self.set('isComponentRecommissionAvailable', true);
                  self.set('isComponentDecommissioning', true);
                  self.set('isComponentDecommissionAvailable', false);
                } else {
                  //decommissioned
                  self.set('isComponentRecommissionAvailable', true);
                  self.set('isComponentDecommissioning', false);
                  self.set('isComponentDecommissionAvailable', false);
                }
              }

            }
            deferred.resolve(curObj);
          });
          break;
      }
      dfd.resolve(desired_admin_state);
    });
    return dfd.promise();
  }

});
