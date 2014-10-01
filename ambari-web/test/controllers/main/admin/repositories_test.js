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
require('controllers/main/admin/repositories');

describe('App.MainAdminRepositoriesController', function() {
  var controller;
  var data = {
    "items": [
      {
        "Versions": {
          "stack_version": "1.3.1",
          "min_upgrade_version": "1.2.0"
        }
      },
      {
        "Versions": {
          "stack_version": "1.3.0",
          "min_upgrade_version": "1.2.0"
        }
      },
      {
        "Versions": {
          "stack_version": "1.2.2",
          "min_upgrade_version": "1.2.0"
        }
      },
      {
        "Versions": {
          "stack_version": "1.2.0",
          "min_upgrade_version": "1.2.0"
        }
      },
      {
        "Versions": {
          "stack_version": "2.0.5",
          "min_upgrade_version": "2.0.0"
        }
      },
      {
        "Versions": {
          "stack_version": "2.0.5",
          "min_upgrade_version": "2.0.5"
        }
      }
    ]
  };

  beforeEach(function() {
    controller = App.MainAdminRepositoriesController.create({
      parseServicesInfo: Em.K
    });
  });

  //todo should be verified
  describe('#updateUpgradeVersionSuccessCallback()', function () {
    it('upgrade version of stack should be "HDP-1.2.2"', function () {
      App.set('currentStackVersion', 'HDP-1.2.2');
      controller.updateUpgradeVersionSuccessCallback.call(controller, data);
      expect(controller.get('upgradeVersion')).to.equal('HDP-1.2.2');
    });
    it('if min upgrade version less then current then upgrade version equal current', function () {
      App.set('currentStackVersion', 'HDP-1.2.2');
      data.items[0].Versions.min_upgrade_version = "1.2.3";
      controller.updateUpgradeVersionSuccessCallback.call(controller, data);
      expect(controller.get('upgradeVersion')).to.equal('HDP-1.2.2');
    })
  });
});
