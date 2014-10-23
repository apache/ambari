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
require('views/main/menu');

var mainMenuView = App.MainMenuView.create();
describe('App.MainMenuView', function () {

  describe('#content', function () {
    var supportsMirroring;
    beforeEach(function () {
      supportsMirroring = Em.get('App.supports.mirroring');
      Em.set('App.supports.mirroring', false);
      sinon.stub(App, 'get').returns(false);
      sinon.stub(App.router, 'get')
        .withArgs('clusterController.isLoaded').returns(true)
        .withArgs('loggedIn').returns(true);
    });
    afterEach(function () {
      Em.set('App.supports.mirroring', supportsMirroring);
      App.get.restore();
      App.router.get.restore();
    });

    it('menu should be populated if cluster installation is completed', function () {
      App.get.withArgs('router.clusterInstallCompleted').returns(true);
      App.router.notifyPropertyChange('clusterInstallCompleted');
      expect(mainMenuView.get('content').length > 0).to.be.true;
    });

    it('menu should not be populated if cluster installation is not completed', function () {
      App.get.withArgs('router.clusterInstallCompleted').returns(false);
      App.router.notifyPropertyChange('clusterInstallCompleted');
      expect(mainMenuView.get('content').length > 0).to.be.false;
    });

  });
});
