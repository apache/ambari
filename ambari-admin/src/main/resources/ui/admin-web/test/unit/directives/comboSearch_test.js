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

describe('#comboSearch', function () {
  var scope, element;

  beforeEach(module('ambariAdminConsole'));
  beforeEach(module('views/directives/comboSearch.html'));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();

    var preCompiledElement = '<combo-search suggestions="filters" filter-change="filterItems" placeholder="Search"></combo-search>';

    scope.filters = [
      {
        key: 'f1',
        label: 'filter1',
        options: []
      },
      {
        key: 'f2',
        label: 'filter2',
        options: []
      }
    ];
    scope.filterItems = angular.noop;
    spyOn(scope, 'filterItems');


    element = $compile(preCompiledElement)(scope);
    scope.$digest();
  }));

  afterEach(function() {
    element.remove();
  });


  describe('#removeFilter', function() {
    it('should remove filter by id', function () {
      var isoScope = element.isolateScope();
      isoScope.appliedFilters.push({
        id: 1
      });
      spyOn(isoScope, 'observeSearchFilterInput');
      spyOn(isoScope, 'updateFilters');

      isoScope.removeFilter({id: 1});

      expect(isoScope.appliedFilters).toEqual([]);
      expect(isoScope.observeSearchFilterInput).toHaveBeenCalled();
      expect(isoScope.updateFilters).toHaveBeenCalledWith([]);
    });
  });

  describe('#clearFilters', function() {
    it('should empty appliedFilters', function () {
      var isoScope = element.isolateScope();
      isoScope.appliedFilters.push({
        id: 1
      });
      spyOn(isoScope, 'updateFilters');

      isoScope.clearFilters();

      expect(isoScope.appliedFilters).toEqual([]);
      expect(isoScope.updateFilters).toHaveBeenCalledWith([]);
    });
  });

  describe('#selectFilter', function() {
    it('should add new filter to appliedFilters', function () {
      var isoScope = element.isolateScope();

      isoScope.selectFilter({
        key: 'f1',
        label: 'filter1',
        options: []
      });

      expect(isoScope.appliedFilters[0]).toEqual({
        id: 'filter_1',
        currentOption: null,
        filteredOptions: [],
        searchOptionInput: '',
        key: 'f1',
        label: 'filter1',
        options: [],
        showAutoComplete: false
      });
      expect(isoScope.isEditing).toBeFalsy();
      expect(isoScope.showAutoComplete).toBeFalsy();
      expect(isoScope.searchFilterInput).toEqual('');
    });
  });

  describe('#selectOption', function() {
    it('should set value to appliedFilter', function () {
      var isoScope = element.isolateScope();
      var filter = {};

      spyOn(isoScope, 'observeSearchFilterInput');
      spyOn(isoScope, 'updateFilters');

      isoScope.selectOption(null, {
        key: 'o1',
        label: 'option1'
      }, filter);

      expect(filter.currentOption).toEqual({
        key: 'o1',
        label: 'option1'
      });
      expect(filter.showAutoComplete).toBeFalsy();
      expect(isoScope.observeSearchFilterInput).toHaveBeenCalled();
      expect(isoScope.updateFilters).toHaveBeenCalled();
    });
  });

  describe('#hideAutocomplete', function() {

    it('showAutoComplete should be false when filter passed', function () {
      var isoScope = element.isolateScope();
      var filter = {
        showAutoComplete: true
      };
      jasmine.Clock.useMock();

      isoScope.hideAutocomplete(filter);

      jasmine.Clock.tick(101);
      expect(filter.showAutoComplete).toBeFalsy();
    });

    it('showAutoComplete should be false when isEditing = false', function () {
      var isoScope = element.isolateScope();
      jasmine.Clock.useMock();

      isoScope.isEditing = false;
      isoScope.showAutoComplete = true;
      isoScope.hideAutocomplete();

      jasmine.Clock.tick(101);
      expect(isoScope.showAutoComplete).toBeFalsy();
    });

    it('showAutoComplete should be false when isEditing = true', function () {
      var isoScope = element.isolateScope();
      jasmine.Clock.useMock();

      isoScope.isEditing = true;
      isoScope.showAutoComplete = true;
      isoScope.hideAutocomplete();

      jasmine.Clock.tick(101);
      expect(isoScope.showAutoComplete).toBeTruthy();
    });
  });

  describe('#makeActive', function() {
    it('category option can not be active', function () {
      var isoScope = element.isolateScope();
      var active = {
        key: 'o1',
        isCategory: true,
        active: false
      };

      isoScope.makeActive(active, [active]);

      expect(active.active).toBeFalsy();
    });

    it('value option can be active', function () {
      var isoScope = element.isolateScope();
      var active = {
        key: 'o1',
        isCategory: false,
        active: false
      };

      isoScope.makeActive(active, [active]);

      expect(active.active).toBeTruthy();
    });
  });

  describe('#updateFilters', function() {
    it('filter function from parent scope should be called', function () {
      var isoScope = element.isolateScope();
      spyOn(isoScope, 'extractFilters').andReturn([{}]);

      isoScope.updateFilters([{}]);

      expect(scope.filterItems).toHaveBeenCalledWith([{}]);
    });
  });

  describe('#extractFilters', function() {
    it('should extract filters', function () {
      var isoScope = element.isolateScope();
      var filters = [
        {
          currentOption: { key: 'o1'},
          key: 'f1'
        },
        {
          currentOption: { key: 'o2'},
          key: 'f1'
        },
        {
          currentOption: null,
          key: 'f2'
        }
      ];

      expect(isoScope.extractFilters(filters)).toEqual([
        {
          key: 'f1',
          values: ['o1', 'o2']
        }
      ]);
    });
  });

});
