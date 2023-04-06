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

App.BackgroundOperation = DS.Model.extend({
  operationName:DS.attr('string'),
  events: DS.hasMany('App.BackgroundOperationEvent'),
  cluster:DS.belongsTo('App.Cluster'),
  host:DS.belongsTo('App.Host'),
  operationLog: DS.attr('string')
});

App.BackgroundOperation.FIXTURES = [
  {
    id:1,
    operation_name:'Decommissioning host1',
    operation_log:'Decommissioning log',
    events:[1,2],
    cluster_id:1,
    host_id:1
  },
  {
    id:2,
    operation_name:'Starting DataNode on host4',
    operation_log:'Starting DataNode log',
    events:[3],
    cluster_id:1,
    host_id:1
  }
];

App.BackgroundOperationEvent = DS.Model.extend({
  eventName:DS.attr('string'),
  operation:DS.belongsTo('App.BackgroundOperation'),
  eventDate: DS.attr('string')
});

App.BackgroundOperationEvent.FIXTURES = [
  {
    id:1,
    event_name:'Some intermediate operation',
    operation_id:1,
    event_date:'4 min ago'
  },
  {
    id:2,
    event_name:'Operation started',
    operation_id:1,
    event_date:'5 min ago'
  },
  {
    id:3,
    event_name:'Operation started',
    operation_id:2,
    event_date:'5 min ago'
  }
];

