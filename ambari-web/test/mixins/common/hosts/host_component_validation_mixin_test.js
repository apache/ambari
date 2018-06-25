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
var helpers = require('test/helpers');

describe('App.HostComponentValidationMixin', function() {
  var mixedObject;
  beforeEach(function() {
    mixedObject = Em.Object.create(App.HostComponentValidationMixin, {});
  });

  describe('#formatValidateComponents', function() {
    [
      {
        components: undefined,
        hosts: [],
        m: 'when undefined components passed should return empty array',
        e: []
      },
      {
        components: [],
        hosts: [],
        m: 'when empty array passed should return empty array',
        e: []
      },
      {
        components: [
          { componentName: 'C1', hosts: ['h1', 'h2']},
          { componentName: 'C2', hosts: ['h3']},
        ],
        hosts: [],
        m: 'when components passed along with hosts location should return correct objects',
        e: [
          Em.Object.create({ componentName: 'C1', hostName: 'h1'}),
          Em.Object.create({ componentName: 'C1', hostName: 'h2'}),
          Em.Object.create({ componentName: 'C2', hostName: 'h3'}),
        ]
      }
    ].forEach(function(test) {
      it(test.m, function() {
        expect(mixedObject.formatValidateComponents(test.components, test.hosts)).to.be.eql(test.e);
      });
    });
  });

  describe('#validateSelectedHostComponents', function() {
    beforeEach(function() {
      sinon.stub(mixedObject, 'getHostComponentValidationRequest');
      sinon.stub(mixedObject, 'getHostComponentValidationParams');
      sinon.stub(mixedObject, 'formatValidateComponents', function(c) { return c;});
    });

    afterEach(function() {
      mixedObject.getHostComponentValidationRequest.restore();
      mixedObject.getHostComponentValidationParams.restore();
      mixedObject.formatValidateComponents.restore();
    });

    [
      {
        opts: null,
        e: {
          hosts: [],
          components: [],
          blueprint: null
        },
        m: 'when no args passed defaults should be used and no error thrown'
      },
      {
        opts: {
          hosts: ['h1'],
          components: [{componentName: 'c1'}],
          blueprint: {recommend:{}}
        },
        e: {
          hosts: ['h1'],
          components: [{componentName: 'c1'}],
          blueprint: {recommend:{}}
        },
        m: 'should normaly merge defaults with passed options'
      }
    ].forEach(function(test) {
      it(test.m, function() {
        mixedObject.validateSelectedHostComponents(test.opts);
        expect(mixedObject.getHostComponentValidationParams.args[0][0]).to.be.eql(test.e);
      });
    });
  });

  describe('#getHostComponentValidationRequest', function() {
    it('default request options checking', function() {
      mixedObject.getHostComponentValidationRequest('someData');
      var args = helpers.findAjaxRequest('name', 'mpack.advisor.validations');
      expect(args[0]).to.be.eql({
        name: 'mpack.advisor.validations',
        sender: mixedObject,
        data: 'someData',
        success: 'updateValidationsSuccessCallback',
        error: 'updateValidationsErrorCallback'
      });
    });
  });

  describe('#getHostComponentValidationParams', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').withArgs('stackVersionURL').returns('/stack/url');
    });
    afterEach(function() {
      App.get.restore();
    });
    [
      {
        options: {
          hosts: ['h1'],
          components: [
            Em.Object.create({
              componentName: 'c1',
              hostName: 'h1',
              mpackInstance: 'm1',
              serviceInstance: 'si1'
            })
          ],
          blueprint: null,
          mpack_instances: []
        },
        e: {
          data: {
            validate: 'host_groups',
            hosts: ['h1'],
            recommendations: {
              blueprint: {
                host_groups: [
                  {
                    name: 'host-group-1', components: [{
                      name: 'c1',
                      mpack_instance: 'm1',
                      service_instance: 'si1' 
                    }]
                  }
                ],
                mpack_instances: []
              },
              blueprint_cluster_binding: {
                host_groups: [
                  { name: 'host-group-1', hosts: [{ fqdn: 'h1' }] }
                ]
              }
            }
          }
        },
        m: 'when blueprint not passed it should be generated from components list'
      },
      {
        options: {
          hosts: ['h1'],
          components: [Em.Object.create({componentName: 'c1', hostName: 'h1'})],
          blueprint: { blueprint: {} },
          mpack_instances: []
        },
        e: {
          data: {
            validate: 'host_groups',
            hosts: ['h1'],
            recommendations: {
              blueprint: {
                mpack_instances: []
              }
            }
          }
        },
        m: 'when blueprint passed it should be used instead of generated blueprint'
      }
    ].forEach(function(test) {
      it(test.m, function() {
        expect(mixedObject.getHostComponentValidationParams(test.options)).to.be.eql(test.e);
      });
    });
  });
});
