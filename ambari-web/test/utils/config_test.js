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
require('config');
require('utils/config');

describe('App.config', function () {

  App.supports.capacitySchedulerUi = true;

  describe('#identifyCategory', function () {
    var data = {};
    it('should return null if config doesn\'t have category', function () {
      expect(App.config.identifyCategory(data)).to.equal(null);
    });
    it('should return "AdvancedCoreSite" if filename "core-site.xml" and serviceName "HDFS"', function () {
      data = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      expect(App.config.identifyCategory(data).name).to.equal('AdvancedCoreSite');
    });
    it('should return "CapacityScheduler" if filename "capacity-scheduler.xml" and serviceName "YARN"', function () {
      data = {
        serviceName: 'YARN',
        filename: 'capacity-scheduler.xml'
      };
      expect(App.config.identifyCategory(data).name).to.equal('CapacityScheduler');
    });
  });

  describe('#handleSpecialProperties', function () {
    var config = {};
    it('value should be transformed to "1024" from "1024m"', function () {
      config = {
        displayType: 'int',
        value: '1024m',
        defaultValue: '1024m'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal('1024');
      expect(config.defaultValue).to.equal('1024');
    });
    it('value should be transformed to true from "true"', function () {
      config = {
        displayType: 'checkbox',
        value: 'true',
        defaultValue: 'true'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(true);
      expect(config.defaultValue).to.equal(true);
    });
    it('value should be transformed to false from "false"', function () {
      config = {
        displayType: 'checkbox',
        value: 'false',
        defaultValue: 'false'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(false);
      expect(config.defaultValue).to.equal(false);
    });
  });

  describe('#calculateConfigProperties', function () {
    var config = {};
    var isAdvanced = false;
    var advancedConfigs = [];
    it('isUserProperty should be true if config is custom(site.xml) and not advanced', function () {
      config = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(true);
    });

    it('isUserProperty should be false if config from "capacity-scheduler.xml" or "mapred-queue-acls.xml" ', function () {
      config = {
        name: 'test',
        serviceName: 'MAPREDUCE',
        filename: 'capacity-scheduler.xml',
        isUserProperty: false
      };
      isAdvanced = true;
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(false);
    });

    it('isRequired should be false if config is advanced"', function () {
      config = {
        name: 'test',
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      isAdvanced = true;
      advancedConfigs = [{name:'test', filename: 'core-site.xml'}];
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.category).to.equal('Advanced');
      expect(config.isRequired).to.equal(true);
      expect(config.filename).to.equal('core-site.xml');
    });
  });

  describe('#fileConfigsIntoTextarea', function () {
    var filename = 'capacity-scheduler.xml';
    var configs = [
      {
        name: 'config1',
        value: 'value1',
        defaultValue: 'value1',
        filename: 'capacity-scheduler.xml'
      },
      {
        name: 'config2',
        value: 'value2',
        defaultValue: 'value2',
        filename: 'capacity-scheduler.xml'
      }
    ];
    it('two configs into textarea', function () {
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\n');
      expect(result[0].defaultValue).to.equal('config1=value1\nconfig2=value2\n');
    });
    it('three config into textarea', function () {
      configs.push({
        name: 'config3',
        value: 'value3',
        defaultValue: 'value3',
        filename: 'capacity-scheduler.xml'
      });
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
      expect(result[0].defaultValue).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
    });
    it('one of three configs has different filename', function () {
      configs[1].filename = 'another filename';
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      //result contains two configs: one with different filename and one textarea config
      expect(result.length).to.equal(2);
      expect(result[1].value).to.equal('config1=value1\nconfig3=value3\n');
      expect(result[1].defaultValue).to.equal('config1=value1\nconfig3=value3\n');
    });
    it('none configs into empty textarea', function () {
      filename = 'capacity-scheduler.xml';
      configs.clear();
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('');
      expect(result[0].defaultValue).to.equal('');
    });

  });

  describe('#textareaIntoFileConfigs', function () {
    var filename = 'capacity-scheduler.xml';
    var testData = [
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1\nconfig2=value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1,value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1 config2=value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      }
    ];

    it('config1=value1 to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[0].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
    });
    it('config1=value1\\nconfig2=value2\\n to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[1].configs, filename);
      expect(result.length).to.equal(2);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
      expect(result[1].value).to.equal('value2');
      expect(result[1].name).to.equal('config2');
    });
    it('config1=value1,value2\n to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[2].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1,value2');
      expect(result[0].name).to.equal('config1');
    });
    it('config1=value1 config2=value2 to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[3].configs, filename);
      expect(result.length).to.equal(1);
    });
  });

  describe('#escapeXMLCharacters', function () {

    var testConfigs = [
      {
        html: '&>"',
        json: '&>"'
      },
      {
        html: '&amp;&gt;&quot;&apos;',
        json: '&>"\''
      },
      {
        html: '&&gt;',
        json: '&>'
      },
      {
        html: '&&&amp;',
        json: '&&&'
      },
      {
        html: 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`',
        json: 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &> /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`'
      }
    ];
    testConfigs.forEach(function(t){
      it('parsing html ' + t.html, function () {
        expect(t.json).to.equal(App.config.escapeXMLCharacters(t.html));
      });
    });

  });
});