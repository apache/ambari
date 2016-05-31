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

/* global moment */

import Ember from 'ember';
import { moduleForComponent, test } from 'ember-qunit';

moduleForComponent('date-range-widget', 'DateRangeWidgetComponent', {
  needs: ['component:extended-input']
});

test('Date fields are set correctly', function() {
  expect(2);

  var component = this.subject();

  var min = moment('04/11/2014', 'DD/MM/YYYY');
  var max = moment('04/12/2014', 'DD/MM/YYYY');
  var from = moment('04/11/2014', 'DD/MM/YYYY');
  var to = moment('04/12/2014', 'DD/MM/YYYY');

  var dateRange = Ember.Object.create({
    from: from.toString(),
    to: to.toString(),
    min: min.toString(),
    max: max.toString()
  });

  component.set('dateRange', Ember.Object.create());

  var $component = this.$();

  Ember.run(function() {
    component.set('dateRange', dateRange);
  });

  equal($component.find('.fromDate').val(), moment(from).format('MM/DD/YYYY'), "From date is set correctly");
  equal($component.find('.toDate').val(), moment(to).format('MM/DD/YYYY'), "To date is set correctly");
});

test('Date fields updates when the date is changed', function() {
  expect(2);

  var component = this.subject();

  var min = moment('04/11/2014', 'DD/MM/YYYY');
  var max = moment('04/12/2014', 'DD/MM/YYYY');
  var from = moment('04/11/2014', 'DD/MM/YYYY');
  var to = moment('04/12/2014', 'DD/MM/YYYY');

  var dateRange = Ember.Object.create({
    from: from.toString(),
    to: to.toString(),
    min: min.toString(),
    max: max.toString()
  });

  Ember.run(function() {
    component.set('dateRange', dateRange);
  });

  var $component = this.$();
  $component.find('.fromDate').datepicker('setDate', '10/10/2014');
  $component.find('.toDate').datepicker('setDate', '11/11/2014');

  equal($component.find('.fromDate').val(), '10/10/2014', "From date field is updated");
  equal($component.find('.toDate').val(), '11/11/2014', "To date field is updated");
});

test('Display dates are formatted correctly', function(){
  expect(2);

  var component = this.subject();

  var min = moment('04/11/2014', 'DD/MM/YYYY');
  var max = moment('04/12/2014', 'DD/MM/YYYY');
  var from = moment('04/11/2014', 'DD/MM/YYYY');
  var to = moment('04/12/2014', 'DD/MM/YYYY');

  var dateRange = Ember.Object.create({
    from: from.toString(),
    to: to.toString(),
    min: min.toString(),
    max: max.toString()
  });

  Ember.run(function () {
    component.set('dateRange', dateRange);
  });

  equal(component.get('displayFromDate'), '11/04/2014', "displayFromDate is formatted correctly");
  equal(component.get('displayToDate'), '12/04/2014', "displayToDate is formatted correctly");
});

test('If from/to are not passed they are set to min/max', function() {
  expect(2);

  var component = this.subject();

  var min = moment('04/11/2014', 'DD/MM/YYYY');
  var max = moment('04/12/2014', 'DD/MM/YYYY');

  var dateRange = Ember.Object.create({
    min: min.toString(),
    max: max.toString()
  });

  Ember.run(function () {
    component.set('dateRange', dateRange);
  });

  var $component = this.$();

  equal(component.get('dateRange.from'), min.toString(), "From date is to min date");
  equal(component.get('dateRange.to'), max.toString(), "To date is set to max date");
});
