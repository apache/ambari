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
var controller = App.WizardConfigureDownloadController.create();
var view = App.WizardConfigureDownloadView.create({ controller: controller });

describe('App.WizardConfigureDownloadView', function () {

  beforeEach(function () {
    controller.set('content', {
      downloadConfig: {
        useRedHatSatellite: false,
        useCustomRepo: false,
        useProxy: false,
        proxyUrl: null,
        proxyAuth: null,
        proxyTestPassed: false
      }
    });
  })

  describe('#useRedHatSatelliteChanged', function () {
    it('Sets useProxy to false, calls useProxyChanged, and shows modal', function () {
      controller.set('content.downloadConfig.useProxy', true);
      sinon.stub(view, 'useProxyChanged');
      sinon.stub(App.ModalPopup, 'show');
      
      view.useRedHatSatelliteChanged({
        currentTarget: {
          checked: true
        }
      });

      expect(controller.get('content.downloadConfig.useProxy')).to.be.false;
      expect(view.useProxyChanged.called).to.be.true;
      expect(App.ModalPopup.show.called).to.be.true;

      view.useProxyChanged.restore();
      App.ModalPopup.show.restore();
    });
  });

  describe('#useProxyChanged', function () {
    it('Sets proxyUrl to null, calls setProxyAuth, and calls proxySettingsChanged on the controller', function () {
      controller.set('content.downloadConfig.proxyUrl', 'not null');
      controller.set('content.downloadConfig.proxyAuth', 1);
      sinon.stub(view, 'setProxyAuth');
      sinon.stub(controller, 'proxySettingsChanged');

      view.useProxyChanged();

      expect(controller.get('content.downloadConfig.proxyUrl')).to.equal(null);
      expect(view.setProxyAuth.called).to.be.true;
      expect(controller.proxySettingsChanged.called).to.be.true;

      view.setProxyAuth.restore();
      controller.proxySettingsChanged.restore();
    });
  });

  describe('#proxyUrlChanged', function () {
    it('Calls proxySettingsChanged on the controller', function () {
      sinon.stub(controller, 'proxySettingsChanged');
      
      view.proxyUrlChanged();
      
      expect(controller.proxySettingsChanged.called).to.be.true;
      
      controller.proxySettingsChanged.restore();
    });
  });

  describe('#setProxyAuth', function () {
    it('Sets the selected proxy authentication option in both the view and the controller', function () {
      var value = 1;
      sinon.stub(controller, 'setProxyAuth');

      view.setProxyAuth(value);

      var selected = view.get('proxyAuthOptions').filterProperty('selected');

      expect(selected.length).to.equal(1);
      expect(selected[0].get('value')).to.equal(value);
      expect(controller.setProxyAuth.called).to.be.true;

      controller.setProxyAuth.restore();
    });

    it('Sets the selected proxy authentication option in just the view', function () {
      var value = 1;
      sinon.stub(controller, 'setProxyAuth');

      view.setProxyAuth(value, true);

      var selected = view.get('proxyAuthOptions').filterProperty('selected');

      expect(selected.length).to.equal(1);
      expect(selected[0].get('value')).to.equal(value);
      expect(controller.setProxyAuth.called).to.be.false;
      
      controller.setProxyAuth.restore();
    });
  });

  describe('#proxyAuthChanged', function () {
    it('Calls setProxyAuth() with the correct value and calls proxySettingsChanged on the controller', function () {
      var value = 1;
      var event = { target: { value: value } };

      sinon.stub(view, 'setProxyAuth');
      sinon.stub(controller, 'proxySettingsChanged');

      view.proxyAuthChanged(event);

      expect(view.setProxyAuth.calledWith(value)).to.be.true;
      expect(controller.proxySettingsChanged.called).to.be.true;

      view.setProxyAuth.restore();
      controller.proxySettingsChanged.restore();
    });
  });

});