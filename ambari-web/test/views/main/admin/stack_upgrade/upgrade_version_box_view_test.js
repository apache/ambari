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
    })
  });

  describe("#versionName", function () {
    it("version is null", function () {
      view.set('version', null);
      view.propertyDidChange('versionName');
      expect(view.get('versionName')).to.be.empty;
    });
    it("version is loaded", function () {
      view.set('version', Em.Object.create({
        stack: 'HDP',
        version: '2.2'
      }));
      view.propertyDidChange('versionName');
      expect(view.get('versionName')).to.equal('HDP-2.2');
    });
  });

  describe("#runAction()", function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'upgrade');
    });
    afterEach(function () {
      view.get('controller').upgrade.restore();
    });
    it("call upgrade()", function () {
      expect(view.runAction({context: 'upgrade'})).to.be.true;
      expect(view.get('controller').upgrade.calledOnce).to.be.true;
    });
    it("no method should be called", function () {
      expect(view.runAction({context: null})).to.be.false;
      expect(view.get('controller').upgrade.called).to.be.false;
    });
  });
});