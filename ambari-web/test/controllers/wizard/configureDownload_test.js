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

describe('App.WizardConfigureDownloadController', function () {

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

  describe('#usePublicRepo', function () {
    it('Sets useCustomRepo and useRedHatSatellite to false', function () {
      controller.set('content.downloadConfig.useCustomRepo', true);
      controller.set('content.downloadConfig.useRedHatSatellite', true);
      
      controller.usePublicRepo();

      expect(controller.get('content.downloadConfig.useCustomRepo')).to.be.false;
      expect(controller.get('content.downloadConfig.useRedHatSatellite')).to.be.false;
    });
  });

  describe('#useCustomRepo', function () {
    it('Sets useCustomRepo to true', function () {
      controller.useCustomRepo();

      expect(controller.get('content.downloadConfig.useCustomRepo')).to.be.true;
    });
  });

  describe('#setProxyAuth', function () {
    it('Sets proxyAuth to the value passed and calls proxySettingsChanged', function () {
      var value = 1;
      sinon.stub(controller, 'proxySettingsChanged');

      controller.setProxyAuth(value);

      expect(controller.get('content.downloadConfig.proxyAuth')).to.equal(value);
      expect(controller.proxySettingsChanged.called).to.be.true;

      controller.proxySettingsChanged.restore();
    });
  });

  describe('#proxySettingsChanged', function () {
    it('Sets proxyTestPassed to false', function () {
      controller.set('content.downloadConfig.proxyTestPassed', true);
    
      controller.proxySettingsChanged();
      
      expect(controller.get('content.downloadConfig.proxyTestPassed')).to.be.false;
    });
  });

  describe('#proxyTest', function () {
    it('Should test the proxy connection');
  });
  
});