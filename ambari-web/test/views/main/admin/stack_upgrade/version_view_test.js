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
require('views/main/admin/stack_upgrade/upgrade_wizard_view');

describe('App.mainAdminStackVersionsView', function () {
  var view = App.MainAdminStackVersionsView.create({
    controller: {
      currentVersion: {
        repository_version: "2.2.1.0"
      }
    }
  });

  describe("#filterBy()", function () {
    var versions = [
      Em.Object.create({
        status: "INIT"
      }),
      Em.Object.create({
        status: "INSTALLING"
      }),
      Em.Object.create({
        status: "INSTALLED",
        repositoryVersion: "2.2.0.1"
      }),
      Em.Object.create({
        status: "INSTALLED",
        repositoryVersion: "2.2.2.1"
      }),
      Em.Object.create({
        status: "INSTALL_FAILED"
      }),
      Em.Object.create({
        status: "OUT_OF_SYNC"
      }),
      Em.Object.create({
        status: "UPGRADING"
      }),
      Em.Object.create({
        status: "UPGRADE_FAILED"
      }),
      Em.Object.create({
        status: "CURRENT"
      })
    ];

    var tets = [
      {
        filter:  Em.Object.create({}),
        filteredVersions: [
          Em.Object.create({
            status: "INIT"
          }),
          Em.Object.create({
            status: "INSTALLING"
          }),
          Em.Object.create({
            status: "INSTALLED",
            repositoryVersion: "2.2.0.1"
          }),
          Em.Object.create({
            status: "INSTALLED",
            repositoryVersion: "2.2.2.1"
          }),
          Em.Object.create({
            status: "INSTALL_FAILED"
          }),
          Em.Object.create({
            status: "OUT_OF_SYNC"
          }),
          Em.Object.create({
            status: "UPGRADING"
          }),
          Em.Object.create({
            status: "UPGRADE_FAILED"
          }),
          Em.Object.create({
            status: "CURRENT"
          })
        ]
      },
      {
        filter:  Em.Object.create({
          statuses: ["INIT", "INSTALLING", "INSTALL_FAILED", "OUT_OF_SYNC"]
        }),
        filteredVersions: [
          Em.Object.create({
            status: "INIT"
          }),
          Em.Object.create({
            status: "INSTALLING"
          }),
          Em.Object.create({
            status: "INSTALL_FAILED"
          }),
          Em.Object.create({
            status: "OUT_OF_SYNC"
          })
        ]
      },
      {
        filter:  Em.Object.create({
          statuses: ["INSTALLED"]
        }),
        filteredVersions: [
          Em.Object.create({
            status: "INSTALLED",
            repositoryVersion: "2.2.2.1"
          })
        ]
      },
      {
        filter:  Em.Object.create({
          statuses: ["CURRENT"]
        }),
        filteredVersions: [
          Em.Object.create({
            status: "CURRENT"
          })
        ]
      }
    ].forEach(function(t) {
        var msg = t.filter.get('statuses') ? t.filter.get('statuses').toString() : "All";
        it("filter By " + msg, function () {
          expect(view.filterBy(versions, t.filter)).to.eql(t.filteredVersions);
        });
      });

  });

});
