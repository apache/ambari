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
var model;

describe('App.Section', function () {

  beforeEach(function () {
    model = App.Section.createRecord();
  });

  describe('#errorsCount', function () {

    beforeEach(function () {
      model.reopen({subSections: [
        App.SubSection.createRecord({configs: [
          App.ServiceConfigProperty.create({isValid: true}),
          App.ServiceConfigProperty.create({isValid: false})
        ]}),
        App.SubSection.createRecord({configs: [
          App.ServiceConfigProperty.create({isValid: true}),
          App.ServiceConfigProperty.create({isValid: false})
        ]})
      ]});
    });

    it('should use subsections.@each.errorsCount', function () {
      expect(model.get('errorsCount')).to.equal(2);
    });

  });

  describe('#isHiddenByFilter', function () {

    Em.A([
        {
          subSections: [],
          m: 'no subsections',
          e: true
        },
        {
          subSections: [
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: false, isVisible: true})]}),
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: false, isVisible: true})]})
          ],
          m: 'no subsections are hidden',
          e: false
        },
        {
          subSections: [
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: true, isVisible: false, hiddenBySection: false}), Em.Object.create({isHiddenByFilter: true, isVisible: true, hiddenBySection: true})]})
          ],
          m: 'no subsections are hidden (hiddenBySection)',
          e: false
        },
        {
          subSections: [
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: true, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})]}),
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: false, isVisible: true}), Em.Object.create({isHiddenByFilter: false, isVisible: true})]})
          ],
          m: 'one subsection is hidden',
          e: false
        },
        {
          subSections: [
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: true, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})]}),
            App.SubSection.createRecord({configs: [Em.Object.create({isHiddenByFilter: true, isVisible: true}), Em.Object.create({isHiddenByFilter: true, isVisible: true})]})
          ],
          m: 'all subsections are hidden',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          model.reopen({
            subSections: test.subSections
          });
          expect(model.get('isHiddenByFilter')).to.equal(test.e);
        });
      });

  });

});
