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

require('models/hosts');

var hostInfo,
  statusCases = [
    {
      status: 'REGISTERED',
      bootStatusForDisplay: 'Success',
      bootBarColor: 'progress-success',
      bootStatusColor: 'text-success',
      isBootDone: true
    },
    {
      status: 'FAILED',
      bootStatusForDisplay: 'Failed',
      bootBarColor: 'progress-danger',
      bootStatusColor: 'text-error',
      isBootDone: true
    },
    {
      status: 'PENDING',
      bootStatusForDisplay: 'Preparing',
      bootBarColor: 'progress-info',
      bootStatusColor: 'text-info',
      isBootDone: false
    },
    {
      status: 'RUNNING',
      bootStatusForDisplay: 'Installing',
      bootBarColor: 'progress-info',
      bootStatusColor: 'text-info',
      isBootDone: false
    },
    {
      status: 'DONE',
      bootStatusForDisplay: 'Registering',
      bootBarColor: 'progress-info',
      bootStatusColor: 'text-info',
      isBootDone: false
    },
    {
      status: 'REGISTERING',
      bootStatusForDisplay: 'Registering',
      bootBarColor: 'progress-info',
      bootStatusColor: 'text-info',
      isBootDone: false
    }
  ],
  tests = ['bootStatusForDisplay', 'bootBarColor', 'bootStatusColor', 'isBootDone'];

describe('App.HostInfo', function () {

  beforeEach(function () {
    hostInfo = App.HostInfo.create();
  });

  tests.forEach(function (property) {
    describe('#' + property, function () {
      statusCases.forEach(function (testCase) {
        it('should be ' + testCase[property], function () {
          hostInfo.set('bootStatus', testCase.status);
          expect(hostInfo.get(property)).to.equal(testCase[property]);
        });
      });
    });
  });

});
