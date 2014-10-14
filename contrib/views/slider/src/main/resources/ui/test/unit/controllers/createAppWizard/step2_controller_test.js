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

moduleFor('controller:createAppWizardStep2', 'App.CreateAppWizardStep2Controller', {

  needs: [
    'controller:createAppWizard'
  ]

});

test('isNotInteger', function () {

  var controller = this.subject({});
  equal(controller.isNotInteger('1'), false, 'Valid value');
  equal(controller.isNotInteger('-1'), true, 'Invalid value (1)');
  equal(controller.isNotInteger('bbb'), true, 'Invalid value (2)');
  equal(controller.isNotInteger('1a'), true, 'Invalid value (3)');
  equal(controller.isNotInteger('!@#$%^'), true, 'Invalid value (4)');
  equal(controller.isNotInteger(null), true, 'Invalid value (5)');

});
