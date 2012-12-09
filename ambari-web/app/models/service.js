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




App.Component = DS.Model.extend({
  componentName:DS.attr('string'),
  label:DS.attr('string'),
  type:DS.attr('boolean'),
  service:DS.belongsTo('App.Service'),
  host:DS.belongsTo('App.Host'),
  workStatus:DS.attr('string'),
  isMaster:function () {
    return this.get('type');
  }.property('type'),
  isSlave:function () {
    return !this.get('type');
  }.property('type'),
  // checkedForHostFilter: true // this is for host page to set checkboxes checked
  decommissioned: DS.attr('boolean')
});

App.Component.Status = {
  started:"STARTED",
  starting:"STARTING",
  stopped:"STOPPED",
  stopping:"STOPPING"
}

App.Component.FIXTURES = [
  {
    id:1,
    component_name:'NameNode',
    label:'NN',
    type:true,
    service_id:1,
    host_id:1,
    work_status:App.Component.Status.stopped
  },
  {
    id:2,
    component_name:'SNameNode',
    label:'SNN',
    type:true,
    service_id:1,
    host_id:2,
    work_status:App.Component.Status.started
  },
  {
    id:3,
    component_name:'DataNode',
    label:'DN',
    service_id:1,
    type:false,
    host_id:2,
    work_status:App.Component.Status.started,
    decommissioned: true
  },
  {
    id:4,
    component_name:'JobTracker',
    label:'JT',
    type:true,
    service_id:2,
    host_id:4,
    work_status:App.Component.Status.started
  },
  {
    id:5,
    component_name:'TaskTracker',
    label:'TT',
    type:false,
    service_id:2,
    host_id:4,
    work_status:App.Component.Status.started
  },
  {
    id:6,
    component_name:'HBase Master',
    label:'HBM',
    type:true,
    service_id:3,
    host_id:4,
    work_status:App.Component.Status.started
  },
  {
    id:7,
    component_name:'Region Server',
    label:'RS',
    type:false,
    service_id:3,
    host_id:2,
    work_status:App.Component.Status.started
  },
  {
    id:8,
    component_name:'Oozie',
    label:'Oz',
    type:false,
    service_id:5,
    host_id:2,
    work_status:App.Component.Status.started
  }
];
/*
App.Service = DS.Model.extend({
  serviceName:DS.attr('string'),
  label:DS.attr('string'),
  components:DS.hasMany('App.Component'),
  serviceAudit:DS.hasMany('App.ServiceAudit'),
  healthStatus:DS.attr('string'),
  workStatus:DS.attr('boolean'),
  alerts:DS.hasMany('App.Alert'),
  quickLinks:DS.hasMany('App.QuickLinks')
});

App.Service.Health = {
  live:"LIVE",
  dead:"DEAD",
  start:"STARTING",
  stop:"STOPPING"
}

*/