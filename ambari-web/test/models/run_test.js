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

var modelSetup = require('test/init_model_test');
require('models/run');

var run,
  job,
  runData = {
    id: 'run'
  },
  jobData = {
    id: 'job'
  },
  cases = [
    {
      id: 'pig_run',
      type: 'Pig'
    },
    {
      id: 'hive_run',
      type: 'Hive'
    },
    {
      id: 'mr_run',
      type: 'MapReduce'
    },
    {
      id: 'run_pig_hive_mr_id',
      type: ''
    }
  ];

describe('App.Run', function () {

  beforeEach(function () {
    run = App.Run.createRecord(runData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(run);
  });

  describe('#idFormatted', function () {
    it('should shorten id to 20 characters', function () {
      for (var i = 21, name = ''; i--; ) {
        name += 'n';
      }
      run.set('id', name);
      expect(run.get('idFormatted')).to.have.length(20);
    });
  });

  describe('#jobs', function () {

    beforeEach(function () {
      job = App.Job.createRecord(jobData);
      job.reopen({
        run: runData
      });
    });

    afterEach(function () {
      modelSetup.deleteRecord(job);
    });

    it('should load corresponding jobs from the store', function () {
      run.set('loadAllJobs', true);
      expect(run.get('jobs')).to.have.length(1);
      expect(run.get('jobs').objectAt(0).get('run.id')).to.equal('run');
    });

  });

  describe('#duration', function () {
    it('should convert elapsedTime into time format', function () {
      run.set('elapsedTime', 1000);
      expect(run.get('duration')).to.equal('1.00 secs');
    });
  });

  describe('#isRunning', function () {
    it('should be true', function () {
      run.setProperties({
        numJobsTotal: 5,
        numJobsCompleted: 0
      });
      expect(run.get('isRunning')).to.be.true;
    });
    it('should be false', function () {
      run.setProperties({
        numJobsTotal: 5,
        numJobsCompleted: 5
      });
      expect(run.get('isRunning')).to.be.false;
    });
  });

  describe('#inputFormatted', function () {
    it('should convert input into bandwidth format', function () {
      run.set('input', 1024);
      expect(run.get('inputFormatted')).to.equal('1.0KB');
    });
  });

  describe('#outputFormatted', function () {
    it('should convert output into bandwidth format', function () {
      run.set('output', 1024);
      expect(run.get('outputFormatted')).to.equal('1.0KB');
    });
  });

  describe('#lastUpdateTime', function () {
    it('should sum elapsedTime and startTime', function () {
      run.setProperties({
        elapsedTime: 1000,
        startTime: 2000
      });
      expect(run.get('lastUpdateTime')).to.equal(3000);
    });
  });

  describe('#lastUpdateTimeFormattedShort', function () {
    it('should form date and time from lastUpdateTime', function () {
      run.setProperties({
        elapsedTime: 1000,
        startTime: 100000000000
      });
      expect(run.get('lastUpdateTimeFormattedShort')).to.equal('Sat Mar 03 1973');
    });
  });

  describe('#type', function () {
    cases.forEach(function (item) {
      it('should be ' + (item.type ? item.type : 'empty'), function () {
        run.set('id', item.id);
        expect(run.get('type')).to.equal(item.type);
      });
    });
  });

});
