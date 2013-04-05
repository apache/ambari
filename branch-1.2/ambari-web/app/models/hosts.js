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
  bootStatus: 'PENDING',
  
  bootStatusForDisplay:function () {
    switch (this.get('bootStatus')) {
      case 'PENDING':
        return 'Preparing';
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
      case 'PENDING':
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
      case 'PENDING':
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
      case 'PENDING':
      case 'RUNNING':
      case 'DONE':
      case 'REGISTERING':
      default:
        return false;
    }

  }.property('bootStatus')
});
