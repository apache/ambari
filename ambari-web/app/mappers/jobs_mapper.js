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
var fillEmptyValues = function(self, obj) {
  $.each(self.config, function(field, value) {
    if (obj[value].length == 0) {
      obj[value].push({x: 0, y: 0});
    };
  });
};

App.jobsMapper = App.QuickDataMapper.create({
  model:App.Job,
  map:function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json.jobs) {
      var result = [];
      json.jobs.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
      }, this);

      var r = Ember.ArrayProxy.create({"content":[]});
      result.forEach(function(item){
        r.content.push(App.Job2.create(item));
      });

      this.set('controller.content.jobs', r.content);
    }
  },
  config:{
    id:'jobId',
    run_id:'workflowId',
    job_name:'jobName',
    workflow_entity_name:'workflowEntityName',
    user_name:'userName',
    submit_time:'submitTime',
    maps:'maps',
    reduces:'reduces',
    status:'status',
    input:'inputBytes',
    output:'outputBytes',
    elapsed_time:'elapsedTime'
  }
});

App.jobTimeLineMapper = App.QuickDataMapper.create({
  model: null, //model will be set outside of mapper
  config:{
    map:'map',
    shuffle:'shuffle',
    reduce:'reduce'
  },
  map:function (json) {
    var job = this.get('model'); // @model App.MainAppsItemBarView
    var parseResult = this.parseIt(json, this.config);
    var self = this;
    $.each(parseResult, function (field, value) {
      var d = self.coordinatesModify(value);
      d.reverse();
      d = self.coordinatesModify(d);
      d.reverse();
      job.set(field, d);
    });
    fillEmptyValues(this, job);
  },

  coordinatesModify: function(data) {
    var d = this.zeroAdding(data);
    d.reverse();
    d = this.zeroAdding(d);
    d.reverse();
    return d;
  },

  zeroAdding: function(data) {
    var d = [];
    var last_y = 0;
    data.forEach(function(coordinates) {
      if (coordinates.y != 0 && last_y == 0) {
        d.push({x: coordinates.x, y: 0});
      }
      d.push(coordinates);
      last_y = coordinates.y;
    });
    return d;
  }
});

App.taskTimeLineMapper = App.QuickDataMapper.create({
  model: null, //model will be set outside of mapper
  config:{
    allmap:'map',
    allshuffle:'shuffle',
    allreduce:'reduce'
  },
  map:function (json) {
    var job = this.get('model'); // @model App.MainAppsItemBarView
    var parseResult = this.parseIt(json, this.config);

    $.each(parseResult, function (field, value) {
      job.set(field, value);
    });
    fillEmptyValues(this, job);
  }
});

App.jobTasksMapper = App.QuickDataMapper.create({
  model: null, //model will be set outside of mapper
  config:{
    mapNodeLocal:'mapNodeLocal',
    mapRackLocal:'mapRackLocal',
    mapOffSwitch:'mapOffSwitch',
    reduceOffSwitch:'reduceOffSwitch',
    submit:'submitTime',
    finish:'finishTime'
  },
  map:function (json) {
    var job = this.get('model'); // @model App.MainAppsItemBarView
    var parseResult = this.parseIt(json, this.config);
    $.each(parseResult, function (field, value) {
      job.set(field, value);
    });
  }
});
