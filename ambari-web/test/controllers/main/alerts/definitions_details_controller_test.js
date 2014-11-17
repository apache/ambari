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

var controller;

describe('App.MainAlertDefinitionDetailsController', function () {

  beforeEach(function () {
    controller = App.MainAlertDefinitionDetailsController.create({
      content: Em.Object.create({
        description: 'description',
        label: 'label'
      })
    });
  });

  describe('#labelValidation()', function () {

    it('should set editing.label.isError to true', function () {
      controller.set('editing.label.value', ' ');
      expect(controller.get('editing.label.isError')).to.be.true;
    });

  });

  describe('#descriptionValidation()', function () {

    it('should set editing.description.isError to true', function () {
      controller.set('editing.description.value', ' ');
      expect(controller.get('editing.description.isError')).to.be.true;
    });

  });

  describe('#edit()', function () {

    it('should change value of value, originalValue and isEditing properties', function () {
      controller.set('editing.label.value', 'test');
      controller.set('editing.label.originalValue', 'test');
      controller.set('editing.label.isEditing', false);

      controller.edit({context:controller.get('editing.label')});

      expect(controller.get('editing.label.value')).to.equal('label');
      expect(controller.get('editing.label.originalValue')).to.equal('label');
      expect(controller.get('editing.label.isEditing')).to.be.true;
    });

  });

  describe('#saveEdit()', function () {

    it('should change values of content.label and isEditing properties', function () {
      controller.set('editing.label.value', 'test');
      controller.set('editing.label.isEditing', true);

      controller.saveEdit({context:controller.get('editing.label')});

      expect(controller.get('content.label')).to.equal('test');
      expect(controller.get('editing.label.isEditing')).to.be.false;
    });

  });

  describe('#toggleState()', function () {

    it('should call App.ajax.send function', function () {

      sinon.spy(App.ajax, 'send');

      controller.toggleState();

      expect(App.ajax.send.calledOnce).to.be.true;

      App.ajax.send.restore();
    });

  });

});
