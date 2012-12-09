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

App.HostInfo = Ember.Object.extend({
  elementId: 'host',
  name: '',
  cpu: null,
  memory: null,
  message: 'Information',
  barColor: 'progress-info',
  isChecked: true,
  bootLog:null,
  bootStatus:'RUNNING',
  
  bootStatusForDisplay:function () {
    switch (this.get('bootStatus')) {
      case 'REGISTERED':
        return 'Success';
      case 'FAILED':
        return 'Failed';
      case 'RUNNING':
        return 'Installing';
      case 'DONE':
      case 'REGISTERING':
      default:
        return 'Registering';
    }
  }.property('bootStatus'),

  bootBarColor:function () {
    switch (this.get('bootStatus')) {
      case 'REGISTERED':
        return 'progress-success';
      case 'FAILED':
        return 'progress-danger';
      case 'RUNNING':
      case 'DONE':
      case 'REGISTERING':
      default:
        return 'progress-info';
    }
  }.property('bootStatus'),

  bootStatusColor:function () {
    switch (this.get('bootStatus')) {
      case 'REGISTERED':
        return 'text-success';
      case 'FAILED':
        return 'text-error';
      case 'RUNNING':
      case 'DONE':
      case 'REGISTERING':
      default:
        return 'text-info';
    }
  }.property('bootStatus'),

  isBootDone:function () {
    switch (this.get('bootStatus')) {
      case 'REGISTERED':
      case 'FAILED':
        return true;
      case 'RUNNING':
      case 'DONE':
      case 'REGISTERING':
      default:
        return false;
    }

  }.property('bootStatus')
  /* horizonData:
   [{"cpu":"61","date":"2012-09-05T11:03:00+03:00","id":"0","memory":"44","name":"Node-0.0","network":"36","status":"LIVE","rackName":"Rack-0","io":"23"},{"cpu":"1","date":"2012-09-05T11:06:00+03:00","id":"0","memory":"46","name":"Node-0.0","network":"54","status":"LIVE","rackName":"Rack-0","io":"96"},{"cpu":"36","date":"2012-09-05T11:09:00+03:00","id":"0","memory":"65","name":"Node-0.0","network":"42","status":"LIVE","rackName":"Rack-0","io":"49"},{"cpu":"50","date":"2012-09-05T11:12:00+03:00","id":"0","memory":"46","name":"Node-0.0","network":"76","status":"LIVE","rackName":"Rack-0","io":"67"},{"cpu":"79","date":"2012-09-05T11:15:00+03:00","id":"0","memory":"91","name":"Node-0.0","network":"14","status":"LIVE","rackName":"Rack-0","io":"47"},{"cpu":"65","date":"2012-09-05T11:18:00+03:00","id":"0","memory":"44","name":"Node-0.0","network":"90","status":"LIVE","rackName":"Rack-0","io":"74"},{"cpu":"42","date":"2012-09-05T11:21:00+03:00","id":"0","memory":"96","name":"Node-0.0","network":"29","status":"LIVE","rackName":"Rack-0","io":"6"},{"cpu":"76","date":"2012-09-05T11:24:00+03:00","id":"0","memory":"28","name":"Node-0.0","network":"52","status":"LIVE","rackName":"Rack-0","io":"1"},{"cpu":"24","date":"2012-09-05T11:27:00+03:00","id":"0","memory":"66","name":"Node-0.0","network":"75","status":"LIVE","rackName":"Rack-0","io":"31"},{"cpu":"71","date":"2012-09-05T11:30:00+03:00","id":"0","memory":"8","name":"Node-0.0","network":"1","status":"LIVE","rackName":"Rack-0","io":"36"},{"cpu":"3","date":"2012-09-05T11:33:00+03:00","id":"0","memory":"57","name":"Node-0.0","network":"12","status":"LIVE","rackName":"Rack-0","io":"71"},{"cpu":"85","date":"2012-09-05T11:36:00+03:00","id":"0","memory":"44","name":"Node-0.0","network":"76","status":"LIVE","rackName":"Rack-0","io":"54"},{"cpu":"49","date":"2012-09-05T11:39:00+03:00","id":"0","memory":"8","name":"Node-0.0","network":"28","status":"LIVE","rackName":"Rack-0","io":"60"},{"cpu":"46","date":"2012-09-05T11:42:00+03:00","id":"0","memory":"75","name":"Node-0.0","network":"7","status":"LIVE","rackName":"Rack-0","io":"86"},{"cpu":"61","date":"2012-09-05T11:45:00+03:00","id":"0","memory":"11","name":"Node-0.0","network":"65","status":"LIVE","rackName":"Rack-0","io":"65"},{"cpu":"5","date":"2012-09-05T11:48:00+03:00","id":"0","memory":"24","name":"Node-0.0","network":"14","status":"LIVE","rackName":"Rack-0","io":"62"},{"cpu":"30","date":"2012-09-05T11:51:00+03:00","id":"0","memory":"40","name":"Node-0.0","network":"52","status":"LIVE","rackName":"Rack-0","io":"98"},{"cpu":"5","date":"2012-09-05T11:54:00+03:00","id":"0","memory":"38","name":"Node-0.0","network":"17","status":"LIVE","rackName":"Rack-0","io":"27"},{"cpu":"22","date":"2012-09-05T11:57:00+03:00","id":"0","memory":"80","name":"Node-0.0","network":"62","status":"LIVE","rackName":"Rack-0","io":"38"}] */
});
