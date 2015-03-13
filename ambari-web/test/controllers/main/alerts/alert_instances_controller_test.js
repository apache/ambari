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

describe('App.MainAlertInstancesController', function () {

  beforeEach(function () {
    controller = App.MainAlertInstancesController.create({});
  });

  describe('#fetchAlertInstances', function () {

    describe('loading instances from correct endpoint', function () {

      beforeEach(function () {
        sinon.stub(App.ajax, 'send', Em.K);
      });

      afterEach(function () {
        App.ajax.send.restore();
      });

      it('should load by Host name', function () {

        controller.loadAlertInstancesByHost('host');
        console.log(App.ajax.send.args[0]);
        expect(App.ajax.send.args[0][0].name).to.equal('alerts.instances.by_host');
        expect(App.ajax.send.args[0][0].data.hostName).to.equal('host');

      });

      it('should load by AlertDefinition id', function () {

        controller.loadAlertInstancesByAlertDefinition('1');
        console.log(App.ajax.send.args[0]);
        expect(App.ajax.send.args[0][0].name).to.equal('alerts.instances.by_definition');
        expect(App.ajax.send.args[0][0].data.definitionId).to.equal('1');

      });

      it('should load all', function () {

        controller.loadAlertInstances();
        console.log(App.ajax.send.args[0]);
        expect(App.ajax.send.args[0][0].name).to.equal('alerts.instances');

      });

    });

  });


  describe('#showPopup', function () {

    describe('#bodyClass', function () {

      var bodyView;

      beforeEach(function () {
        controller.reopen({unhealthyAlertInstances: [
          App.AlertInstance.createRecord({state: 'CRITICAL'}),
          App.AlertInstance.createRecord({state: 'WARNING'}),
          App.AlertInstance.createRecord({state: 'WARNING'}),
          App.AlertInstance.createRecord({state: 'CRITICAL'})
        ]});
        bodyView = controller.showPopup().get('bodyClass').create();
      });

      it('#content', function () {
        expect(bodyView.get('content.length')).to.equal(4);
      });

      it('#isLoaded', function () {
        expect(bodyView.get('isLoaded')).to.be.true;
      });

      it('#isAlertEmptyList', function () {
        expect(bodyView.get('isAlertEmptyList')).to.be.false;
      });

    });

  });

});