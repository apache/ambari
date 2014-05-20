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
require('models/dataset');

var dataset,
  datasetData = {
    id: 'dataset',
    name: 'dataset'
  },
  statusCases = [
    {
      status: 'RUNNING',
      property: 'isRunning'
    },
    {
      status: 'SUSPENDED',
      property: 'isSuspended'
    },
    {
      status: 'SUBMITTED',
      property: 'isSubmitted'
    }
  ],
  healthCases = [
    {
      title: 'should be live',
      data: {
        datasetJobs: [
          Em.Object.create({
            status: 'SUCCESSFUL'
          })
        ]
      },
      className: 'health-status-LIVE',
      icon: App.healthIconClassGreen
    },
    {
      title: 'should be dead for failed first job',
      data: {
        datasetJobs: [
          Em.Object.create({
            status: 'SUSPENDED',
            endDate: 1
          }),
          Em.Object.create({
            status: 'FAILED',
            endDate: 0
          })
        ]
      },
      className: 'health-status-DEAD-RED',
      icon: App.healthIconClassRed
    },
    {
      title: 'should be for no jobs',
      data: {
        datasetJobs: []
      },
      className: 'health-status-LIVE',
      icon: App.healthIconClassGreen
    }
  ];

describe('App.Dataset', function () {

  beforeEach(function () {
    dataset = App.Dataset.createRecord(datasetData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(dataset);
  });

  describe('#prefixedName', function () {
    it('should add mirroring prefix before the name', function () {
      dataset.set('name', 'name');
      expect(dataset.get('prefixedName')).to.equal(App.mirroringDatasetNamePrefix + 'name');
    });
  });

  describe('#statusFormatted', function () {
    it('should be in lower case and capitalized', function () {
      dataset.set('status', 'RUNNING');
      expect(dataset.get('statusFormatted')).to.equal('Running');
    });
  });

  statusCases.forEach(function (item) {
    describe(item.property, function () {

      beforeEach(function () {
        dataset.set('status', item.status);
      });

      it('should be true', function () {
        expect(dataset.get(item.property)).to.be.true;
      });

      it('others should be false', function () {
        var falseProperties = statusCases.mapProperty('property').without(item.property);
        var falseStates = [];
        falseProperties.forEach(function (prop) {
          falseStates.push(dataset.get(prop));
        });
        expect(falseStates).to.eql([false, false]);
      });

    });
  });

  describe('#healthClass', function () {
    healthCases.forEach(function (item) {
      it(item.title, function () {
        dataset.reopen(item.data);
        expect(dataset.get('healthClass')).to.equal(item.className);
      });
    });
  });

  describe('#healthIconClass', function () {
    healthCases.forEach(function (item) {
      it(item.title, function () {
        dataset.reopen(item.data);
        expect(dataset.get('healthIconClass')).to.equal(item.icon);
      });
    });
  });

});
