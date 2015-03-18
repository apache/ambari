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
require('views/main/admin/stack_upgrade/upgrade_version_box_view');

describe('App.UpgradeVersionBoxView', function () {
  var view = App.UpgradeVersionBoxView.create({
    controller: Em.Object.create({
      upgrade: Em.K
    }),
    content: Em.Object.create()
  });

  describe("#isUpgrading", function () {
    afterEach(function () {
      App.set('upgradeState', 'INIT');
    });
    it("wrong version", function () {
      App.set('upgradeState', 'IN_PROGRESS');
      view.set('controller.upgradeVersion', 'HDP-2.2.1');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
    it("correct version", function () {
      App.set('upgradeState', 'IN_PROGRESS');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.true;
    });
    it("upgradeState INIT", function () {
      App.set('upgradeState', 'INIT');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.2');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
    it("upgradeState INIT and wrong version", function () {
      App.set('upgradeState', 'INIT');
      view.set('controller.upgradeVersion', 'HDP-2.2.2');
      view.set('content.displayName', 'HDP-2.2.1');
      view.propertyDidChange('isUpgrading');
      expect(view.get('isUpgrading')).to.be.false;
    });
  });

  describe("#installProgress", function () {
    beforeEach(function () {
      sinon.stub(App.db, 'get').returns(1);
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      App.db.get.restore();
      this.mock.restore();
    });
    it("request absent", function () {
      this.mock.returns([]);
      view.propertyDidChange('installProgress');
      expect(view.get('installProgress')).to.equal(0);
    });
    it("request present", function () {
      this.mock.returns([Em.Object.create({progress: 100})]);
      view.propertyDidChange('installProgress');
      expect(view.get('installProgress')).to.equal(100);
    });
  });

  describe("#versionClass", function () {
    it("status CURRENT", function () {
      view.set('content.status', 'CURRENT');
      view.propertyDidChange('versionClass');
      expect(view.get('versionClass')).to.equal('current-version-box');
    });
    it("status INSTALLED", function () {
      view.set('content.status', 'INSTALLED');
      view.propertyDidChange('versionClass');
      expect(view.get('versionClass')).to.equal('');
    });
  });

  describe("#isOutOfSync", function () {
    it("status OUT_OF_SYNC", function () {
      view.set('content.status', 'OUT_OF_SYNC');
      view.propertyDidChange('isOutOfSync');
      expect(view.get('isOutOfSync')).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(App, 'tooltip').returns(1);
    });
    afterEach(function () {
      App.tooltip.restore();
    });
    it("init tooltips", function () {
      view.didInsertElement();
      expect(App.tooltip.callCount).to.equal(3);
    });
  });

  describe("#runAction()", function () {
    beforeEach(function () {
      view.set('stateElement', Em.Object.create({}));
      sinon.stub(view.get('controller'), 'upgrade').returns(1);
    });
    afterEach(function () {
      view.get('controller').upgrade.restore();
    });
    it("action = null", function () {
      view.set('stateElement.action', null);
      view.runAction();
      expect(view.get('controller').upgrade.called).to.be.false;
    });
    it("action = 'upgrade'", function () {
      view.set('content', 'content');
      view.set('stateElement.action', 'upgrade');
      view.runAction();
      expect(view.get('controller').upgrade.calledWith('content')).to.be.true;
    });
  });

  describe("#editRepositories()", function () {
    beforeEach(function () {
      sinon.stub(App.RepositoryVersion, 'find').returns(Em.Object.create({
        operatingSystems: []
      }));
      sinon.stub(App.ModalPopup, 'show', Em.K);
    });
    afterEach(function () {
      App.RepositoryVersion.find.restore();
      App.ModalPopup.show.restore();
    });
    it("show popup", function () {
      view.editRepositories();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#showHosts()", function () {
    beforeEach(function () {
      sinon.spy(App.ModalPopup, 'show');
      sinon.stub(view, 'filterHostsByStack', Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      view.filterHostsByStack.restore();
    });
    it("no hosts", function () {
      view.set('content', Em.Object.create({
        p1: []
      }));
      view.showHosts({contexts: [
        {'property': 'p1'}
      ]});
      expect(App.ModalPopup.show.called).to.be.false;
    });
    it("one host", function () {
      view.set('content', Em.Object.create({
        p1: ['host1'],
        displayName: 'version'
      }));
      var popup = view.showHosts({contexts: [
        {id: 1, 'property': 'p1'}
      ]});
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(view.filterHostsByStack.calledWith('version', 1)).to.be.true;
    });
  });

  describe("#filterHostsByStack()", function () {
    var mock = {
      set: Em.K,
      filterByStack: Em.K
    };
    beforeEach(function () {
      sinon.stub(App.router, 'get').withArgs('mainHostController').returns(mock);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(mock, 'set');
      sinon.spy(mock, 'filterByStack');
    });
    afterEach(function () {
      App.router.get.restore();
      App.router.transitionTo.restore();
      mock.set.restore();
      mock.filterByStack.restore();
    });
    it("version and state are valid", function () {
      view.filterHostsByStack('version', 'state');
      expect(mock.set.calledWith('showFilterConditionsFirstLoad', true)).to.be.true;
      expect(mock.filterByStack.calledWith('version', 'state')).to.be.true;
      expect(App.router.transitionTo.calledWith('hosts.index')).to.be.true;
    });
    it("version is null", function () {
      view.filterHostsByStack(null, 'state');
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
    it("state is null", function () {
      view.filterHostsByStack('version', null);
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
    it("state and version are null", function () {
      view.filterHostsByStack(null, null);
      expect(mock.set.called).to.be.false;
      expect(mock.filterByStack.called).to.be.false;
      expect(App.router.transitionTo.called).to.be.false;
    });
  });


});