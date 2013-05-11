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

App.ServiceAudit = DS.Model.extend({
  date: DS.attr('date'),
  service: DS.belongsTo('App.Service'),
  operationName: DS.attr('string'),
  user: DS.belongsTo('App.User')
});

App.ServiceAudit.FIXTURES = [
  {
    id: 1,
    date: 'September 12, 2012 17:00',
    operation_name: 'Reconfigure',
    user_id: 2,
    service_id: 1
  },
  {
    id: 2,
    date: 'September 13, 2012 17:00',
    operation_name: 'Start',
    user_id: 1,
    service_id: 1
  },
  {
    id: 3,
    date: 'September 14, 2012 17:00',
    operation_name: 'Install',
    user_id: 1,
    service_id: 1
  },
  {
    id: 4,
    date: 'September 12, 2012 17:00',
    operation_name: 'Reconfigure',
    user_id: 2,
    service_id: 2
  },
  {
    id: 5,
    date: 'September 13, 2012 17:00',
    operation_name: 'Start',
    user_id: 1,
    service_id: 2
  },
  {
    id: 6,
    date: 'September 14, 2012 17:00',
    operation_name: 'Install',
    user_id: 1,
    service_id: 2
  }
];