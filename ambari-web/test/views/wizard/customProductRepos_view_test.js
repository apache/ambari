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
      selected: false,
      mpacks: [
        Em.Object.create({
          id: '0-alpha',
          operatingSystems: [
            Em.Object.create({
              type: 'alpha',
              selected: false,
              isFirstSelected: false,
              isLastSelected: false
            }),
            Em.Object.create({
              type: 'bravo',
              selected: true,
              isFirstSelected: true,
              isLastSelected: true
            })
          ]
        }),
        Em.Object.create({
          id: '1-alpha',
          operatingSystems: [
            Em.Object.create({
              type: 'alpha',
              selected: false,
              isFirstSelected: false,
              isLastSelected: false
            }),
            Em.Object.create({
              type: 'bravo',
              selected: true,
              isFirstSelected: true,
              isLastSelected: true
            })
          ]
        })
      ]
    }),
    Em.Object.create({
      type: 'bravo',
      selected: true,
      mpacks: [
        Em.Object.create({
          id: '0-bravo',
          operatingSystems: [
            Em.Object.create({
              type: 'alpha',
              selected: false,
              isFirstSelected: false,
              isLastSelected: false
            }),
            Em.Object.create({
              type: 'bravo',
              selected: true,
              isFirstSelected: true,
              isLastSelected: true
            })
          ]
        }),
        Em.Object.create({
          id: '1-bravo',
          operatingSystems: [
            Em.Object.create({
              type: 'alpha',
              selected: false,
              isFirstSelected: false,
              isLastSelected: false
            }),
            Em.Object.create({
              type: 'bravo',
              selected: true,
              isFirstSelected: true,
              isLastSelected: true
            })
          ]
        })
      ]
    })
  ]
});
var view = App.WizardCustomProductReposView.create({ controller: controller, parentView: {} });

describe('App.WizardCustomProductReposView', function () {
  describe('#revertUrl', function () {
    it('Sets downloadUrl to the value of publicUrl', function () {
      var repo = Em.Object.create({
        id: 0,
        downloadUrl: 'downloadUrl',
        publicUrl: 'publicUrl'
      });
      sinon.stub(view.get('controller'), 'findRepoById').returns(repo);

      view.revertUrl({
        currentTarget: {
          value: 0
        }
      });

      expect(repo.get('downloadUrl')).to.equal(repo.get('publicUrl'));
    });
  });

  describe('#selectedOsChanged', function () {
    before(function () {
      view.set('parentView', view);
    });

    it('Correctly toggles the OS when it was previously unselected.', function () {
      view.get('controller.operatingSystems')[0].set('selected', true);
      view.selectedOsChanged({
        srcElement: {
          name: 'alpha'
        }
      });

      var actual = view.get('controller.operatingSystems');

      //checking that controller.toggleOs() worked
      expect(actual[0].get('mpacks')[0].get('operatingSystems')[0].get('selected')).to.be.true;
      expect(actual[0].get('mpacks')[1].get('operatingSystems')[0].get('selected')).to.be.true;

      //checking that view.updateOsState() worked
      expect(actual[0].get('mpacks')[0].get('operatingSystems')[0].get('isFirstSelected')).to.be.true;
      expect(actual[0].get('mpacks')[0].get('operatingSystems')[0].get('isLastSelected')).to.be.false;
      expect(actual[0].get('mpacks')[0].get('operatingSystems')[1].get('isFirstSelected')).to.be.false;
      expect(actual[0].get('mpacks')[0].get('operatingSystems')[1].get('isLastSelected')).to.be.true;

      expect(actual[0].get('mpacks')[1].get('operatingSystems')[0].get('isFirstSelected')).to.be.true;
      expect(actual[0].get('mpacks')[1].get('operatingSystems')[0].get('isLastSelected')).to.be.false;
      expect(actual[0].get('mpacks')[1].get('operatingSystems')[1].get('isFirstSelected')).to.be.false;
      expect(actual[0].get('mpacks')[1].get('operatingSystems')[1].get('isLastSelected')).to.be.true;
    });

    it('Correctly toggles the OS when it was previously selected.', function () {
      view.get('controller.operatingSystems')[1].set('selected', false);
      view.selectedOsChanged({
        srcElement: {
          name: 'bravo'
        }
      });

      var actual = view.get('controller.operatingSystems');

      //checking that controller.toggleOs() worked
      expect(actual[1].get('mpacks')[0].get('operatingSystems')[0].get('selected')).to.be.false;
      expect(actual[1].get('mpacks')[1].get('operatingSystems')[0].get('selected')).to.be.false;

      //checking that view.updateOsState() worked
      //actually, it will change nothing because nothing is selected for OS "bravo" now
      expect(actual[1].get('mpacks')[0].get('operatingSystems')[0].get('isFirstSelected')).to.be.false;
      expect(actual[1].get('mpacks')[0].get('operatingSystems')[0].get('isLastSelected')).to.be.false;
      expect(actual[1].get('mpacks')[0].get('operatingSystems')[1].get('isFirstSelected')).to.be.true;
      expect(actual[1].get('mpacks')[0].get('operatingSystems')[1].get('isLastSelected')).to.be.true;

      expect(actual[1].get('mpacks')[1].get('operatingSystems')[0].get('isFirstSelected')).to.be.false;
      expect(actual[1].get('mpacks')[1].get('operatingSystems')[0].get('isLastSelected')).to.be.false;
      expect(actual[1].get('mpacks')[1].get('operatingSystems')[1].get('isFirstSelected')).to.be.true;
      expect(actual[1].get('mpacks')[1].get('operatingSystems')[1].get('isLastSelected')).to.be.true;
    });

  });
});