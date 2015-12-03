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

require('mixins/main/service/configs/config_overridable');

var configOverridable;

var configGroups = [
  Em.Object.create({name: 'configGroup1'}),
  Em.Object.create({name: 'configGroup2'}),
  Em.Object.create({name: 'configGroup3'})
];

describe('App.Decommissionable', function () {

  beforeEach(function () {
    configOverridable = Em.Object.create(App.ConfigOverridable);
  });

  describe('#validate of Config Group Selection Creation Dialog', function () {

    var testCases = [
      {
        text: 'fail validation because of disallowed symbols',
        name: '123=',
        isWarn: true,
        errorMessage: Em.I18n.t("form.validator.configGroupName"),
        option: false
      },
      {
        text: 'fail validation because of the same name',
        name: 'configGroup1',
        isWarn: true,
        errorMessage: Em.I18n.t("config.group.selection.dialog.err.name.exists"),
        option: false
      },
      {
        text: 'pass validation',
        name: '123',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: false
      },
      {
        text: 'pass validation as another option is selected',
        name: '123',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      },
      {
        text: 'pass validation because there is no value entered',
        name: '',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      },
      {
        text: 'pass validation because there is no value entered',
        name: '      ',
        isWarn: false,
        errorMessage: '&nbsp;',
        option: true
      }
    ];

    testCases.forEach(function (item) {
      it('should ' + item.text, function () {
        var popup = configOverridable.launchConfigGroupSelectionCreationDialog('service', configGroups, Em.Object.create());
        popup.set('newConfigGroupName', item.name);
        popup.set('optionSelectConfigGroup', item.option);
        expect(popup.get('isWarning')).to.equal(item.isWarn);
        expect(popup.get('warningMessage')).to.equal(item.errorMessage);
      });
    });
  });

});
