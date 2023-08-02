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

require('mixins/main/service/themes_mapping');

var mixin;

describe('App.ThemesMappingMixin', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.ThemesMappingMixin, {});
  });

  describe("#loadConfigTheme()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.Tab, 'find');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("should not execute ajax send function", function() {
      this.mock.returns([{serviceName: 'HIVE'}]);
      mixin.loadConfigTheme('HIVE');
      expect(App.ajax.send.calledOnce).to.be.false;
    });

    it("should execute ajax send function", function() {
      this.mock.returns([{serviceName: 'YARN'}]);
      mixin.loadConfigTheme('HIVE');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#_saveThemeToModel()", function () {

    beforeEach(function() {
      sinon.stub(App.themesMapper, 'map');
    });

    afterEach(function() {
      App.themesMapper.map.restore();
    });

    it("should not execute ajax send function", function() {
      var saveThemeToModel = mixin.get('_saveThemeToModel');
      saveThemeToModel({}, {}, {serviceName: 'HIVE'});
      expect(App.themesMapper.map.calledWith({}, ['HIVE'])).to.be.true;
    });
  });

  describe("#loadConfigThemeForServices()", function () {

    it("should not execute ajax send function", function() {
      mixin.loadConfigThemeForServices('HIVE');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#_saveThemeToModel()", function () {

    beforeEach(function() {
      sinon.stub(App.themesMapper, 'map');
    });

    afterEach(function() {
      App.themesMapper.map.restore();
    });

    it("should not execute App.themesMapper.map function", function() {
      var loadConfigThemeForServicesSuccess = mixin.get('_loadConfigThemeForServicesSuccess');
      loadConfigThemeForServicesSuccess({items: {}}, {}, {serviceNames: 'HIVE,YARN'});
      expect(App.themesMapper.map.called).to.be.false;
    });

    it("should execute App.themesMapper.map function", function() {
      var loadConfigThemeForServicesSuccess = mixin.get('_loadConfigThemeForServicesSuccess');
      loadConfigThemeForServicesSuccess({items: [{themes: 'name1'}, {themes: 'name2'}]}, {}, {serviceNames: 'HIVE,YARN'});
      expect(App.themesMapper.map.calledWith({items: 'name1name2'}, ['HIVE', 'YARN'])).to.be.true;
    });
  });

});