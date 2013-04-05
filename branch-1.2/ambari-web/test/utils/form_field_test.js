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
require('models/form');


/*
 * formField.isValid property doesn't update correctly, so I have to work with errorMessage property
 */
describe('App.FormField', function () {

  describe('#validate()', function () {
    /*DIGITS TYPE*/
    it('123456789 is correct digits', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'digits');
      formField.set('value', 123456789);
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('"a33bc" is incorrect digits', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'digits');
      formField.set('value', 'a33bc');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    /*DIGITS TYPE END*/
    /*NUMBER TYPE*/
    it('+1234 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '+1234');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('-1234 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-1234');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('-1.23.6 is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-1.23.6');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    it('+1.6 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', +1.6);
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('-1.6 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', -1.6);
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('1.6 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', 1.6);
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('-.356 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-.356');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('+.356 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '+.356');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('-1. is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-1.');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    it('+1. is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '+1.');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    it('1. is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '1.');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    it('-1,23,6 is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-1,23,6');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    it('-1234567890 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '-1234567890');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('+1234567890 is correct number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '+1234567890');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(true);
    })
    it('123eed is incorrect number', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'number');
      formField.set('value', '123eed');
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    /*NUMBER TYPE END*/
    /*REQUIRE*/
    it('Required field shouldn\'t be empty', function () {
      var formField = App.FormField.create();
      formField.set('displayType', 'string');
      formField.set('value', '');
      formField.set('isRequired', true);
      formField.validate();
      expect(formField.get('errorMessage') === '').to.equal(false);
    })
    /*REQUIRE END*/

  })
})