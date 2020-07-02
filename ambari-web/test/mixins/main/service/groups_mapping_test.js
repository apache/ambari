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

require('mixins/main/service/groups_mapping');

var mixin;

describe('App.GroupsMappingMixin', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.GroupsMappingMixin, {
      trackRequest: Em.K
    });
  });

  describe("#loadConfigGroups()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'trackRequest');
    });

    afterEach(function() {
      mixin.trackRequest.restore();
    });

    it("should set configGroupsAreLoaded to true", function() {
      mixin.loadConfigGroups();
      expect(mixin.get('configGroupsAreLoaded')).to.be.true;
    });

    it("should execute trackRequest function", function() {
      mixin.loadConfigGroups(['HIVE', 'YARN']);
      expect(mixin.trackRequest.calledOnce).to.be.true;
    });
  });

  describe("#saveConfigGroupsToModel()", function () {

    beforeEach(function() {
      sinon.stub(App.configGroupsMapper, 'map', Em.K);
    });

    afterEach(function() {
      App.configGroupsMapper.map.restore();
    });

    it("should set configGroupsAreLoaded to true", function() {
      mixin.saveConfigGroupsToModel({}, {}, {serviceNames: 'HIVE, YARN', dfd: $.Deferred()});
      expect(mixin.get('configGroupsAreLoaded')).to.be.true;
    });
  });

});