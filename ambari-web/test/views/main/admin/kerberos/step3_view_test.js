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
require('views/main/admin/kerberos/step3_view');

describe('App.KerberosWizardStep3View', function () {
  var view = App.KerberosWizardStep3View.create({
    controller: Em.Object.create({
      content: {},
      loadStep: Em.K
    })
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.spy(view.get('controller'), 'loadStep');
    });
    after(function () {
      view.get('controller').loadStep.restore();
    });
    it("call loadStep", function () {
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.be.true;
    });
  });

  describe("#isHostHeartbeatLost", function () {
    it("should return true", function () {
      view.set('controller.heartBeatLostHosts', Em.A(['host1', 'host2']));
      view.propertyDidChange('isHostHeartbeatLost');
      expect(view.get('isHostHeartbeatLost')).to.be.true;
    });
    it("should return false", function () {
      view.set('controller.heartBeatLostHosts', Em.A([]));
      view.propertyDidChange('isHostHeartbeatLost');
      expect(view.get('isHostHeartbeatLost')).to.be.false;
    });
  });

  describe("#resultMsg", function () {
    it("get message if isHostHeartbeatLost is true", function () {
      view.set('controller.heartBeatLostHosts', Em.A(['host1', 'host2']));
      view.set('isHostHeartbeatLost', true);
      view.propertyDidChange('resultMsg');
      expect(view.get('resultMsg')).to.be.equal(Em.I18n.t('installer.step9.status.hosts.heartbeat_lost').format(view.get('controller.heartBeatLostHosts.length')));
    });
    it("no message if isHostHeartbeatLost is false", function () {
      view.set('controller.heartBeatLostHosts', Em.A([]));
      view.set('isHostHeartbeatLost', false);
      view.propertyDidChange('resultMsg');
      expect(view.get('resultMsg')).to.be.equal('');
    });
  });

  describe("#showHostsWithLostHeartBeat", function () {
    before(function () {
      sinon.spy(App.ModalPopup, 'show');
    });
    after(function () {
      App.ModalPopup.show.restore();
    });
    it("should show modal popup", function () {
      view.set('controller.heartBeatLostHosts', Em.A(['host1', 'host2']));
      view.showHostsWithLostHeartBeat();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

});
