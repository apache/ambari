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
require('views/main/admin/stack_versions/stack_version_view');
var view;

describe('App.MainStackVersionsDetailsView', function () {

  beforeEach(function () {
    view = App.MainStackVersionsDetailsView.create({
      "controller": {
        "hostsToInstall" : 0,
        "progress": 0,
        "doPolling": Em.K
      },
      "content": {
        "stackVersion":
          {
            "state" : "ANY"
          }
      }
    });
  });

  describe('#installButtonMsg', function () {
    it("install button msg for init state" , function() {
      view.set("controller.hostsToInstall", 2);
      view.set("content.stackVersion.state", "ANY");
      expect(view.get('installButtonMsg')).to.equal(Em.I18n.t('admin.stackVersions.details.hosts.btn.install').format(2))
    });

    it("install button msg for install failed state" , function() {
      view.set("content.stackVersion.state", "INSTALL_FAILED");
      expect(view.get('installButtonMsg')).to.equal(Em.I18n.t('admin.stackVersions.details.hosts.btn.reinstall'))
    });
  });

  describe('#installButtonClass', function () {
    it("install button class for init state" , function() {
      view.set("content.stackVersion.state", "ANY");
      expect(view.get('installButtonClass')).to.equal('btn-success')
    });

    it("install button class install failed state" , function() {
      view.set("content.stackVersion.state", "INSTALL_FAILED");
      expect(view.get('installButtonClass')).to.equal('btn-danger')
    });
  });

  describe('#progress', function () {
    it("this that is used as width of progress bar" , function() {
      view.set("controller.progress", 20);
      expect(view.get('progress')).to.equal('width:20%');
    });
  });

  describe('#showCounters', function () {
    it("true when repo version has cluster stack version" , function() {
      view.set("content.stackVersion", Em.Object.create({}));
      expect(view.get('showCounters')).to.be.true;
    });
    it("false when repo version has no cluster stack version" , function() {
      view.set("content.stackVersion", null);
      expect(view.get('showCounters')).to.be.false;
    });
  });

  describe('#didInsertElement', function () {
    beforeEach(function() {
      sinon.stub(App.get('router.mainStackVersionsController'), 'set', Em.K);
      sinon.stub(App.get('router.mainStackVersionsController'), 'load', Em.K);
      sinon.stub(App.get('router.mainStackVersionsController'), 'doPolling', Em.K);
      sinon.stub(view.get('controller'), 'doPolling', Em.K);
      sinon.stub(App.RepositoryVersion, 'find', function() {
        return [{id: 1}]
      });
    });
    afterEach(function() {
      App.get('router.mainStackVersionsController').set.restore();
      App.get('router.mainStackVersionsController').load.restore();
      App.get('router.mainStackVersionsController').doPolling.restore();
      view.get('controller').doPolling.restore();
      App.RepositoryVersion.find.restore();
    });
    it("runs polling and load when view is in dom" , function() {
      view.set('content.id', 2);
      view.didInsertElement();
      expect(App.get('router.mainStackVersionsController').set.calledWith('isPolling', true)).to.be.true;
      expect(App.get('router.mainStackVersionsController').load.calledOnce).to.be.true;
      expect(App.get('router.mainStackVersionsController').doPolling.calledOnce).to.be.true;
      expect(view.get('controller').doPolling.calledOnce).to.be.true;
    });

    it("runs polling when view is in dom" , function() {
      view.set('content.id', 1);
      view.didInsertElement();
      expect(App.get('router.mainStackVersionsController').set.calledWith('isPolling', true)).to.be.true;
      expect(App.get('router.mainStackVersionsController').load.calledOnce).to.be.false;
      expect(App.get('router.mainStackVersionsController').doPolling.calledOnce).to.be.true;
      expect(view.get('controller').doPolling.calledOnce).to.be.true;
    });
  });

  describe('#willDestroyElement', function () {
    beforeEach(function() {
      sinon.stub(App.get('router.mainStackVersionsController'), 'set', Em.K);
    });
    afterEach(function() {
      App.get('router.mainStackVersionsController').set.restore();
    });
    it("runs polling when view is in dom" , function() {
      view.willDestroyElement();
      expect(App.get('router.mainStackVersionsController').set.calledWith('isPolling', false)).to.be.true;
    });
  });
});
