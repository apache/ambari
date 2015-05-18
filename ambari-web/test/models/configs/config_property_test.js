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

require('models/configs/config_property');

var model;

describe('App.ConfigProperty', function () {
  model = App.ConfigProperty.createRecord();

  describe('#hasErrors', function () {
    it('should set hasErrors to true', function () {
      expect(model.setProperties({'errorMessage': 'some error'}).get('hasErrors')).to.eql(true);
    });
    it('should set hasErrors to false', function () {
      expect(model.setProperties({'errorMessage': ''}).get('hasErrors')).to.eql(false);
    });
  });

  describe('#hasWarnings', function () {
    it('should set hasWarnings to true', function () {
      expect(model.setProperties({'warnMessage': 'some warning'}).get('hasWarnings')).to.eql(true);
    });
    it('should set hasWarnings to false', function () {
      expect(model.setProperties({'warnMessage': ''}).get('hasWarnings')).to.eql(false);
    });
  });

  describe.skip('#isNotDefaultValue', function () {
    var tests = [
      { isEditable: false, value: 1, defaultValue: 2, supportsFinal: true, isFinal: true, defaultIsFinal: false, isNotDefaultValue: false },
      { isEditable: true, value: 1, defaultValue: 1, supportsFinal: false, isFinal: true, defaultIsFinal: true, isNotDefaultValue: false },
      { isEditable: true, value: 1, defaultValue: null, supportsFinal: false, isFinal: true, defaultIsFinal: true, isNotDefaultValue: false },
      { isEditable: true, value: 1, defaultValue: 1, supportsFinal: true, isFinal: true, defaultIsFinal: true, isNotDefaultValue: false },

      { isEditable: true, value: 2, defaultValue: 1, supportsFinal: true, isFinal: true, defaultIsFinal: true, isNotDefaultValue: true },
      { isEditable: true, value: 2, defaultValue: 1, supportsFinal: false, isFinal: true, defaultIsFinal: false, isNotDefaultValue: true },
      { isEditable: true, value: 1, defaultValue: 1, supportsFinal: true, isFinal: false, defaultIsFinal: true, isNotDefaultValue: true },
    ];

    tests.forEach(function(t, i) {
      it('should set isNotDefaultValue to ' + t.isNotDefaultValue + ' situation ' + i, function () {
        expect(model.setProperties({'isEditable': t.isEditable, 'value': t.value, defaultValue: t.defaultValue,
          supportsFinal: t.supportsFinal, isFinal: t.isFinal, defaultIsFinal:t.defaultIsFinal}).get('isNotDefaultValue')).to.eql(t.isNotDefaultValue);
      });
    });
  });

});
