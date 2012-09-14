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

App.Alert = DS.Model.extend({
  title: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  date: DS.attr('date'),
  status: DS.attr('string'),
  message: DS.attr('string')
});

App.Alert.FIXTURES = [
  {
    id: 1,
    title: 'Corrupt/Missing Block',
    service_id: 1,
    date: 'August 29, 2012 17:00',
    status: 'all bad',
    message: 'message'
  },

  {
    id: 2,
    title: 'Corrupt/Missing Block',
    service_id: 2,
    date: 'August 29, 2012 17:00',
    status: 'all bad',
    message: 'message'
  }
];