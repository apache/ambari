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

require('controllers/main/admin/security/add/step1');
require('models/service');
var controller;
describe('App.MainAdminSecurityAddStep1Controller', function () {

  beforeEach(function () {
    controller = App.MainAdminSecurityAddStep1Controller.create({
      content: {}
    });
  });

  describe('#shouldRemoveATS()', function () {

    var tests = Em.A([
      {
        doesATSSupportKerberos: true,
        isATSInstalled: true,
        e: false
      },
      {
        doesATSSupportKerberos: true,
        isATSInstalled: false,
        e: false
      },
      {
        doesATSSupportKerberos: false,
        isATSInstalled: true,
        e: true
      },
      {
        doesATSSupportKerberos: false,
        isATSInstalled: false,
        e: false
      }
    ]);

    tests.forEach(function (test) {
      it('', function () {
        controller.set('content.isATSInstalled', test.isATSInstalled);
        sinon.stub(App, 'get', function (k) {
          if ('doesATSSupportKerberos' === k) return test.doesATSSupportKerberos;
          return Em.get(App, k);
        });
        var result = controller.shouldRemoveATS();
        App.get.restore();
        expect(result).to.equal(test.e);
      });
    });

  });
});
