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

App.ServiceInfo = Ember.Object.extend({
  elementId:'service',
  serviceName:'',
  displayName:'',
  isMaster:'',
  isClient:'',
  isDisabled:'',
  isHidden:'',
  isSelected:'true',
  description:''
});

/*App.User = Em.Object.extend({
 username: null
 });*/

App.ServiceModel = Em.Object.extend({
  name:null,
  components:[]
});

// uncomment if column names are camelized in JSON (or fixture), rather than _ separated
/*
 DS.Model.reopen({
 namingConvention: {
 keyToJSONKey: function(key) {
 return key;
 },

 foreignKey: function(key) {
 return key;
 }
 }
 });
 */


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

App.Service.FIXTURES = [
  {
    id:1,
    service_name:'hdfs',
    label:'HDFS',
    components:[1, 2, 3],
    service_audit:[1, 2, 3],
    health_status:App.Service.Health.live,
    work_status:true,
    alerts:[1, 2],
    quick_links:[1, 2, 3, 4]
  },
  {
    id:2,
    service_name:'mapreduce',
    label:'MapReduce',
    components:[4, 5],
    service_audit:[4, 5, 6],
    health_status:App.Service.Health.start,
    work_status:true,
    alerts:[3, 4],
    quick_links:[5, 6, 7, 8, 9, 10, 17, 18]
  },
  {
    id:3,
    service_name:'hbase',
    label:'HBase',
    components:[6, 7],
    health_status:App.Service.Health.dead,
    work_status:false,
    alerts:[5, 6],
    quick_links:[11, 12, 13, 14, 15, 16]
  },
  {
    id:4,
    service_name:'zookeeper',
    label:'Zookeeper',
    health_status:App.Service.Health.stop,
    work_status:false,
    alerts:[7, 8]
  },
  {
    id:5,
    service_name:'oozie',
    label:'Oozie',
    health_status:App.Service.Health.dead,
    work_status:false,
    alerts:[9, 10]
  },
  {
    id:6,
    service_name:'hive',
    label:'Hive',
    health_status:App.Service.Health.dead,
    work_status:false,
    alerts:[11, 12]
  }
];

App.QuickLinks = DS.Model.extend({
  label:DS.attr('string'),
  url:DS.attr('string')
});

App.QuickLinks.FIXTURES = [
  {
    id:1,
    label:'NameNode UI',
    url:'http://%@:50070/dfshealth.jsp'
  },
  {
    id:2,
    label:'NameNode logs',
    url:'http://%@:50070/logs'
  },
  {
    id:3,
    label:'NameNode JMX',
    url:'http://%@:50070/jmx'
  },
  {
    id:4,
    label:'Thread Stacks',
    url:'http://%@:50070/stacks'
  },
  {
    id:5,
    label:'JobTracker UI',
    url:'http://%@:50030/jobtracker.jsp'
  },
  {
    id:6,
    label:'Scheduling Info',
    url:'http://%@:50030/scheduler'
  },
  {
    id:7,
    label:'Running Jobs',
    url:'http://%@:50030/jobtracker.jsp#running_jobs'
  },
  {
    id:8,
    label:'Retired Jobs',
    url:'http://%@:50030/jobtracker.jsp#retired_jobs'
  },
  {
    id:9,
    label:'JobHistory Server',
    url:'http://%@:51111/jobhistoryhome.jsp'
  },
  {
    id:10,
    label:'JobTracker Logs',
    url:'http://%@:50030/logs'
  },
  {
    id:11,
    label:'HBase Master UI',
    url:'http://%@:60010/master-status'
  },
  {
    id:12,
    label:'HBase Logs',
    url:'http://%@:60010/logs'
  },
  {
    id:13,
    label:'Zookeeper Info',
    url:'http://%@:60010/zk.jsp'
  },
  {
    id:14,
    label:'HBase Master JMX',
    url:'http://%@:60010/jmx'
  },
  {
    id:15,
    label:'Debug Dump',
    url:'http://%@:60010/dump'
  },
  {
    id:16,
    label:'Thread Stacks',
    url:'http://%@:60010/stacks'
  },
  {
    id:17,
    label:'JobTracker JMX',
    url:'http://%@:50030/jmx'
  },
  {
    id:18,
    label:'Thread Stacks',
    url:'http://%@:50030/stacks'
  }
];
