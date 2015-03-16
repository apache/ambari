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
    App.set('viewEnabled', true);
    App.__container__.lookup('controller:Slider').getViewDisplayParametersSuccessCallback({
      "ViewInstanceInfo": {
        "context_path": "/views/SLIDER/1.0.0/s1",
        "description": "DESCRIPTION",
        "label": "SLIDER LABEL",
        "properties": {
          "slider.user": "admin"
        }
      }
    });
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
  equal(App.SliderApp.store.all('sliderConfig').content.length, 4, 'slider configs should be set');

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

test('Create-App button visible/hidden', function () {

  Em.run(function () {
    App.__container__.lookup('controller:application').set('hasConfigErrors', true);
  });

  visit('/');
  equal(find('.create-app').length, 0, 'Create App button should be hidden if some config errors');

});

test('Slider Title', function () {

  visit('/');
  equal(find('.slider-app-title').text(), 'SLIDER LABEL', 'App has valid Slider Title');

});

test('Slider Title Popover', function () {

  visit('/');
  triggerEvent('#slider-title', 'mouseenter'); // not hover!
  andThen(function () {
    equal(find('.popover').length, 1, 'popover exists');
    equal(find('.popover-title').text(), 'SLIDER LABEL', 'popover has valid title');
    equal(find('.slider-description').text(), 'DESCRIPTION', 'popover has slider description');
  });

});

test('Clear Filters', function () {

  visit('/');
  fillIn('#filter-row input:eq(0)', 'Some val');
  find('#filter-row select:eq(0)  :nth-child(1)').attr('selected', 'selected');
  fillIn('#filter-row input:eq(1)', 'Some val');
  fillIn('#filter-row input:eq(2)', 'Some val');
  find('#filter-row select:eq(1) :nth-child(1)').attr('selected', 'selected');

  andThen(function () {
    click('.clearFiltersLink');

    andThen(function () {
      equal(find('#filter-row input:eq(0)').val(), '');
      equal(find('#filter-row select:eq(0)').val(), 'All Status');
      equal(find('#filter-row input:eq(1)').val(), '');
      equal(find('#filter-row input:eq(2)').val(), '');
      equal(find('#filter-row select:eq(1)').val(), 'All Dates');

    });
  });

});
