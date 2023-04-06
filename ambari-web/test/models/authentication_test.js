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

require('models/authentication');

var form,
  methods = [
    {
      name: 'method',
      fields: ['primaryServer', 'searchBaseDn', 'usernameAttribute']
    },
    {
      name: 'bindMethod',
      fields: ['bindUser', 'password', 'passwordRetype']
    }
  ],
  classCases = [
    {
      result: 0,
      message: 'fail',
      className: 'danger'
    },
    {
      result: 1,
      message: 'success',
      className: 'success'
    }
  ];

describe('App.AuthenticationForm', function () {

  beforeEach(function() {
    form = App.AuthenticationForm.create();
  });

  methods.forEach(function (method) {
    method.fields.forEach(function (field) {
      describe('#' + field + '.isRequired', function () {
        [2, 1, 0].forEach(function (i) {
          it('should be ' + i + ' dependent on ' + method.name + ' value', function () {
            form.getField(method.name).set('value', i);
            expect(form.getField(field).get('isRequired')).to.equal(i);
          });
        });
      });
    });
  });

  App.TestAliases.testAsComputedIfThenElse(App.AuthenticationForm.create(), 'testConfigurationMessage', 'testResult', Em.I18n.t('admin.authentication.form.test.success'), Em.I18n.t('admin.authentication.form.test.fail'));

  App.TestAliases.testAsComputedIfThenElse(App.AuthenticationForm.create(), 'testConfigurationClass', 'testResult', 'text-success', 'text-danger');

  describe('#testResult', function () {
    it('should be 0 or 1', function () {
      form.testConfiguration();
      expect([0, 1]).to.include(Number(form.get('testResult')));
    });
  });

  describe('#testConfigurationMessage', function () {
    classCases.forEach(function (item) {
      it('should indicate ' + item.message, function () {
        form.set('testResult', item.result);
        expect(form.get('testConfigurationMessage')).to.equal(Em.I18n.t('admin.authentication.form.test.' + item.message));
      });
    });
  });

  describe('#testConfigurationClass', function () {
    classCases.forEach(function (item) {
      it('should indicate ' + item.className, function () {
        form.set('testResult', item.result);
        expect(form.get('testConfigurationClass')).to.equal('text-' + item.className);
      });
    });
  });

});
