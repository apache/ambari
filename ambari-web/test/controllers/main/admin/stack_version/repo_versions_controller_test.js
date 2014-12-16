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
require('controllers/main/admin/stack_versions/repo_versions_controller');
var repoVersionsController;

describe('App.RepoVersionsController', function () {

  beforeEach(function () {
    repoVersionsController = App.RepoVersionsController.create();
  });

  describe('#installRepoVersion', function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("runs post request to create stack version", function () {
      var repoVersion = Em.Object.create({
        stackVersionType: "HDP",
        stackVersionNumber: "2.2",
        repositoryVersion: "2.2.0.1"
      });
      repoVersionsController.installRepoVersion({context: repoVersion});
      expect(App.ajax.send.getCall(0).args[0].data.ClusterStackVersions).to.deep.eql({
        "stack": "HDP",
        "version": "2.2",
        "repository_version": "2.2.0.1"
      });
    });
  });

  describe('#load', function () {
    it('loads data by running loadRepoVersionsToModel', function () {
      sinon.stub(repoVersionsController, 'loadRepoVersionsToModel').returns({done: Em.K});
      sinon.stub(App.get('router.mainStackVersionsController'), 'loadStackVersionsToModel', function() { return $.Deferred().resolve()});
      repoVersionsController.load();
      expect(repoVersionsController.loadRepoVersionsToModel.calledOnce).to.be.true;
      expect(App.get('router.mainStackVersionsController').loadStackVersionsToModel.calledOnce).to.be.true;
      repoVersionsController.loadRepoVersionsToModel.restore();
      App.get('router.mainStackVersionsController').loadStackVersionsToModel.restore();
    });
  });
  describe('#loadRepoVersionsToModel()', function () {
    it('loads data to model', function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(repoVersionsController, 'getUrl', Em.K);

      repoVersionsController.loadRepoVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(repoVersionsController.getUrl.calledOnce).to.be.true;

      repoVersionsController.getUrl.restore();
      App.HttpClient.get.restore();
    });
  });

  describe('#installStackVersionSuccess()', function () {
    var repoId = "1";
    var requestId = "2";
    var stackVersionObject = {repositoryVersion: {id: repoId}};
    var stackVersion;
    beforeEach(function() {
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(App.StackVersion, 'find', function() {
        return [stackVersion];
      });
    });

    afterEach(function() {
      App.db.set.restore();
      App.router.transitionTo.restore();
      App.StackVersion.find.restore();
    });
    it('success callback for install stack version', function () {
      stackVersion = null;
      sinon.stub(App.get('router.mainStackVersionsController'), 'loadStackVersionsToModel', function() {
        stackVersion = stackVersionObject;
        return $.Deferred().resolve()});

      repoVersionsController.installStackVersionSuccess({Requests: {id: requestId}}, null, {id: repoId});
      expect(App.db.set.calledWith('repoVersion', 'id', [requestId])).to.be.true;
      expect(App.get('router.mainStackVersionsController').loadStackVersionsToModel.calledOnce).to.be.true;
      expect(App.StackVersion.find.called).to.be.true;
      expect(App.router.transitionTo.calledWith('main.admin.adminStackVersions.version', stackVersion)).to.be.true;

      App.get('router.mainStackVersionsController').loadStackVersionsToModel.restore();
    });

    it('success callback for install stack version without redirect', function () {
      stackVersion = stackVersionObject;
      sinon.stub(App.get('router.mainStackVersionsController'), 'loadStackVersionsToModel', function() {
        return $.Deferred().resolve()
      });

      repoVersionsController.installStackVersionSuccess({Requests: {id: requestId}}, null, {id: repoId});
      expect(App.db.set.calledWith('repoVersion', 'id', [requestId])).to.be.true;
      expect(App.get('router.mainStackVersionsController').loadStackVersionsToModel.calledOnce).to.be.false;
      expect(App.StackVersion.find.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('main.admin.adminStackVersions.version', stackVersion)).to.be.false;

      App.get('router.mainStackVersionsController').loadStackVersionsToModel.restore();

    });
  });


});
