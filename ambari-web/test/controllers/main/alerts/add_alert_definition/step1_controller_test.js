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
require('controllers/main/alerts/add_alert_definition/step1_controller');

var controller;

describe('App.AddAlertDefinitionStep1Controller', function () {

  beforeEach(function () {
    controller = App.AddAlertDefinitionStep1Controller.create({content: {}});
  });

  describe('#selectType', function() {

    beforeEach(function () {
      controller.get('alertDefinitionsTypes').setEach('isActive', false);
    });

    it('should set isActive for selected type', function () {
      var e = {context: {value: 'PORT'}};
      controller.selectType(e);
      expect(controller.get('alertDefinitionsTypes').findProperty('value', 'PORT').get('isActive')).to.be.true;
    });

  });

  describe('#loadStep', function () {

    beforeEach(function () {
      controller.set('content.selectedType', 'PORT');

    });

    it('should set predefined type', function () {
      controller.loadStep();
      expect(controller.get('alertDefinitionsTypes').findProperty('value', 'PORT').get('isActive')).to.be.true;
    });

  });

  describe('#isSubmitDisabled', function () {

    beforeEach(function () {
      controller.get('alertDefinitionsTypes').setEach('isActive', false);
    });

    it('should be based on isActive', function () {

      expect(controller.get('isSubmitDisabled')).to.be.true;
      controller.get('alertDefinitionsTypes').objectAt(0).set('isActive', true);
      expect(controller.get('isSubmitDisabled')).to.be.false;

    });

  });

});