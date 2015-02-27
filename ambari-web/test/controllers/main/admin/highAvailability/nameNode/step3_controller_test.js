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

describe('App.HighAvailabilityWizardStep3Controller', function() {
  
  describe('#removeConfigs', function() {

    var tests = [
      {
        m: 'should not delete properties if configsToRemove is empty',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {},
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        }
      },
      {
        m: 'should delete properties from configsToRemove',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {
          'site1': ['property1', 'property3']
        },
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property2: 'value2',
                property4: 'value4'
              }
            }
          ]
        }
      },
      {
        m: 'should delete properties from configsToRemove from different sites',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            },
            {
              type: 'site2',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {
          'site1': ['property1', 'property3'],
          'site2': ['property2', 'property4']
        },
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property2: 'value2',
                property4: 'value4'
              }
            },
            {
              type: 'site2',
              properties: {
                property1: 'value1',
                property3: 'value3'
              }
            }
          ]
        }
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.HighAvailabilityWizardStep3Controller.create({
          configsToRemove: test.toRemove,
          serverConfigData: test.configs
        });
        var result = controller.removeConfigs(test.toRemove, controller.get('serverConfigData'));
        expect(JSON.stringify(controller.get('serverConfigData'))).to.equal(JSON.stringify(test.expected));
        expect(JSON.stringify(result)).to.equal(JSON.stringify(test.expected));
      });
    });
  });
});

