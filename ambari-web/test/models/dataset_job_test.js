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
require('models/dataset_job');

var dataSetJob,
  dataSetJobData = {
    id: 'job',
    name: 'job'
  },
  timeCases = [
    {
      property: 'startFormatted',
      dateProperty: 'startDate'
    },
    {
      property: 'endFormatted',
      dateProperty: 'endDate'
    }
  ],
  timeTestData = [
    {
      title: 'should calculate time period',
      time: function () {
        return App.dateTime();
      },
      result: 'a moment ago'
    },
    {
      title: 'should be empty',
      time: function () {
        return 0;
      },
      result: ''
    }
  ],
  healthCases = [
    {
      status: 'SUCCEEDED',
      className: 'icon-ok'
    },
    {
      status: 'SUSPENDED',
      className: 'icon-cog'
    },
    {
      status: 'WAITING',
      className: 'icon-time'
    },
    {
      status: 'RUNNING',
      className: 'icon-play'
    },
    {
      status: 'KILLED',
      className: 'icon-exclamation-sign'
    },
    {
      status: 'FAILED',
      className: 'icon-warning-sign'
    },
    {
      status: 'ERROR',
      className: 'icon-remove'
    },
    {
      status: '',
      className: 'icon-question-sign'
    }
  ];

describe('App.DataSetJob', function () {

  beforeEach(function () {
    dataSetJob = App.DataSetJob.createRecord(dataSetJobData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(dataSetJob);
  });

  describe('#statusFormatted', function () {
    it('should be in lower case and capitalized', function () {
      dataSetJob.set('status', 'RUNNING');
      expect(dataSetJob.get('statusFormatted')).to.equal('Running');
    });
  });

  describe('#isSuspended', function () {
    it('should be false', function () {
      dataSetJob.set('status', 'RUNNING');
      expect(dataSetJob.get('isSuspended')).to.be.false;
    });
    it('should be true', function () {
      dataSetJob.set('status', 'SUSPENDED');
      expect(dataSetJob.get('isSuspended')).to.be.true;
    });
  });

  timeCases.forEach(function (item) {
    describe('#' + item.property, function () {
      timeTestData.forEach(function (test) {
        it(test.title, function () {
          dataSetJob.set(item.dateProperty, test.time());
          expect(dataSetJob.get(item.property)).to.equal(test.result);
        });
      });
    });
  });

  describe('#healthClass', function () {
    healthCases.forEach(function (item) {
      it('should be ' + item.className, function () {
        dataSetJob.set('status', item.status);
        expect(dataSetJob.get('healthClass')).to.equal(item.className);
      });
    });
  });

});
