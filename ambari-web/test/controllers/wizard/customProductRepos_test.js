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
var controller = App.WizardCustomProductReposController.create({
  operatingSystems: [
    Em.Object.create({
      type: 'alpha',
      selected: true
    }),
    Em.Object.create({
      type: 'bravo',
      selected: false
    })
  ],
  mpacks: [
    {
      operatingSystems: [
        Em.Object.create({
          type: 'charlie',
          repos: [
            Em.Object.create({
              id: 0,
              expected: true
            }),
            Em.Object.create({
              id: 1,
              expected: false
            })
          ]
        })
      ]
    }
  ]
});

describe('App.WizardCustomProductReposController', function () {
  describe('#isOsSelected', function () {
    it('Correctly reports whether an OS is selected', function () {
      expect(controller.isOsSelected('alpha')).to.be.true;
      expect(controller.isOsSelected('bravo')).to.be.false;
      expect(controller.isOsSelected('charle')).to.be.false;
    });
  });  

  describe('#anySelectedOs', function () {
    it('Correctly reports whether any OS is selected', function () {
      controller.get('operatingSystems')[0].set('selected', false);
      expect(controller.get('anySelectedOs')).to.be.false;

      controller.get('operatingSystems')[0].set('selected', true);
      expect(controller.get('anySelectedOs')).to.be.true;
    });
  });  

  describe('#findRepoById', function () {
    //cannot be tested because the code under test uses for..of loops, so left it pending for now
    it('Returns the repo with the given id');
    //here's the test for when we can test it:
    //function () {
    //  var actual = controller.findRepoById(0);
    //  expect(actual.get('expected')).to.be.true;
    //} 
  });  
});