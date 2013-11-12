/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/validators/service_configs_validator');

describe('App.ServiceConfigsValidator', function() {

  describe('#validateConfig', function() {
    it('No config validator', function() {
      var v = App.ServiceConfigsValidator.create({});
      expect(v.validateConfig(Em.Object.create({name:'name'}))).to.equal(null);
    });
  });

  describe('#validatorLessThenDefaultValue', function() {
    var tests = [
      {
        recommendedDefaults: {
          'property1': 100500
        },
        config: Em.Object.create({
          value: 100000,
          name: 'property1'
        }),
        m: 'Numeric value',
        e: 'string'
      },
      {
        recommendedDefaults: {
          'property1': 'xx100500x'
        },
        config: Em.Object.create({
          value: 'xx100000x',
          name: 'property1'
        }),
        m: 'String value',
        e: 'string'
      },
      {
        recommendedDefaults: {
          'property1': null
        },
        config: Em.Object.create({
          value: 100000,
          name: 'property1'
        }),
        m: 'No default value for property',
        e: null
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var v = App.ServiceConfigsValidator.create({});
        v.set('recommendedDefaults', test.recommendedDefaults);
        var r = v.validatorLessThenDefaultValue(test.config);
        if (test.e) {
          expect(r).to.be.a(test.e);
        }
        else {
          expect(r).to.equal(null)
        }
      });
    });
  });

});
