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

/*jshint node:true*/
/* global moment */

import ENV from 'hawq-view/config/environment';

export default {
  formatTimeOfDay: function(date) {
    return moment(date).local().format("HH:mm:ss");
  },

  calculateTimeDelta: function(time1, time2) {
    return moment(time2).diff(moment(time1));
  },

  formatTimeDelta: function(time1, time2) {
    let response = {
      value: Math.max(0, this.calculateTimeDelta(time1, time2))
    };

    let delta =  Math.round((response.value) / 1000);
    let deltaString = `${delta % 60}s`;
    delta = Math.floor(delta / 60);

    if (delta > 0) {
      deltaString = `${delta % 60}m ${deltaString}`;
      delta = Math.floor(delta / 60);
    }

    if (delta > 0) {
      deltaString = `${delta}h ${deltaString}`;
    }

    response.text = deltaString;
    return response;
  },

  computeClientAddress: function(clientHost, clientPort) {
    if (clientPort === -1 || !clientHost) {
      return 'local';
    }
    return `${clientHost}:${clientPort}`;
  },

  getWindowPathname: function() {
    return window.location.pathname;
  },

  getNamespace: function() {
    return (ENV.environment === 'test' ? '' : this.getWindowPathname()) + ENV.apiURL;
  },

  generateStatusString: function(waiting, waitingResource) {
    if(waitingResource) {
      return 'Queued';
    } else if(waiting) {
      return 'Waiting on Lock';
    }
    return 'Running';
  }
};
