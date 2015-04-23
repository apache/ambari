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
require('models/configs/service_config_version');

var model;

describe('App.ServiceConfigVersion', function () {

  beforeEach(function () {
    model = App.ServiceConfigVersion.createRecord({});
  });

  describe('#authorFormatted', function () {

    var cases = [
      {
        author: 'admin',
        authorFormatted: 'admin',
        title: 'should display username as is'
      },
      {
        author: 'userNameIsTooLongToDisplay',
        authorFormatted: 'userNameIsTooLongToD...',
        title: 'should trim username to 20 chars'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        model.set('author', item.author);
        expect(model.get('authorFormatted')).to.equal(item.authorFormatted);
      });
    });

  });

  describe('#canBeMadeCurrent', function () {

    var cases = [
      {
        isCompatible: true,
        isCurrent: true,
        canBeMadeCurrent: false,
        title: 'current version'
      },
      {
        isCompatible: true,
        isCurrent: false,
        canBeMadeCurrent: true,
        title: 'compatible version'
      },
      {
        isCompatible: false,
        isCurrent: false,
        canBeMadeCurrent: false,
        title: 'not compatible version'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        model.setProperties({
          isCompatible: item.isCompatible,
          isCurrent: item.isCurrent
        });
        expect(model.get('canBeMadeCurrent')).to.equal(item.canBeMadeCurrent);
      });
    });

  });

});
