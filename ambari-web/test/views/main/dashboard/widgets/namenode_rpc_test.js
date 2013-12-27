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

require('messages');
require('views/main/dashboard/widgets/namenode_rpc');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

describe('App.NameNodeRpcView', function() {

  var tests = [
    {
      model: {
        nameNodeRpc: 1
      },
      e: {
        isRed: false,
        isOrange: true,
        isGreen: false,
        isNA: false,
        content: '1.00 ms',
        data: '1.00'
      }
    },
    {
      model: {
        nameNodeRpc: 10
      },
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false,
        isNA: false,
        content: '10.00 ms',
        data: '10.00'
      }
    },
    {
      model: {
        nameNodeRpc: 0
      },
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: false,
        content: '0 ms',
        data: 0
      }
    },
    {
      model: {
        nameNodeRpc: null
      },
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    }
  ];

  tests.forEach(function(test) {
    describe('nameNodeRpc - ' + test.model.nameNodeRpc, function() {
      var jobTrackerRpcView = App.NameNodeRpcView.create({model_type:null, model: test.model});
      it('content', function() {
        expect(jobTrackerRpcView.get('content')).to.equal(test.e.content);
      });
      it('data', function() {
        expect(jobTrackerRpcView.get('data')).to.equal(test.e.data);
      });
      it('isRed', function() {
        expect(jobTrackerRpcView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(jobTrackerRpcView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(jobTrackerRpcView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function() {
        expect(jobTrackerRpcView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
