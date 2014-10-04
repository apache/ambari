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
    App.set('viewEnabled', true);
    Ember.run(App, App.advanceReadiness);
  },

  teardown: function () {
    App.reset();
  }

});

test('route', function () {

  visit('/');
  andThen(function () {
    equal(currentRouteName(), 'slider_apps.index', 'route is valid');
    equal(currentPath(), 'slider_apps.index', 'path is valid');
    equal(currentURL(), '/', 'url is valid');
  });

});

test('sliderConfigs', function () {

  visit('/');
  // configs count may be changed by adding new slider-configs
  equal(App.SliderApp.store.all('sliderConfig').content.length, 2, 'slider configs should be set');

});

test('Create-App button', function () {

  visit('/');
  click('.create-app a');

  andThen(function () {
    equal(currentRouteName(), 'createAppWizard.step1', 'route is valid');
    equal(currentPath(), 'slider_apps.createAppWizard.step1', 'path is valid');
    equal(currentURL(), '/createAppWizard/step1', 'url is valid');
  });

});