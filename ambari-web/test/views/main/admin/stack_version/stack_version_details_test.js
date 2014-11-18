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
require('views/main/admin/stack_versions/stack_version_view');
var mainStackVersionsDetailsView;

describe('App.MainStackVersionsDetailsView', function () {

  beforeEach(function () {
    mainStackVersionsDetailsView = App.MainStackVersionsDetailsView.create();
  });

  describe('#statusClass', function () {
    var tests = [
      {
        status: "ALL_INSTALLED",
        buttonClass: 'disabled'
      },
      {
        status: "INSTALL",
        buttonClass: 'btn-success'
      },
      {
        status: "INSTALLING",
        buttonClass: 'btn-primary'
      }
    ].forEach(function(t) {
      it("status is " + t.status + " class is " + t.buttonClass, function() {
        mainStackVersionsDetailsView.reopen({'status': t.status});
        expect(mainStackVersionsDetailsView.get('statusClass')).to.equal(t.buttonClass);
      });
    });
  });
});
