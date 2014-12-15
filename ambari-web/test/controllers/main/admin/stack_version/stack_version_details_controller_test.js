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
      sinon.stub(mainStackVersionsDetailsController, 'installRepoVersion', Em.K);
    });
    afterEach(function() {
      mainStackVersionsDetailsController.showProgressPopup.restore();
      mainStackVersionsDetailsController.installRepoVersion.restore();
    });
    it("shows installing proggress", function() {
      mainStackVersionsDetailsController.reopen({'installInProgress': true});
      mainStackVersionsDetailsController.installStackVersion({});
      expect(mainStackVersionsDetailsController.showProgressPopup.calledOnce).to.be.true;
    });
    it("shows senq request to install/reinstall repoVersion", function() {
      mainStackVersionsDetailsController.reopen({'installFailed': true});
      mainStackVersionsDetailsController.installStackVersion({context: "1"});
      expect(mainStackVersionsDetailsController.installRepoVersion.calledWith({context: "1"})).to.be.true;
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
      mainStackVersionsDetailsController.reopen({'content': { 'repositoryVersion': {'displayName': "v1"}}});
      var popupTitle = Em.I18n.t('admin.stackVersions.datails.install.hosts.popup.title').format("v1");
      var requestIds =[1];
      mainStackVersionsDetailsController.showProgressPopup();
      expect(App.router.get('highAvailabilityProgressPopupController').initPopup.calledWith(popupTitle, requestIds, mainStackVersionsDetailsController)).to.be.true;
    });
  });
});
