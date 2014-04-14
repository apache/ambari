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
var jobsUtils = require('utils/jobs');
require('models/service/yarn');
require('mappers/jobs/hive_job_mapper');
require('utils/ajax/ajax');
require('utils/http_client');
require('models/jobs/tez_dag');
require('controllers/main/jobs/hive_job_details_controller');

describe('App.MainHiveJobDetailsController', function () {
  var yarnService = {
    id: 'YARN',
    ahsWebPort: 8188
  };
  App.store.load(App.YARNService, yarnService);
  var job = {
    id: 'hrt_qa_20140311131919_1d932567-71c2-4341-9b50-6df1f58a9114',
    queryText: 'show tables',
    name: 'hrt_qa_20140311131919_1d932567-71c2-4341-9b50-6df1f58a9114',
    user: ['hrt_qa'],
    hasTezDag: true,
    failed: false,
    startTime: 1394569191001,
    jobType: 'hive',
    tezDag: {
      id: 'hrt_qa_20140311131919_1d932567-71c2-4341-9b50-6df1f58a9114:1',
      instanceId: 'dag_1394502141829_0425_1',
      name: 'hrt_qa_20140311131919_1d932567-71c2-4341-9b50-6df1f58a9114:1',
      yarnApplicationId: 'application_1395263571423_0014',
      stage: 'Stage-1'
    }
  };
  var mainHiveJobDetailsController = App.MainHiveJobDetailsController.create({
    job: Ember.Object.create(job),
    content: {
      id: 'id'
    }
  });
  mainHiveJobDetailsController.set('job', mainHiveJobDetailsController.get('content'));
  describe('#loaded', function () {
    it('content loading from model', function () {
      jobsUtils.refreshHiveJobDetails = function (hiveJob, successCallback, errorCallback) {
        successCallback();
      };
      mainHiveJobDetailsController.loadJobDetails();
      expect(mainHiveJobDetailsController.get('loaded')).to.equal(true);
      expect(mainHiveJobDetailsController.get('content.id')).to.equal(mainHiveJobDetailsController.get('job.id'));
    });
  });
});
