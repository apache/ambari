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

  describe('#edit()', function () {

    it('should change value of value, originalValue and isEditing properties', function () {
      controller.set('editing.label.value', 'test');
      controller.set('editing.label.originalValue', 'test');
      controller.set('editing.label.isEditing', false);

      controller.edit({context: controller.get('editing.label')});

      expect(controller.get('editing.label.value')).to.equal('label');
      expect(controller.get('editing.label.originalValue')).to.equal('label');
      expect(controller.get('editing.label.isEditing')).to.be.true;
    });

  });

  describe('#saveEdit()', function () {

    it('should change values of content.label and isEditing properties', function () {
      controller.set('editing.label.value', 'test');
      controller.set('editing.label.isEditing', true);

      controller.saveEdit({context: controller.get('editing.label')});

      expect(controller.get('content.label')).to.equal('test');
      expect(controller.get('editing.label.isEditing')).to.be.false;
    });

  });

  describe('#toggleDefinitionState()', function () {
    beforeEach(function() {
      sinon.stub(App.ajax, 'send', Em.K);
      controller.reopen({
        content: [
          App.AlertDefinition.createRecord({id: 1, enabled: true})
        ]
      });
    });

    afterEach(function() {
      App.ajax.send.restore();
    });

    it('should call App.ajax.send function', function () {
      var alertDefinition = controller.get('content')[0];
      controller.toggleDefinitionState(alertDefinition);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#deleteAlertDefinition()", function () {
    beforeEach(function () {
      sinon.stub(App.get('router'), 'transitionTo', Em.K);
    });
    afterEach(function () {
      App.get('router').transitionTo.restore();
    });
    it("deleteAlertDefinitionSuccess", function () {
      controller.deleteAlertDefinitionSuccess();
      expect(App.get('router').transitionTo.calledWith('main.alerts.index')).to.be.true;
    });
  });

  describe("#loadAlertInstancesHistory()", function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it("should load alert instances history", function () {

      controller.set('lastDayAlertsCount', 'test');

      controller.loadAlertInstancesHistory();

      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('lastDayAlertsCount')).to.equal(null);
    });
  });

  describe("#loadAlertInstancesHistorySuccess()", function () {

    it("should calculate alerts count in different hosts", function () {

      controller.set('lastDayAlertsCount', null);

      controller.loadAlertInstancesHistorySuccess({
        items: [
          {
            AlertHistory: {
              host_name: 'host1'
            }
          },
          {
            AlertHistory: {
              host_name: 'host2'
            }
          },
          {
            AlertHistory: {
              host_name: 'host1'
            }
          },
          {
            AlertHistory: {
              host_name: 'host3'
            }
          }
        ]
      });

      expect(controller.get('lastDayAlertsCount')).to.eql({
        host1: 2,
        host2: 1,
        host3: 1
      });
    });
  });

});
