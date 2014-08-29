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

App.SliderAppsController = Ember.ArrayController.extend({

  /**
   *  Load resources on controller initialization
   * @method initResources
   */
  initResources:function () {
    this.loadComponentHost({componentName:"GANGLIA_SERVER",callback:"loadGangliaHostSuccessCallback"});
    this.loadComponentHost({componentName:"NAGIOS_SERVER",callback:"loadNagiosHostSuccessCallback"});
  }.on('init'),

  /**
   * Load ganglia server host
   * @method loadGangliaHost
   */
  loadComponentHost: function (params) {
    return App.ajax.send({
      name: 'components_hosts',
      sender: this,
      data: {
        componentName: params.componentName,
        urlPrefix: '/api/v1/'
      },
      success: params.callback
    });

  },

  /**
   * Success callback for hosts-request
   * Save host name to gangliaHost
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadGangliaHostSuccessCallback: function (data) {
    if(data.items[0]){
      App.set('gangliaHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  },

  /**
   * Success callback for hosts-request
   * Save host name to nagiosHost
   * @param {Object} data
   * @method loadGangliaHostSuccessCallback
   */
  loadNagiosHostSuccessCallback: function (data) {
    if(data.items[0]){
      App.set('nagiosHost', Em.get(data.items[0], 'Hosts.host_name'));
    }
  },
});
