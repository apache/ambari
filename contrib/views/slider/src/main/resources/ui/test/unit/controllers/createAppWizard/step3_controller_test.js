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

moduleFor('controller:createAppWizardStep3', 'App.CreateAppWizardStep3Controller', {

  needs: [
    'controller:createAppWizard',
    'controller:slider'
  ]

});

test('initConfigs', function () {

  var controller = this.subject(),
    wizardController = controller.get('controllers.createAppWizard');

  Em.run(function () {
    wizardController.setProperties({
      content: {},
      newApp: {
        appType: {
          configs: []
        },
        configs: {
          java_home: '/usr/jdk64/jdk1.7.0_40'
        }
      }
    });
    App.__container__.lookup('controller:slider').getViewDisplayParametersSuccessCallback({
      "ViewInstanceInfo" : {
        "instance_data": {
          "java.home": "/usr/jdk64/jdk1.7.0_45"
        },
        "properties": {
          "slider.user": "admin"
        }
      }
    });
    Em.run(function () {
      controller.initConfigs(true);
    });
  });

  equal(controller.get('configs').findBy('label', 'java_home').get('value'), '/usr/jdk64/jdk1.7.0_45', 'should set default java_home property value');

});