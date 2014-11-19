/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

module.exports = {

  /**
   * Examples:
   * <code>
   *   var actual = [{a:1, b: [1, 2], c: 3}],
   *    expected = [{a: 1, b: [1, 2]}];
   *  nestedExpect(expected, actual); // valid
   * </code>
   *
   * <code>
   *   var actual = [{a:1, b: [1, 2]}],
   *    expected = [{a: 1, b: [1, 2], c: 3}];
   *  nestedExpect(expected, actual); // invalid valid (actual[0] doesn't contains key 'c)
   * </code>
   * @param {object[]} expected
   * @param {object[]} actual
   * @method nestedExpect
   */
  nestedExpect: function (expected, actual) {
    expected.forEach(function (group, i) {
      Em.keys(group).forEach(function (key) {
        if ('array' === Em.typeOf(actual[i][key])) {
          expect(group[key]).to.eql(actual[i][key].toArray());
        }
        else {
          expect(group[key]).to.equal(actual[i][key]);
        }
      });
    });
  }

};