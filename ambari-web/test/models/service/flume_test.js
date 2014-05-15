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
require('models/service/flume');

var flumeAgent,
  flumeAgentData = {
    id: 'agent',
    name: 'agent'
  },
  cases = [
    {
      status: 'RUNNING',
      healthClass: App.healthIconClassGreen
    },
    {
      status: 'NOT_RUNNING',
      healthClass: App.healthIconClassRed
    },
    {
      status: 'UNKNOWN',
      healthClass: App.healthIconClassYellow
    },
    {
      status: 'ANOTHER_STATUS',
      healthClass: App.healthIconClassYellow
    }
  ];

describe('App.FlumeAgent', function () {

  beforeEach(function () {
    flumeAgent = App.FlumeAgent.createRecord(flumeAgentData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(flumeAgent);
  });

  describe('#healthClass', function () {
    cases.forEach(function (item) {
      var healthClass = item.healthClass;
      it('should be ' + healthClass, function () {
        flumeAgent.set('status', item.status);
        expect(flumeAgent.get('healthClass')).to.equal(healthClass);
      });
    });
  });

});
