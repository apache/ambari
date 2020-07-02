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
require('views/wizard/step7/accounts_tab_view');
var view;

function getView() {
  return App.AccountsTabOnStep7View.create({
    controller: Em.Object.create({
      stepConfigs: Em.A([
        Em.Object.create({
          serviceName: 'MISC',
          configs: Em.A([
            Em.Object.create({ name: "sysprep_skip_create_users_and_groups", editDone: true, displayType: "user" }),
            Em.Object.create({ name: "ignore_groupsusers_create", editDone: false, displayType: "user" }),
            Em.Object.create({ name: "override_uid", editDone: true, displayType: "host2" })
          ])
        })
      ])
    })
  });
}

describe('App.AccountsTabOnStep7View', function () {

  beforeEach(function () {
    view = getView();
  });

  describe("#properties", function () {
    it("return properties with display type - user", function () {
      expect(view.get('properties').mapProperty('displayType')).to.eql(['user', 'user']);
    });
  });

  describe("#propertyChanged", function () {
    beforeEach(function() {
      sinon.spy(view, 'showConfirmationDialogIfShouldChangeProps');
    });

    Em.A([
      Em.Object.create({ serviceName: "sysprep_skip_create_users_and_groups", editDone: true, displayType: "user" }),
      Em.Object.create({ serviceName: "ignore_groupsusers_create", editDone: false, displayType: "user" })
    ]).forEach(function(test) {
      it(test.serviceName, function() {
        view.reopen({
          properties: Em.A([Em.Object.create(test)])
        });
        view.propertyChanged();
        if (test.editDone) {
          expect(view.showConfirmationDialogIfShouldChangeProps.calledOnce).to.be.true;
        } else {
          expect(view.showConfirmationDialogIfShouldChangeProps.calledOnce).to.be.false;
        }
      });
    });
  });

  describe("#checkboxes", function () {
    it("return checkboxes names", function () {
      expect(view.get('checkboxes').mapProperty('name')).to.eql(["sysprep_skip_create_users_and_groups", "ignore_groupsusers_create", "override_uid"]);
    });
  });

});