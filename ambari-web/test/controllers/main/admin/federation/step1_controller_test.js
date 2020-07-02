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

function getController() {
  return App.NameNodeFederationWizardStep1Controller.create({});
}

describe('App.NameNodeFederationWizardStep1Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#next()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      App.router.send.restore();
    });

    it("App.router.send should be called", function() {
      controller.reopen({
        isNameServiceIdError: false
      });
      controller.next();
      expect(App.router.send.calledOnce).to.be.true;
    });

    it("App.router.send should not be called", function() {
      controller.reopen({
        isNameServiceIdError: true
      });
      controller.next();
      expect(App.router.send.calledOnce).to.be.false;
    });
  });

  describe("#existingNameServices()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
      sinon.stub(App.HDFSService, 'find').returns([
        Em.Object.create({
          masterComponentGroups: [
            {name: 'YARN'},
            {name: 'TEZ'},
            {name: 'HIVE'}
          ]
        })
      ]);
    });

    afterEach(function() {
      App.HDFSService.find.restore();
      this.mock.restore();
    });

    it("should return array with services", function() {
      this.mock.returns(true);
      controller.propertyDidChange('existingNameServices');
      expect(controller.get('existingNameServices')).to.eql(['YARN', 'TEZ', 'HIVE']);
    });

    it("should return empty array", function() {
      this.mock.returns(false);
      controller.propertyDidChange('existingNameServices');
      expect(controller.get('existingNameServices')).to.eql([]);
    });
  });

  describe("#existingNameServicesString()", function () {

    it("should return string with services", function() {
      controller.reopen({
        existingNameServices: ['YARN', 'TEZ', 'HIVE']
      });
      controller.propertyDidChange('existingNameServicesString');
      expect(controller.get('existingNameServicesString')).to.equal('YARN, TEZ, HIVE');
    });
  });

  describe("#isNameServiceIdError()", function () {

    it("should return false", function() {
      controller.reopen({
        existingNameServices: ['TEZ', 'HIVE'],
        content: Em.Object.create({
          nameServiceId: 'YARN'
        })
      });
      controller.propertyDidChange('isNameServiceIdError');
      expect(controller.get('isNameServiceIdError')).to.be.false;
    });

    it("should return true", function() {
      controller.reopen({
        existingNameServices: ['YARN', 'TEZ', 'HIVE'],
        content: Em.Object.create({
          nameServiceId: null
        })
      });
      controller.propertyDidChange('isNameServiceIdError');
      expect(controller.get('isNameServiceIdError')).to.be.true;
    });
  });

});
