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
require('controllers/main/admin/stack_versions/stack_version_details_controller');
var mainStackVersionsDetailsController;

describe('App.MainStackVersionsDetailsController', function () {

  beforeEach(function () {
    mainStackVersionsDetailsController = App.MainStackVersionsDetailsController.create();
  });

  describe('#installStackVersion', function () {
    beforeEach(function() {
      sinon.stub(mainStackVersionsDetailsController, 'showProgressPopup', Em.K);
      sinon.stub(mainStackVersionsDetailsController, 'doInstallStackVersion', Em.K);
    });
    afterEach(function() {
      mainStackVersionsDetailsController.showProgressPopup.restore();
      mainStackVersionsDetailsController.doInstallStackVersion.restore();
    });
    it("shows installing proggress", function() {
      mainStackVersionsDetailsController.reopen({'installInProgress': true});
      mainStackVersionsDetailsController.installStackVersion({});
      expect(mainStackVersionsDetailsController.showProgressPopup.calledOnce).to.be.true;
      expect(mainStackVersionsDetailsController.doInstallStackVersion.calledOnce).to.be.false;
    });
    it("runs install stack version", function() {
      mainStackVersionsDetailsController.reopen({'installInProgress': false});
      mainStackVersionsDetailsController.reopen({'allInstalled': false});
      mainStackVersionsDetailsController.installStackVersion({});
      expect(mainStackVersionsDetailsController.showProgressPopup.calledOnce).to.be.false;
      expect(mainStackVersionsDetailsController.doInstallStackVersion.calledOnce).to.be.true;
    });
    it("doesn't do anything", function() {
      mainStackVersionsDetailsController.reopen({'installInProgress': false});
      mainStackVersionsDetailsController.reopen({'allInstalled': true});
      mainStackVersionsDetailsController.installStackVersion({});
      expect(mainStackVersionsDetailsController.showProgressPopup.calledOnce).to.be.false;
      expect(mainStackVersionsDetailsController.doInstallStackVersion.calledOnce).to.be.false;
    });
  });

  describe('#showProgressPopup', function () {
    beforeEach(function() {
      sinon.stub(App.router.get('highAvailabilityProgressPopupController'), 'initPopup', Em.K);
      App.set('testMode', true);
    });
    afterEach(function() {
      App.router.get('highAvailabilityProgressPopupController').initPopup.restore();
      App.set('testMode', false);
    });
    it("runs initPopup", function() {
      mainStackVersionsDetailsController.reopen({'content': { 'version': "v1"}});
      var popupTitle = Em.I18n.t('admin.stackVersions.datails.install.hosts.popup.title').format("v1");
      var requestIds =[1];
      mainStackVersionsDetailsController.showProgressPopup();
      expect(App.router.get('highAvailabilityProgressPopupController').initPopup.calledWith(popupTitle, requestIds, mainStackVersionsDetailsController, true)).to.be.true;
    });
  });

  describe('#doInstallStackVersion', function () {
    beforeEach(function() {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(mainStackVersionsDetailsController, 'generateDataForInstall', Em.K);
    });
    afterEach(function() {
      App.ajax.send.restore();
      mainStackVersionsDetailsController.generateDataForInstall.restore();
    });
    it("runs initPopup", function() {
      mainStackVersionsDetailsController.doInstallStackVersion({});
      expect(mainStackVersionsDetailsController.generateDataForInstall.calledOnce).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

});