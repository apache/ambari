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

App.HostComponent = DS.Model.extend({
  workStatus: DS.attr('string'),
  componentName: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  service: DS.belongsTo('App.Service'),

  isRunning: function(){
    return (this.get('workStatus') == 'STARTED' || this.get('workStatus') == 'STARTING');
  }.property('workStatus'),
  displayName: function () { console.log('model',this.get('isLoaded'));
    return App.format.role(this.get('componentName'));
  }.property('componentName')
})

App.HostComponent.Status = {
  started: "STARTED",
  starting: "STARTING",
  stopped: "INSTALLED",
  stopping: "STOPPING",

  getKeyName:function(value){
    switch(value){
      case this.started:
        return 'started';
      case this.starting:
        return 'starting';
      case this.stopped:
        return 'installed';
      case this.stopping:
        return 'stopping';
    }
    return 'none';
  }
}

App.HostComponent.FIXTURES = [];

