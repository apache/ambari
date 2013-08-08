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

require('views/main/dashboard/widgets/hbase_regions_in_transition');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

describe('App.HBaseRegionsInTransitionView', function() {

  var tests = [
    {
      model: {
        regionsInTransition: 1
      },
      e: {
        isRed: false,
        isOrange: true,
        isGreen: false,
        isNA: false,
        content: '1'
      }
    },
    {
      model: {
        regionsInTransition: 10
      },
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false,
        isNA: false,
        content: '10'
      }
    },
    {
      model: {
        regionsInTransition: 0
      },
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: false,
        content: '0'
      }
    },
    {
      model: {
        regionsInTransition: null
      },
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: true,
        content: 'null'
      }
    }
  ];

  tests.forEach(function(test) {
    describe('regionsInTransition - ' + test.model.regionsInTransition, function() {
      var hBaseRegionsInTransitionView = App.HBaseRegionsInTransitionView.create({model_type:null, model: test.model});
      it('content', function() {
        expect(hBaseRegionsInTransitionView.get('content')).to.equal(test.e.content);
      });
      it('data', function() {
        expect(hBaseRegionsInTransitionView.get('data')).to.equal(test.model.regionsInTransition);
      });
      it('isRed', function() {
        expect(hBaseRegionsInTransitionView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(hBaseRegionsInTransitionView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(hBaseRegionsInTransitionView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function() {
        expect(hBaseRegionsInTransitionView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
