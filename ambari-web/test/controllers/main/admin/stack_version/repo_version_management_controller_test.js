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
require('controllers/main/admin/stack_versions/stack_versions_controller');
var controller;

describe('App.RepoVersionsManagementController', function () {

  beforeEach(function () {
    controller = App.RepoVersionsManagementController.create({});
  });

  describe('#getUrl()', function () {
    beforeEach(function() {
      controller.reopen({
        'mockStackUrl': 'mockStackUrl',
        'mockRepoUrl': 'mockRepoUrl',
        'realStackUrl': 'realStackUrl',
        'realRepoUrl': 'realRepoUrl',
        'realUpdateUrl': 'realUpdateUrl'
      });
    });
    afterEach(function() {
      App.set('testMode', false);
    });

    it('gets url for testMode for stackVersion', function () {
      App.set('testMode', true);
      expect(controller.getUrl(true)).to.be.equal('mockStackUrl');
    });
    it('gets url for testMode for repoVersion', function () {
      App.set('testMode', true);
      expect(controller.getUrl(false)).to.equal('mockRepoUrl');
    });
    it('gets url for stackVersion', function () {
      App.set('testMode', false);
      expect(controller.getUrl(true, true)).to.be.equal('realStackUrl');
    });
    it('gets url for repoVersion', function () {
      App.set('testMode', false);
      expect(controller.getUrl(false, true)).to.be.equal('realRepoUrl');
    });
    it('gets url to upadte stackVersion', function () {
      App.set('testMode', false);
      expect(controller.getUrl(true)).to.be.equal('realUpdateUrl');
    });

  });

  describe('#load()', function () {
    it('loads data to model by running loadStackVersionsToModel', function () {
      sinon.stub(controller, 'loadStackVersionsToModel').returns($.Deferred().resolve());
      sinon.stub(controller, 'loadRepoVersionsToModel').returns($.Deferred().resolve());

      controller.load();
      expect(controller.loadStackVersionsToModel.calledWith(true)).to.be.true;
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;

      controller.loadStackVersionsToModel.restore();
      controller.loadRepoVersionsToModel.restore();
    });
  });

  describe('#loadRepoVersionsToModel()', function () {
    it('loads data to model', function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(controller, 'getUrl', Em.K);

      controller.loadRepoVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(controller.getUrl.calledWith(false, true)).to.be.true;

      controller.getUrl.restore();
      App.HttpClient.get.restore();
    });
  });

  describe('#loadStackVersionsToModel()', function () {
    beforeEach(function() {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(controller, 'getUrl', Em.K);
    });

    afterEach(function() {
      controller.getUrl.restore();
      App.HttpClient.get.restore();
    });
    it('loads all data to model', function () {
      controller.loadStackVersionsToModel(true);
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(controller.getUrl.calledWith(true, true)).to.be.true;
    });

    it('loads update data to model', function () {
      controller.loadStackVersionsToModel(false);
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(controller.getUrl.calledWith(true, false)).to.be.true;
    });
  });

  describe('#filterHostsByStack()', function () {
    beforeEach(function() {
      sinon.stub(App.router.get('mainHostController'), 'filterByStack', Em.K);
      sinon.stub(App.router, 'transitionTo', Em.K);
    });
    afterEach(function() {
      App.router.get('mainHostController').filterByStack.restore();
      App.router.transitionTo.restore();
    });
    var tests = [
      {
        version: "version1",
        state: "state1",
        m: 'go to hosts filtered by host stack version and host stack state',
        runAll: true
      },
      {
        version: null,
        state: "state1",
        m: 'doesn\'t do anything because version is missing'
      },
      {
        version: "version1",
        state: null,
        m: 'doesn\'t do anything because state is missing'
      }
    ].forEach(function(t) {
        it(t.m, function () {
          controller.filterHostsByStack(t.version, t.state);
          if (t.runAll) {
            expect(App.router.get('mainHostController').filterByStack.calledWith(t.version, t.state)).to.be.true;
            expect(App.router.transitionTo.calledWith('hosts.index')).to.be.true;
          } else {
            expect(App.router.get('mainHostController').filterByStack.calledOnce).to.be.false;
            expect(App.router.transitionTo.calledOnce).to.be.false;
          }

        });
      });
  });

  describe('#showHosts()', function () {
    it('show list of hosts for current version in choosen state', function () {
      sinon.stub(controller, 'filterHostsByStack', Em.K);

      controller.showHosts({ contexts: [{id: "state", label: "label"}, "version",["host"]]}).onPrimary();
      expect(controller.filterHostsByStack.calledWith("version", "state")).to.be.true;

      controller.filterHostsByStack.restore();
    });
  });
});