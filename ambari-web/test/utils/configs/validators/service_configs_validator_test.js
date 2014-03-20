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

var App = require('app');
require('utils/configs/validators/service_configs_validator');

describe('App.ServiceConfigsValidator', function() {

  describe('#validateConfig', function() {
    it('No config validator', function() {
      var v = App.ServiceConfigsValidator.create({});
      expect(v.validateConfig(Em.Object.create({name:'name'}))).to.equal(null);
    });
  });

  describe('#validatorLessThenDefaultValue', function() {
    var tests = Em.A([
      {
        recommendedDefaults: {
          'property1': 100500
        },
        config: Em.Object.create({
          value: 100000,
          name: 'property1'
        }),
        m: 'Numeric value',
        e: 'string'
      },
      {
        recommendedDefaults: {
          'property1': 'xx100500x'
        },
        config: Em.Object.create({
          value: 'xx100000x',
          name: 'property1'
        }),
        m: 'String value',
        e: 'string'
      },
      {
        recommendedDefaults: {
          'property1': null
        },
        config: Em.Object.create({
          value: 100000,
          name: 'property1'
        }),
        m: 'No default value for property',
        e: null
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        var v = App.ServiceConfigsValidator.create({});
        v.set('recommendedDefaults', test.recommendedDefaults);
        var r = v.validatorLessThenDefaultValue(test.config);
        if (test.e) {
          expect(r).to.be.a(test.e);
        }
        else {
          expect(r).to.equal(null)
        }
      });
    });
  });

  describe('#_checkXmxValueFormat', function() {
    var tests = Em.A([
      {value: '',e: false},
      {value: '-',e: false},
      {value: '100',e: false},
      {value: '-Xmx',e: false},
      {value: '-XMX1',e: false},
      {value: '-Xmxb',e: false},
      {value: '-Xmxk',e: false},
      {value: '-Xmxm',e: false},
      {value: '-Xmxg',e: false},
      {value: '-Xmxp',e: false},
      {value: '-Xmxt',e: false},
      {value: '-XmxB',e: false},
      {value: '-XmxK',e: false},
      {value: '-XmxM',e: false},
      {value: '-XmxG',e: false},
      {value: '-XmxP',e: false},
      {value: '-XmxT',e: false},
      {value: '-Xmx1',e: true},
      {value: '-Xmx1b',e: true},
      {value: '-Xmx1k',e: true},
      {value: '-Xmx1m',e: true},
      {value: '-Xmx1g',e: true},
      {value: '-Xmx1t',e: true},
      {value: '-Xmx1p',e: true},
      {value: '-Xmx1B',e: true},
      {value: '-Xmx1K',e: true},
      {value: '-Xmx1M',e: true},
      {value: '-Xmx1G',e: true},
      {value: '-Xmx1T',e: true},
      {value: '-Xmx1P',e: true},
      {value: '-Xmx100',e: true},
      {value: '-Xmx100b',e: true},
      {value: '-Xmx100k',e: true},
      {value: '-Xmx100m',e: true},
      {value: '-Xmx100g',e: true},
      {value: '-Xmx100t',e: true},
      {value: '-Xmx100p',e: true},
      {value: '-Xmx100B',e: true},
      {value: '-Xmx100K',e: true},
      {value: '-Xmx100M',e: true},
      {value: '-Xmx100G',e: true},
      {value: '-Xmx100T',e: true},
      {value: '-Xmx100P',e: true},
      {value: '-Xmx100Psome',e: false},
      {value: '-Xmx100P-Xmx',e: false},
      {value: '-Xmx100P -Xmx',e: false},
      {value: '-Xmx100P -XMX',e: false},
      {value: '-server -Xmx1024m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: true},
      {value: '-server -Xmx1024 -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: true},
      {value: '-server -Xmx1024', e: true},
      {value: '-Xmx1024 -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: true},
      {value: '-server -Xmx1024m-Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server -Xmx1024-Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server-Xmx1024m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server-Xmx1024 -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server-Xmx1024m-Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server-Xmx1024-Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-Xmx1024-Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC', e: false},
      {value: '-server-Xmx1024', e: false},
      {value: '-server    -Xmx1024m   -Da=b',e: true},
      {value: '-server -Xmx1024m -Da=b',e: true},
      {value: '-server -XMx1024m -Da=b',e: false},
      {value: '-server -Xmx1024M -Da=b',e: true},
      {value: '-server -Xmx1 -Da=b',e: true},
      {value: '-server -Xmx1100MBPS -Da=b',e: false},
      {value: '-server -Xmx1100M -Xmx200 -Da=b',e: false},
      {value: '-server --Xmx1100M -Da=b',e: false},
      {value: '-Xmx1024m -server -Da=b',e: true},
      {value: ' -server -Da=b -Xmx1024m',e: true}
    ]);
    tests.forEach(function(test) {
      it(test.value, function() {
        var v = App.ServiceConfigsValidator.create({});
        expect(v._checkXmxValueFormat(test.value)).to.equal(test.e);
      });
    });
  });

  describe('#_getXmxSize', function() {
    var tests = Em.A([
      {value: '-Xmx1', e: '1'},
      {value: '-Xmx1b', e: '1b'},
      {value: '-Xmx1k', e: '1k'},
      {value: '-Xmx1m', e: '1m'},
      {value: '-Xmx1g', e: '1g'},
      {value: '-Xmx1t', e: '1t'},
      {value: '-Xmx1p', e: '1p'},
      {value: '-Xmx1B', e: '1b'},
      {value: '-Xmx1K', e: '1k'},
      {value: '-Xmx1M', e: '1m'},
      {value: '-Xmx1G', e: '1g'},
      {value: '-Xmx1T', e: '1t'},
      {value: '-Xmx1P', e: '1p'},
      {value: '-Xmx100b', e: '100b'},
      {value: '-Xmx100k', e: '100k'},
      {value: '-Xmx100m', e: '100m'},
      {value: '-Xmx100g', e: '100g'},
      {value: '-Xmx100t', e: '100t'},
      {value: '-Xmx100p', e: '100p'},
      {value: '-Xmx100B', e: '100b'},
      {value: '-Xmx100K', e: '100k'},
      {value: '-Xmx100M', e: '100m'},
      {value: '-Xmx100G', e: '100g'},
      {value: '-Xmx100T', e: '100t'},
      {value: '-Xmx100P', e: '100p'}
    ]);
    tests.forEach(function(test) {
      it(test.value, function() {
        var v = App.ServiceConfigsValidator.create({});
        expect(v._getXmxSize(test.value)).to.equal(test.e);
      });
    });
  });

  describe('#_formatXmxSizeToBytes', function() {
    var tests = Em.A([
      {value: '1', e: 1},
      {value: '1 ', e: 1},
      {value: '100', e: 100},
      {value: '100 ', e: 100},
      {value: '100b', e: 100},
      {value: '100B', e: 100},
      {value: '100k', e: 100 * 1024},
      {value: '100K', e: 100 * 1024},
      {value: '100m', e: 100 * 1024 * 1024},
      {value: '100M', e: 100 * 1024 * 1024},
      {value: '100g', e: 100 * 1024 * 1024 * 1024},
      {value: '100G', e: 100 * 1024 * 1024 * 1024},
      {value: '100t', e: 100 * 1024 * 1024 * 1024 * 1024},
      {value: '100T', e: 100 * 1024 * 1024 * 1024 * 1024},
      {value: '100p', e: 100 * 1024 * 1024 * 1024 * 1024 * 1024},
      {value: '100P', e: 100 * 1024 * 1024 * 1024 * 1024 * 1024}
    ]);
    tests.forEach(function(test) {
      it(test.value, function() {
        var v = App.ServiceConfigsValidator.create({});
        expect(v._formatXmxSizeToBytes(test.value)).to.equal(test.e);
      });
    });
  });

  describe('#validateXmxValue', function() {
    var tests = Em.A([
      {
        recommendedDefaults: {
          'property1': '-Xmx1024m'
        },
        config: Em.Object.create({
          value: '-Xmx2g',
          name: 'property1'
        }),
        e: null
      },
      {
        recommendedDefaults: {
          'property1': '-Xmx12'
        },
        config: Em.Object.create({
          value: '-Xmx24',
          name: 'property1'
        }),
        e: null
      },
      {
        recommendedDefaults: {
          'property1': '-Xmx333k'
        },
        config: Em.Object.create({
          value: '-Xmx134k',
          name: 'property1'
        }),
        e: 'string'
      },
      {
        recommendedDefaults: {
          'property1': '-Xmx333k'
        },
        config: Em.Object.create({
          value: '-Xmx534',
          name: 'property1'
        }),
        e: 'string'
      },
      {
        recommendedDefaults: {},
        config: Em.Object.create({
          defaultValue: '-Xmx123',
          value: '-Xmx123',
          name: 'name'
        }),
        e: null
      },
      {
        recommendedDefaults: {},
        config: Em.Object.create({
          defaultValue: '-Xmx124',
          value: '-Xmx123',
          name: 'name'
        }),
        e: 'string'
      }
    ]);
    tests.forEach(function(test) {
      it(test.config.get('value'), function() {
        var v = App.ServiceConfigsValidator.create({});
        v.set('recommendedDefaults', test.recommendedDefaults);
        var r = v.validateXmxValue(test.config);
        if (test.e) {
          expect(r).to.be.a(test.e);
        }
        else {
          expect(r).to.equal(null)
        }
      });
    });

    it('Error should be thrown', function() {
      var v = App.ServiceConfigsValidator.create({});
      v.set('recommendedDefaults', {});
      expect(function() {v.validateXmxValue(Em.Object.create({value:''}));}).to.throw(Error);
    });

  });

});
