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
  
  bootStatusForDisplay: Em.computed.getByKey('bootStatusForDisplayMap', 'bootStatus', 'Registering'),

  bootStatusForDisplayMap: {
    PENDING: 'Preparing',
    REGISTERED: 'Success',
    FAILED: 'Failed',
    RUNNING: 'Installing',
    DONE: 'Registering',
    REGISTERING: 'Registering'
  },

  bootBarColor: Em.computed.getByKey('bootBarColorMap', 'bootStatus', 'progress-info'),

  bootBarColorMap: {
    REGISTERED: 'progress-success',
    FAILED: 'progress-danger',
    PENDING: 'progress-info',
    RUNNING: 'progress-info',
    DONE: 'progress-info',
    REGISTERING: 'progress-info'
  },

  bootStatusColor:Em.computed.getByKey('bootStatusColorMap', 'bootStatus', 'text-info'),

  bootStatusColorMap: {
    REGISTERED: 'text-success',
    FAILED: 'text-error',
    PENDING: 'text-info',
    RUNNING: 'text-info',
    DONE: 'text-info',
    REGISTERING: 'text-info'
  },

  isBootDone: Em.computed.existsIn('bootStatus', ['REGISTERED', 'FAILED'])

});
