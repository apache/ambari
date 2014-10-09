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
require('models/cluster');
require('controllers/wizard');
require('controllers/installer');

describe('App.InstallerController', function () {

  var installerController = App.InstallerController.create();

  describe('#loadStacksVersionsSuccessCallback', function() {
    beforeEach(function () {
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function () {
      App.store.commit.restore();
    });
    it ('Correct data', function() {
      installerController.loadStacksVersionsSuccessCallback(require('test/stack'));
      expect(installerController.get('content.stacks.length')).to.equal(2);
      expect(installerController.get('content.stacks').everyProperty('isSelected')).to.equal(false);
      expect(installerController.get('content.stacks').mapProperty('id')).to.eql(['HDP-2.1','HDP-1.3']);
    });
  });
});
