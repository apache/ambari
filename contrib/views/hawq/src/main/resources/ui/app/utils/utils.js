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

import ENV from 'hawq-view/config/environment';

export default {
  computeClientAddress(clientHost, clientPort) {
    return (clientPort === -1 || !clientHost) ? 'local' : `${clientHost}:${clientPort}`;
  },

  formatDuration(duration) {
    if (isNaN(duration)) {
      return "00:00:00";
    }

    let hours = Math.floor(duration / 3600);
    let durationForMinutes = duration % 3600;
    let minutes = Math.floor(durationForMinutes / 60);
    let seconds = Math.ceil(durationForMinutes % 60);

    hours = ( hours < 10 ? "0" : "") + hours;
    minutes = (minutes < 10 ? "0" : "") + minutes;
    seconds = (seconds < 10 ? "0" : "") + seconds;

    return `${hours}:${minutes}:${seconds}`;
  },

  getWindowPathname() {
    return window.location.pathname;
  },

  getNamespace() {
    let version = '1.0.0',
      instanceName = 'HAWQ',
      apiPrefix = '/api/v1/views/HAWQ/versions/',
      instancePrefix = '/instances/';

    let params = this.getWindowPathname().split('/').filter(function (param) {
      return !!param;
    });

    if (params[params.length - 3] === 'HAWQ') {
      version = params[params.length - 2];
      instanceName = params[params.length - 1];
    }

    let hawqViewInstanceURL = `${apiPrefix}${version}${instancePrefix}${instanceName}`;
    return ENV.environment === 'test' ? ENV.apiURL : hawqViewInstanceURL;
  },

  generateStatusString(waiting, waitingResource) {
    if (waitingResource) {
      return 'Queued';
    } else if (waiting) {
      return 'Waiting on Lock';
    }
    return 'Running';
  }
};