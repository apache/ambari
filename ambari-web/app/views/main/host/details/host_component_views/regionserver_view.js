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

App.RegionServerComponentView = App.HostComponentView.extend(App.Decommissionable, {

  componentForCheckDecommission: 'HBASE_MASTER',
  /**
   * load Recommission/Decommission status of RegionServer
   */
  loadComponentDecommissionStatus: function () {
    var hostName = this.get('content.hostName');
    var slaveType = 'HBASE_REGIONSERVER';
    var self = this;
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
          self.set('isComponentDecommissioning', self.get('isStart'));
          self.set('isComponentDecommissionAvailable', false);
          break;
      }
      deferred.resolve(desired_admin_state);
    });
    return deferred.promise();
  }

});
