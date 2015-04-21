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
require('views/common/chart/pie');
require('views/common/configs/services_config');

describe('App.ServiceConfigsByCategoryView', function () {

  var view = App.ServiceConfigsByCategoryView.create({
    serviceConfigs: []
  });

  var result = [1, 2, 3, 4];

  var testData = [
    {
      title: 'four configs in correct order',
      configs: [
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 4, resultId: 4})
      ]
    },
    {
      title: 'four configs in reverse order',
      configs: [
        Em.Object.create({index: 4, resultId: 4}),
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 1, resultId: 1})
      ]
    },
    {
      title: 'four configs in random order',
      configs: [
        Em.Object.create({index: 3, resultId: 3}),
        Em.Object.create({index: 4, resultId: 4}),
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2})
      ]
    },
    {
      title: 'four configs with no index',
      configs: [
        Em.Object.create({resultId: 1}),
        Em.Object.create({resultId: 2}),
        Em.Object.create({resultId: 3}),
        Em.Object.create({resultId: 4})
      ]
    },
    {
      title: 'four configs but one with index',
      configs: [
        Em.Object.create({resultId: 2}),
        Em.Object.create({resultId: 3}),
        Em.Object.create({resultId: 4}),
        Em.Object.create({index: 1, resultId: 1})
      ]
    },
    {
      title: 'index is null or not number',
      configs: [
        Em.Object.create({index: null, resultId: 3}),
        Em.Object.create({index: 1, resultId: 1}),
        Em.Object.create({index: 2, resultId: 2}),
        Em.Object.create({index: 'a', resultId: 4})
      ]
    },
    {
      title: 'four configs when indexes skipped',
      configs: [
        Em.Object.create({index: 88, resultId: 3}),
        Em.Object.create({index: 67, resultId: 2}),
        Em.Object.create({index: 111, resultId: 4}),
        Em.Object.create({index: 3, resultId: 1})
      ]
    }
  ];

  describe('#sortByIndex', function () {
    testData.forEach(function(_test){
      it(_test.title, function () {
        expect(view.sortByIndex(_test.configs).mapProperty('resultId')).to.deep.equal(result);
      })
    })
  });

  describe('#filteredCategoryConfigs', function () {

    var view,
      cases = [
        {
          filter: '',
          serviceConfigs: [],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: true,
          title: 'no filter, empty category, initial rendering'
        },
        {
          filter: '',
          serviceConfigs: [],
          propertyToChange: 'categoryConfigs',
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'no filter, new property added'
        },
        {
          filter: '',
          serviceConfigs: [
            Em.Object.create({
              category: 'c',
              isVisible: true,
              isHiddenByFilter: false
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'no filter, initial rendering, not empty category'
        },
        {
          filter: '',
          serviceConfigs: [],
          propertyToChange: null,
          isCollapsed: false,
          collapsedByDefault: true,
          expectedIsCollapsed: true,
          title: 'no filter, restore after filtering'
        },
        {
          filter: 'n',
          serviceConfigs: [
            Em.Object.create({
              name: 'nm',
              category: 'c',
              isVisible: true
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by name, not empty category'
        },
        {
          filter: 'd',
          serviceConfigs: [
            Em.Object.create({
              displayName: 'dn',
              category: 'c',
              isVisible: true
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by display name, not empty category'
        },
        {
          filter: 'd',
          serviceConfigs: [
            Em.Object.create({
              description: 'desc',
              category: 'c',
              isVisible: true
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by description, not empty category'
        },
        {
          filter: 'd',
          serviceConfigs: [
            Em.Object.create({
              defaultValue: 'dv',
              category: 'c',
              isVisible: true
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by default value, not empty category'
        },
        {
          filter: 'v',
          serviceConfigs: [
            Em.Object.create({
              value: 'val',
              category: 'c',
              isVisible: true
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by value, not empty category'
        },
        {
          filter: 'v',
          serviceConfigs: [
            Em.Object.create({
              category: 'c',
              isVisible: true,
              overrides: [
                Em.Object.create({
                  value: 'val'
                })
              ]
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by overridden property value, not empty category'
        },
        {
          filter: 'n',
          serviceConfigs: [
            Em.Object.create({
              category: 'c',
              isVisible: true,
              overrides: [
                Em.Object.create({
                  group: {
                    name: 'nm'
                  }
                })
              ]
            })
          ],
          propertyToChange: null,
          isCollapsed: false,
          expectedIsCollapsed: false,
          title: 'filtering by overridden property name, not empty category'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view = App.ServiceConfigsByCategoryView.create({
          parentView: {
            filter: item.filter,
            columns: []
          },
          category: {
            name: 'c',
            isCollapsed: item.isCollapsed,
            collapsedByDefault: item.collapsedByDefault
          },
          serviceConfigs: item.serviceConfigs
        });
        if (item.propertyToChange) {
          view.propertyDidChange(item.propertyToChange);
        } else {
          view.filteredCategoryConfigs();
        }
        expect(view.get('category.isCollapsed')).to.equal(item.expectedIsCollapsed);
      });
    });

  });

});
