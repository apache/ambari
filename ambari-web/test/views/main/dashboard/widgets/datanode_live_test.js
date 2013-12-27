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

require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/datanode_live');

describe('App.DataNodeUpView', function() {

  var tests = [
    {
      data: 100,
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true
      }
    },
    {
      data: 0,
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false
      }
    },
    {
      data: 50,
      e: {
        isRed: false,
        isOrange: true,
        isGreen: false
      }
    }
  ];

  tests.forEach(function(test) {
    describe('', function() {
      var dataNodeUpView = App.DataNodeUpView.create({model_type:null, data: test.data, content: test.data.toString()});
      it('isRed', function() {
        expect(dataNodeUpView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(dataNodeUpView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(dataNodeUpView.get('isGreen')).to.equal(test.e.isGreen);
      });
    });
  });

});
