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

describe('App.ServerValidatorMixin', function() {
  var mixinObject = Em.Object.extend(App.ServerValidatorMixin, {});
  describe('#validationSuccess', function() {
    var instanceObject;
    var genRespItem = function(name, filename, level, message) {
      return {
        type: 'configuration',
        'config-name': name,
        'config-type': filename,
        level: level,
        message: message
      };
    };
    var genResponse = function(items) {
      return {
        items: items.map(function(item) { return genRespItem.apply(undefined, item); })
      };
    };
    var genConfigs = function(configs) {
      return Em.Object.create({
        configs: configs.map(function(item) {
            return Em.Object.create({ name: item[0], filename: item[1] });
          })
      });
    };
    var tests = [
      {
        stepConfigs: Em.A([
          genConfigs([
            ['prop1', 'some-site.xml']
          ])
        ]),
        resources: [
          genResponse([
            ['prop1', 'some-site', 'WARN', 'Some warn'],
            ['prop2', 'some-site', 'ERROR', 'Value should be set']
          ])
        ],
        expected: [
          { prop: 'configValidationError', value: true },
          { prop: 'configValidationWarning', value: true },
          { prop: 'configValidationGlobalMessage.length', value: 1 },
          { prop: 'configValidationGlobalMessage[0].serviceName', value: 'Some Service' },
          { prop: 'configValidationGlobalMessage[0].propertyName', value: 'prop2' }
        ],
        message: 'validation failed on absent property from step configs. global message should be showed.'
      },
      {
        stepConfigs: Em.A([
          genConfigs([
            ['prop1', 'some-site.xml'],
            ['prop2', 'some-site.xml']
          ])
        ]),
        resources: [
          genResponse([
            ['prop1', 'some-site', 'WARN', 'Some warn']
          ])
        ],
        expected: [
          { prop: 'configValidationError', value: false },
          { prop: 'configValidationWarning', value: true },
          { prop: 'configValidationGlobalMessage.length', value: 0}
        ],
        message: 'all properties present in step configs. validation failed. Present WARN and ERROR level messages.'
      },
            {
        stepConfigs: Em.A([
          genConfigs([
            ['prop1', 'some-site.xml'],
            ['prop2', 'some-site.xml']
          ])
        ]),
        resources: [
          {
            items: []
          }
        ],
        expected: [
          { prop: 'configValidationFailed', value: false },
          { prop: 'configValidationError', value: false },
          { prop: 'configValidationWarning', value: false },
          { prop: 'configValidationGlobalMessage.length', value: 0}
        ],
        message: 'validation success. no errors flags should be set.'
      }
    ];
    
    beforeEach(function() {
      instanceObject = mixinObject.create({});
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          displayName: 'Some Service',
          configTypes: { 'some-site': {} }
        })
      ]);
    });

    afterEach(function() {
      App.StackService.find.restore();
    });
    
    tests.forEach(function(test) {
      describe(test.message, function() {

        beforeEach(function () {
          instanceObject.set('stepConfigs', test.stepConfigs);
          instanceObject.validationSuccess({resources: test.resources});
        });

        test.expected.forEach(function(e) {
          it(e.prop + ': ' + e.value, function () {
            expect(instanceObject).to.have.deep.property(e.prop, e.value);
          });
        });
      });
    });
  });
});

