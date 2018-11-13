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
require('views/common/rolling_restart_view');

describe('App.RollingRestartView', function () {

  var view = App.RollingRestartView.create({
    restartHostComponents: []
  });

  describe('#initialize', function () {
    var testCases = [
      {
        restartHostComponents: [],
        result: {
          batchSize: 1,
          tolerateSize: 1,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(10),
        result: {
          batchSize: 1,
          tolerateSize: 1,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(11),
        result: {
          batchSize: 2,
          tolerateSize: 2,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'NOT_DATANODE',
        restartHostComponents: new Array(20),
        result: {
          batchSize: 2,
          tolerateSize: 2,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'HBASE_REGIONSERVER',
        restartHostComponents: new Array(10),
        result: {
          batchSize: 1,
          tolerateSize: 1,
          isRegionServer: true,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'DATANODE',
        restartHostComponents: new Array(20),
        result: {
          batchSize: 1,
          tolerateSize: 1,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      }
    ];

    testCases.forEach(function (test) {
      describe(test.restartHostComponents.length + ' components of ' + test.hostComponentName + ' to restart', function () {

        beforeEach(function () {
          view.set('batchSize', -1);
          view.set('interBatchWaitTimeSeconds', -1);
          view.set('tolerateSize', -1);
          view.set('regionMoverThreadPoolSize', -1);
          view.set('hostComponentName', test.hostComponentName);
          view.set('restartHostComponents', test.restartHostComponents);
          view.initialize();
        });

        it('batchSize is ' + test.result.batchSize, function () {
          expect(view.get('batchSize')).to.equal(test.result.batchSize);
        });

        it('tolerateSize is ' + test.result.tolerateSize, function () {
          expect(view.get('tolerateSize')).to.equal(test.result.tolerateSize);
        });

        it('isRegionServer is ' + test.result.isRegionServer, function () {
          expect(view.get('isRegionServer')).to.equal(test.result.isRegionServer);
        });

        it('gracefulRSRestart is ' + test.result.gracefulRSRestart, function () {
          expect(view.get('gracefulRSRestart')).to.equal(test.result.gracefulRSRestart);
        });

        it('disableHBaseBalancerBeforeRR is ' + test.result.disableHBaseBalancerBeforeRR, function () {
          expect(view.get('disableHBaseBalancerBeforeRR')).to.equal(test.result.disableHBaseBalancerBeforeRR);
        });

        it('enableHBaseBalancerAfterRR is ' + test.result.enableHBaseBalancerAfterRR, function () {
          expect(view.get('enableHBaseBalancerAfterRR')).to.equal(test.result.enableHBaseBalancerAfterRR);
        });

        it('regionMoverThreadPoolSize is ' + test.result.regionMoverThreadPoolSize, function () {
          expect(view.get('regionMoverThreadPoolSize')).to.equal(test.result.regionMoverThreadPoolSize);
        });
      })
    }, this);
  });

  describe('#observeGracefulRSRestart', function () {
    var testCases = [
      {
        hostComponentName: 'DATANODE',
        restartHostComponents: new Array(10),
        result: {
          batchSize: 1,
          tolerateSize: 1,
          interBatchWaitTimeSeconds: 120,
          isRegionServer: false,
          gracefulRSRestart: false,
          disableHBaseBalancerBeforeRR: false,
          enableHBaseBalancerAfterRR: false,
          regionMoverThreadPoolSize: 1
        }
      },
      {
        hostComponentName: 'HBASE_REGIONSERVER',
        restartHostComponents: new Array(10),
        result: {
          batchSize: 1,
          tolerateSize: 1,
          interBatchWaitTimeSeconds: 0,
          isRegionServer: true,
          gracefulRSRestart: true,
          disableHBaseBalancerBeforeRR: true,
          enableHBaseBalancerAfterRR: true,
          regionMoverThreadPoolSize: 10
        }
      }
    ];

    testCases.forEach(function (test) {
      describe(test.restartHostComponents.length + ' components of ' + test.hostComponentName + ' to restart', function () {

        before(function () {
          view.set('batchSize', -1);
          view.set('interBatchWaitTimeSeconds', -1);
          view.set('tolerateSize', -1);
          view.set('regionMoverThreadPoolSize', -1);
          view.set('hostComponentName', test.hostComponentName);
          view.set('restartHostComponents', test.restartHostComponents);
          view.initialize();
          view.set('gracefulRSRestart', true);
        });

        it('interBatchWaitTimeSeconds is ' + test.result.interBatchWaitTimeSeconds, function () {
          expect(view.get('interBatchWaitTimeSeconds')).to.equal(test.result.interBatchWaitTimeSeconds);
        });

        it('isRegionServer is ' + test.result.isRegionServer, function () {
          expect(view.get('isRegionServer')).to.equal(test.result.isRegionServer);
        });

        it('gracefulRSRestart is ' + test.result.gracefulRSRestart, function () {
          expect(view.get('gracefulRSRestart')).to.equal(test.result.gracefulRSRestart);
        });

        it('disableHBaseBalancerBeforeRR is ' + test.result.disableHBaseBalancerBeforeRR, function () {
          expect(view.get('disableHBaseBalancerBeforeRR')).to.equal(test.result.disableHBaseBalancerBeforeRR);
        });

        it('enableHBaseBalancerAfterRR is ' + test.result.enableHBaseBalancerAfterRR, function () {
          expect(view.get('enableHBaseBalancerAfterRR')).to.equal(test.result.enableHBaseBalancerAfterRR);
        });

        it('regionMoverThreadPoolSize is ' + test.result.regionMoverThreadPoolSize, function () {
          expect(view.get('regionMoverThreadPoolSize')).to.equal(test.result.regionMoverThreadPoolSize);
        });

        it('errors is empty', function () {
          expect(view.get('errors').toString()).to.equal('');
        });
      })
    }, this);
  });
});
