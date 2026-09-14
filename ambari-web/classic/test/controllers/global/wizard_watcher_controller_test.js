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
require('controllers/global/wizard_watcher_controller');
var scopedWorkflowPersistence = require('utils/scoped_workflow_persistence');

var controller;

describe('App.wizardWatcherController', function () {
  beforeEach(function() {
    controller = App.WizardWatcherController.create();
  });

  describe("#isWizardRunning", function() {
    it("wizardUser is null", function() {
      controller.set('wizardUser', null);
      controller.propertyDidChange('isWizardRunning');
      expect(controller.get('isWizardRunning')).to.be.false;
    });
    it("wizardUser is correct", function() {
      controller.set('wizardUser', 'admin');
      controller.propertyDidChange('isWizardRunning');
      expect(controller.get('isWizardRunning')).to.be.true;
    });
  });

  describe("#wizardDisplayName", function() {
    beforeEach(function () {
      controller.set('wizardUser', 'tdk');
      sinon.stub(App.router, 'get').returns(Em.Object.create({displayName: 'Wizard'}));
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("controllerName is null", function() {
      controller.set('controllerName', null);
      controller.propertyDidChange('wizardDisplayName');
      expect(controller.get('wizardDisplayName')).to.be.empty;
    });
    it("controllerName is correct", function() {
      controller.set('controllerName', 'ctrl1');
      controller.propertyDidChange('wizardDisplayName');
      expect(controller.get('wizardDisplayName')).to.equal(Em.I18n.t('wizard.inProgress').format('Wizard', 'tdk'));
    });
  });


  describe("#isNonWizardUser", function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("isWizardRunning is false", function() {
      controller.reopen({
        isWizardRunning: false
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.false;
    });
    it("isWizardRunning is true, wizardUser is admin", function() {
      controller.setProperties({
        isWizardRunning: true,
        wizardUser: 'admin'
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.false;
    });
    it("isWizardRunning is true, wizardUser is admin2", function() {
      controller.setProperties({
        isWizardRunning: true,
        wizardUser: 'admin2'
      });
      controller.propertyDidChange('isNonWizardUser');
      expect(controller.get('isNonWizardUser')).to.be.true;
    });
  });

  describe("#setUser()", function() {
    beforeEach(function () {
      sinon.stub(scopedWorkflowPersistence, 'setUser').returns($.Deferred().resolve().promise());
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function () {
      scopedWorkflowPersistence.setUser.restore();
      App.router.get.restore();
    });
    it("claims the scoped workflow", function() {
      controller.setUser('ctrl1');
      expect(scopedWorkflowPersistence.setUser.calledWith('ctrl1')).to.be.true;
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });

  describe("#resetUser()", function() {
    beforeEach(function () {
      sinon.stub(scopedWorkflowPersistence, 'release').returns($.Deferred().resolve().promise());
    });
    afterEach(function () {
      scopedWorkflowPersistence.release.restore();
    });
    it("releases scoped workflow ownership", function() {
      controller.setProperties({wizardUser: 'admin', controllerName: 'ctrl1'});
      controller.resetUser();
      expect(scopedWorkflowPersistence.release.calledOnce).to.be.true;
      expect(controller.get('wizardUser')).to.be.null;
      expect(controller.get('controllerName')).to.be.null;
    });
    it("retains scoped workflow ownership when release fails", function() {
      scopedWorkflowPersistence.release.restore();
      sinon.stub(scopedWorkflowPersistence, 'release').returns($.Deferred().reject({
        code: 'WORKFLOW_VERSION_CONFLICT',
        message: 'changed'
      }).promise());
      sinon.stub(App, 'showConfirmationPopup');
      controller.setProperties({wizardUser: 'admin', controllerName: 'ctrl1'});

      controller.resetUser();

      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
      App.showConfirmationPopup.restore();
    });
  });

  describe("#getUser()", function() {
    beforeEach(function () {
      sinon.stub(scopedWorkflowPersistence, 'loadCurrent').returns($.Deferred().resolve({
        values: {wizardData: {userName: 'admin', controllerName: 'ctrl1'}}
      }).promise());
    });
    afterEach(function () {
      scopedWorkflowPersistence.loadCurrent.restore();
    });
    it("loads scoped workflow ownership", function() {
      controller.getUser();
      expect(scopedWorkflowPersistence.loadCurrent.calledOnce).to.be.true;
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });

  describe("#getUserPrefSuccessCallback()", function() {
    it("data is null", function() {
      controller.getUserPrefSuccessCallback(null);
      expect(controller.get('wizardUser')).to.be.null;
      expect(controller.get('controllerName')).to.be.null;
    });
    it("data is correct", function() {
      controller.getUserPrefSuccessCallback({userName: 'admin', controllerName: 'ctrl1'});
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });

  describe("#getUserPrefErrorCallback()", function() {
    beforeEach(function () {
      sinon.stub(App, 'showConfirmationPopup');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });
    it("shows one recovery error without releasing another tab's workflow", function() {
      controller.setProperties({wizardUser: 'admin', controllerName: 'ctrl1'});
      controller.getUserPrefErrorCallback({code: 'WORKFLOW_VERSION_CONFLICT', message: 'changed'});
      controller.getUserPrefErrorCallback({code: 'WORKFLOW_VERSION_CONFLICT', message: 'changed'});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });

  describe("#retryPersistence()", function () {
    beforeEach(function () {
      sinon.stub(scopedWorkflowPersistence, 'retryLoad').returns($.Deferred().resolve({
        values: {wizardData: {userName: 'admin', controllerName: 'ctrl1'}}
      }).promise());
    });
    afterEach(function () {
      scopedWorkflowPersistence.retryLoad.restore();
    });
    it("reloads the authoritative scoped workflow", function () {
      controller.retryPersistence();

      expect(scopedWorkflowPersistence.retryLoad.calledOnce).to.be.true;
      expect(controller.get('wizardUser')).to.equal('admin');
      expect(controller.get('controllerName')).to.equal('ctrl1');
    });
  });
});
