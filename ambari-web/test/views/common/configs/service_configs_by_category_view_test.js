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

  describe('#isShowBlock', function() {
    var tests = [
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: {},
        m: 'no configs with widget, filtered properties are visible. Panel should be shown',
        e: true
      },
      {
        categoryConfigs: Em.A([]),
        category: Em.Object.create({ customCanAddProperty: true}),
        m: 'Category with custom properties. Panel shouldn\'t be shown',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: Em.Object.create({ customCanAddProperty: true}),
        m: 'Category with custom properties. Filtered configs are hidden. Panel should be shown',
        e: true
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ isHiddenByFilter: true })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Filtered configs are hidden. Category not for custom properties. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category without properties and not for custom configurations. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'}}),
          Em.Object.create({ widget: {someProp: 'b'}})
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'All properties have widgets and category is not custom. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: null }),
          Em.Object.create({ widget: null })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'All properties have widgets set to `null` and category is not custom. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'} }),
          Em.Object.create({ isHiddenByFilter: true })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category contains mixed properties. Properties are hidden by filter. Panel should be hidden',
        e: false
      },
      {
        categoryConfigs: Em.A([
          Em.Object.create({ widget: {someProp: 'a'} }),
          Em.Object.create({ isHiddenByFilter: false })
        ]),
        category: Em.Object.create({ customCanAddProperty: false }),
        m: 'Category contains mixed properties. Properties are visible. Panel should be shown',
        e: true
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        var _view = App.ServiceConfigsByCategoryView.create({
          serviceConfigs: Em.A([]),
          category: test.category,
          categoryConfigs: test.categoryConfigs
        });
        sinon.stub(_view, 'filteredCategoryConfigs', Em.K);
        _view.filteredCategoryConfigs.restore();
        expect(_view.get('isShowBlock')).to.be.eql(test.e);
        _view.destroy();
      });
    });
  });

  describe('#isSecureConfig', function () {

    var cases = [
      {
        name: 'n0',
        filename: 'f0',
        isSecureConfig: true,
        title: 'secure config'
      },
      {
        name: 'n1',
        filename: 'f1',
        isSecureConfig: false,
        title: 'secure config with the same name is present in another filename'
      },
      {
        name: 'n2',
        filename: 'f2',
        isSecureConfig: false,
        title: 'no configs of the specified filename are secure'
      }
    ];

    before(function () {
      App.config.reopen({
        secureConfigs: [
          {
            name: 'n0',
            filename: 'f0'
          },
          {
            name: 'n1',
            filename: 'f0'
          },
          {
            name: 'n2',
            filename: 'f1'
          }
        ]
      })
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(view.isSecureConfig(item.name, item.filename)).to.equal(item.isSecureConfig);
      });
    });

  });

  describe('#createProperty', function () {

    var cases = [
      {
        propertyObj: {
          name: 'n0',
          displayName: 'd0',
          value: 'v0',
          filename: 'f0',
          categoryName: 'c0',
          serviceName: 's0'
        },
        isDefaultConfigGroup: true,
        result: {
          name: 'n0',
          displayName: 'd0',
          value: 'v0',
          displayType: 'string',
          isSecureConfig: true,
          category: 'c0',
          serviceName: 's0',
          savedValue: null,
          supportsFinal: true,
          filename: 'f0',
          isUserProperty: true,
          isNotSaved: true,
          isRequired: false,
          group: null,
          isOverridable: true
        },
        title: 'single line value, secure config, final attribute supported, default config group'
      },
      {
        propertyObj: {
          name: 'n1',
          value: 'v\n1',
          filename: '',
          categoryName: 'c1',
          serviceName: 's1'
        },
        isDefaultConfigGroup: false,
        result: {
          name: 'n1',
          displayName: 'n1',
          value: 'v\n1',
          displayType: 'multiLine',
          isSecureConfig: false,
          category: 'c1',
          serviceName: 's1',
          savedValue: null,
          supportsFinal: false,
          filename: '',
          isUserProperty: true,
          isNotSaved: true,
          isRequired: false,
          group: Em.Object.create({
            isDefault: false
          }),
          isOverridable: false
        },
        title: 'multiline value, non-secure config, no display name and filename, final attribute not supported, custom config group'
      }
    ];

    before(function () {
      view.get('serviceConfigs').clear();
      sinon.stub(view, 'isSecureConfig').withArgs('n0', 'f0').returns(true).withArgs('n1', '').returns(false);
      sinon.stub(App.config, 'shouldSupportFinal').withArgs('s0', 'f0').returns(true).withArgs('s1', '').returns(false);
    });

    after(function () {
      view.get('serviceConfigs').clear();
      view.isSecureConfig.restore();
      App.config.shouldSupportFinal.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.reopen({
          filteredCategoryConfigs: [],
          controller: {
            selectedConfigGroup: Em.Object.create({
              isDefault: item.isDefaultConfigGroup
            })
          }
        });
        view.createProperty(item.propertyObj);
        expect(view.get('serviceConfigs').filterProperty('name', item.propertyObj.name)).to.have.length(1);
        expect(view.get('serviceConfigs').findProperty('name', item.propertyObj.name).getProperties([
          'name', 'displayName', 'value', 'displayType', 'isSecureConfig', 'category', 'serviceName', 'savedValue',
          'supportsFinal', 'filename', 'isUserProperty', 'isNotSaved', 'isRequired', 'group', 'isOverridable'
        ])).to.eql(item.result);
      });
    });

  });

  describe('#categoryConfigs', function () {
    var view,
      result = [1,2,3,4,5],
      cases = [
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 1, name: "a", isVisible: true, resultId: 1}),
            Em.Object.create({category: "TestCategory", index: 2, name: "b", isVisible: true, resultId: 2}),
            Em.Object.create({category: "TestCategory", index: 5, name: "c", isVisible: true, resultId: 5}),
            Em.Object.create({category: "TestCategory", index: 4, name: "d", isVisible: true, resultId: 4}),
            Em.Object.create({category: "TestCategory", index: 3, name: "e", isVisible: true, resultId: 3})
          ],
          title: 'Order by index with no content type'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 1, name: "a", isVisible: true, resultId: 1, displayType: 'int'}),
            Em.Object.create({category: "TestCategory", index: 2, name: "b", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 3, name: "c", isVisible: true, resultId: 2}),
            Em.Object.create({category: "TestCategory", index: 4, name: "d", isVisible: true, resultId: 5, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 5, name: "e", isVisible: true, resultId: 3})
          ],
          title: 'Order configs by index and display type equal to content'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", name: "a", isVisible: true, resultId: 1, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "b", isVisible: true, resultId: 2, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "c", isVisible: true, resultId: 3, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "d", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", name: "e", isVisible: true, resultId: 5, displayType: 'content'})
          ],
          title: 'Order configs by display type equal to content - so they will be sorted alphabetically'
        },
        {
          categoryNname: 'TestCategory',
          serviceConfigs: [
            Em.Object.create({category: "TestCategory", index: 5, name: "a", isVisible: true, resultId: 1, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 4, name: "b", isVisible: true, resultId: 2, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 3, name: "c", isVisible: true, resultId: 3, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 2, name: "d", isVisible: true, resultId: 4, displayType: 'content'}),
            Em.Object.create({category: "TestCategory", index: 1, name: "e", isVisible: true, resultId: 5, displayType: 'content'})
          ],
          title: 'Order configs by display type equal to content - so they will be sorted alphabetically not by index'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view = App.ServiceConfigsByCategoryView.create({
          category: {
            name: item.categoryNname
          },
          serviceConfigs: item.serviceConfigs
        });
        expect(view.get('categoryConfigs').mapProperty('resultId')).to.deep.equal(result);
      });
    });
  });
});
