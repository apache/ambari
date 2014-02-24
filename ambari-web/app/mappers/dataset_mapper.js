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

App.dataSetMapper = App.QuickDataMapper.create({
  model: App.Dataset,
  Jobs_model: App.DataSetJob,
  config: {
    id: 'name',
    name: 'name',
    status: 'status',
    source_cluster_name: 'sourceClusterName',
    target_cluster_name: 'targetClusterName',
    source_dir: 'sourceDir',
    target_dir: 'targetDir',
    frequency: 'frequency',
    frequency_unit: 'frequencyUnit',
    schedule_start_date: 'scheduleStartDate',
    schedule_end_date: 'scheduleEndDate',
    dataset_jobs_key: 'instances',
    dataset_jobs_type: 'array',
    dataset_jobs: {
      item: 'id'
    }
  },
  jobs_config: {
    id: 'id',
    name: 'name',
    status : 'status',
    start_date: 'startTime',
    end_date: 'endTime',
    dataset_id: 'dataset'
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json && json.length > 0) {
      var datasetResults = [];
      var dataSetJobResults = [];
      json.forEach(function (item) {
        item.instances.forEach(function (job) {
          var newInstance = this.parseIt(job, this.get('jobs_config'));
          dataSetJobResults.push(newInstance);
        }, this);
        var newitem = this.parseIt(item, this.get('config'));
            datasetResults.push(newitem);
      }, this);
      App.store.loadMany(this.get('Jobs_model'), dataSetJobResults);
      App.store.loadMany(this.get('model'), datasetResults);
    }
  }
});
