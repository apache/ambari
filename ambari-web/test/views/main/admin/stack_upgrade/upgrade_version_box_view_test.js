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
    content: Em.K
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
});