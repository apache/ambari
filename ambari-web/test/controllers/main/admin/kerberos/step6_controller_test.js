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

describe('App.KerberosWizardStep6Controller', function() {
  describe('#checkComponentsRemoval', function() {

    var tests = [
      { yarnInstalled: true, doesATSSupportKerberos: false, commands: ['stopServices', 'deleteATS'], ATSInstalled: true},
      { yarnInstalled: false, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: true},
      { yarnInstalled: false, doesATSSupportKerberos: false, commands: ['stopServices'], ATSInstalled: true},
      { yarnInstalled: true, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: false},
      { yarnInstalled: true, doesATSSupportKerberos: true, commands: ['stopServices'], ATSInstalled: true}
    ];

    tests.forEach(function(test) {
      it('YARN installed: {0}, ATS supported: {1} list of commands should be {2}'.format(test.yarnInstalled, test.doesATSSupportKerberos, test.commands.toString()), function () {
        var controller = App.KerberosWizardStep6Controller.create({ commands: ['stopServices'] });
        sinon.stub(App, 'get').withArgs('doesATSSupportKerberos').returns(test.doesATSSupportKerberos);
        sinon.stub(App.Service, 'find').returns(test.yarnInstalled ? [Em.Object.create({ serviceName: 'YARN'})] : []);
        sinon.stub(App.HostComponent, 'find').returns(test.ATSInstalled ? [Em.Object.create({ componentName: 'APP_TIMELINE_SERVER'})] : []);
        controller.checkComponentsRemoval();
        App.get.restore();
        App.Service.find.restore();
        App.HostComponent.find.restore();
        expect(controller.get('commands').toArray()).to.eql(test.commands);
      });
    });
  });
});
