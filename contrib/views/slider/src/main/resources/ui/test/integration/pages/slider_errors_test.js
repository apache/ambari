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

QUnit.module('integration/pages - index', {

  setup: function () {
    sinon.config.useFakeTimers = false;
    Ember.run(App, App.advanceReadiness);
    Em.run(function () {
      var p = {
        validations: [
          {message: 'Some mythical error'},
          {message: 'Error with DNA'}
        ],
        parameters: {}
      };
      App.__container__.lookup('controller:Slider').getParametersFromViewPropertiesSuccessCallback(p);
    });
  },

  teardown: function () {
    App.reset();
  }

});

test('Slider has validation errors', function () {

  visit('/');
  equal(find('.error-message').length, 2, 'Error-messages exist on the page');
  ok(find('.create-app a').attr('disabled'), 'Create App button is disabled');

});

test('Slider has no validation errors', function () {

  Em.run(function () {
    App.__container__.lookup('controller:Slider').getParametersFromViewPropertiesSuccessCallback({
      validations: [],
      parameters: {}
    });
  });

  visit('/');
  equal(find('.error-message').length, 0, 'No error-messages on the page');
  ok(!find('.create-app a').attr('disabled'), 'Create App button is enabled');

});