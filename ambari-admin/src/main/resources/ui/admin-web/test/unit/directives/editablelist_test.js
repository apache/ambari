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

describe('#Editablelist directive', function () {

  describe('Editing', function () {
    var scope, element;
    var $location, $modal;

    beforeEach(module('ambariAdminConsole'));
    beforeEach(module('views/directives/editableList.html'));

    beforeEach(inject(function($rootScope, $compile, _$location_, _$modal_) {
      $location = _$location_;
      $modal = _$modal_;

      spyOn($modal, 'open').andReturn({
        result:{
          then: function() {
          }
        }
      });

      scope = $rootScope.$new();

      element = '<editable-list items-source="permissionsEdit.TestPermission.USER" resource-type="User" editable="true"></editable-list>';

      scope.permissionsEdit = {
         'TestPermission': {
          'USER': ['user1', 'user2']
         }
      };

      element = $compile(element)(scope);
      scope.$digest();
    }));

    afterEach(function() {
      element.remove();
    });
    

    it('Updates permissions after save', function () {
      var isoScope = element.isolateScope();
      isoScope.items.push('user3');
      
      expect(scope.permissionsEdit.TestPermission.USER).toEqual(['user1', 'user2']);
      
      isoScope.save();
      scope.$digest();
      
      expect(scope.permissionsEdit.TestPermission.USER).toEqual(['user1', 'user2', 'user3']);
    });

    it('Show dialog window if user trying to leave page without save', function() {
      var isoScope = element.isolateScope();
      isoScope.items.push('user3');
      isoScope.editMode = true;
      
      expect(isoScope.editMode).toBe(true);
      scope.$broadcast('$locationChangeStart', 'some#url');
      expect($modal.open).toHaveBeenCalled();
    });

    it('Saves current user in editing window if user click "save"', function() {
      var isoScope = element.isolateScope();
      isoScope.editMode = true;
      isoScope.input = 'user3';
      isoScope.save();
      scope.$digest();

      expect(scope.permissionsEdit.TestPermission.USER).toEqual(['user1', 'user2', 'user3']);
    });
  });
});
