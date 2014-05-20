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
require('models/jobs/job');

var job,
  jobData = {
    id: 'job'
  },
  timeCases = [
    {
      toSet: 'startTime',
      toExpect: 'startTimeDisplay'
    },
    {
      toSet: 'endTime',
      toExpect: 'endTimeDisplay'
    }
  ],
  timeDataCorrect = {
    startTime: 1000,
    endTime: 2000
  },
  timeDataRunning = {
    startTime: App.dateTime() - 1000,
    endTime: undefined
  },
  timeDataIncorrect = {
    startTime: App.dateTime() - 1000,
    endTime: 1
  };

describe('App.AbstractJob', function () {

  beforeEach(function () {
    job = App.AbstractJob.createRecord(jobData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(job);
  });

  timeCases.forEach(function (item) {
    var toSet = item.toSet,
      toExpect = item.toExpect;
    describe('#' + toExpect, function () {
      it('should be empty', function () {
        job.set(toSet, 0);
        expect(job.get(toExpect)).to.be.empty;
      });
      it('should return formatted time', function () {
        job.set(toSet, 1000000);
        expect(job.get(toExpect)).to.equal('Thu, Jan 01, 1970 00:16');
      });
    });
  });

  describe('#duration', function () {
    it('should calculate the difference between endTime and startTime', function () {
      job.setProperties(timeDataCorrect);
      expect(job.get('duration')).to.equal(1000);
    });
    it('should calculate the difference between current time and startTime if the job is running', function () {
      job.setProperties(timeDataRunning);
      expect(job.get('duration')).to.be.at.least(1000);
    });
    it('should calculate the difference between current time and startTime if endTime is incorrect', function () {
      job.setProperties(timeDataIncorrect);
      expect(job.get('duration')).to.be.at.least(1000);
    });
  });

  describe('#durationDisplay', function () {
    it('should return formatted string', function () {
      job.setProperties(timeDataCorrect);
      expect(job.get('durationDisplay')).to.equal('1.00 secs');
    });
  });

});
