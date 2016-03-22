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

var helpers = App.TestAliases.helpers;

/**
 *
 * @param {Em.Object} context
 * @param {string} propertyName
 * @param {string} dependentKey
 * @param {number} neededValue
 */
App.TestAliases.testAsComputedGetByKey = function (context, propertyName, objectKey, propertyKey) {

  var obj = Em.get(context, objectKey);
  var toCheck = Object.keys(obj);

  describe('#' + propertyName + ' as Em.computed.getByKey', function () {

    afterEach(function () {
      helpers.smartRestoreGet(context);
    });

    it('has valid dependent keys', function () {
      expect(Em.meta(context).descs[propertyName]._dependentKeys).to.eql([objectKey, propertyKey]);
    });

    toCheck.forEach(function (key) {
      var expectedValue = obj[key];
      it('should be `' + JSON.stringify(expectedValue) + '` if '+ JSON.stringify(propertyKey) + 'is ' + JSON.stringify(key), function () {
        helpers.smartStubGet(context, propertyKey, key)
          .propertyDidChange(context, propertyName);
        var value = helpers.smartGet(context, propertyName);
        expect(value).to.be.equal(expectedValue);
      });
    });

  });

};