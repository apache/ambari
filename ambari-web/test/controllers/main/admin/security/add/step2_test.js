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
require('controllers/main/admin/security/add/step2');
require('utils/polling');
require('models/cluster_states');

describe('App.MainAdminSecurityAddStep2Controller', function () {

  var mainAdminSecurityAddStep2Controller = App.MainAdminSecurityAddStep2Controller.create();

  describe('#clearStep', function() {
    mainAdminSecurityAddStep2Controller.set('stepConfigs',[1,2,3]);
    it('clear', function() {
      mainAdminSecurityAddStep2Controller.clearStep();
      expect(mainAdminSecurityAddStep2Controller.get('stepConfigs.length')).to.equal(0);
    });
  });

  describe('#isSubmitDisabled', function() {
    var tests = [
      {
        config:[
          {
            showConfig: true,
            errorCount: 0
          }
        ],
        m: 'All show configs, nothing with errors',
        e: false
      },
      {
        config:[
          {
            showConfig: true,
            errorCount: 0
          },
          {
            showConfig: true,
            errorCount: 1
          }
        ],
        m: 'All show configs, 1 with errors',
        e: true
      },
      {
        config:[
          {
            showConfig: true,
            errorCount: 0
          },
          {
            showConfig: false,
            errorCount: 1
          }
        ],
        m: '1 has errors but not visible',
        e: false
      },
      {
        config:[
          {
            showConfig: false,
            errorCount: 0
          },
          {
            showConfig: false,
            errorCount: 1
          }
        ],
        m: '1 has errors, all not visible',
        e: false
      },
      {
        config:[
          {
            showConfig: true,
            errorCount: 1
          },
          {
            showConfig: true,
            errorCount: 1
          }
        ],
        m: 'All has errors, all not visible',
        e: true
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        mainAdminSecurityAddStep2Controller.set('stepConfigs', test.config);
        expect(mainAdminSecurityAddStep2Controller.get('isSubmitDisabled')).to.equal(test.e);
      });
    });
  });

});
