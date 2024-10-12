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

module.exports = function(config) {
  config.set({
    basePath: '',
    plugins: [
      'karma-chrome-launcher',
      'karma-mocha',
      'karma-chai',
      'karma-sinon',
      'karma-coverage',
      'karma-ember-precompiler-brunch',
      'karma-commonjs-require',
      'karma-babel-preprocessor'
    ],
    frameworks: ['mocha', 'chai', 'sinon'],
    files: [
      'node_modules/commonjs-require-definition/require.js',
      'vendor/scripts/console-helper.js',
      'vendor/scripts/jquery-3.7.1.js',
      'vendor/scripts/handlebars-1.0.0.beta.6.js',
      'vendor/scripts/ember-latest.js',
      'vendor/scripts/ember-data-latest.js',
      'vendor/scripts/ember-i18n-1.4.1.js',
      'vendor/scripts/bootstrap.js',
      'vendor/scripts/bootstrap-combobox.js',
      'vendor/scripts/bootstrap-switch.min.js',
      'vendor/scripts/d3.v2.js',
      'vendor/scripts/cubism.v1.js',
      'vendor/scripts/jquery.ui.core.js',
      'vendor/scripts/jquery.ui.position.js',
      'vendor/scripts/jquery.ui.widget.js',
      'vendor/scripts/jquery.ui.autocomplete.js',
      'vendor/scripts/jquery.ui.mouse.js',
      'vendor/scripts/jquery.ui.datepicker.js',
      'vendor/scripts/jquery-ui-timepicker-addon.js',
      'vendor/scripts/jquery.ui.slider.js',
      'vendor/scripts/jquery.ui.sortable.js',
      'vendor/scripts/jquery.ui.custom-effects.js',
      'vendor/scripts/jquery.timeago.js',
      'vendor/scripts/jquery.ajax-retry.js',
      'vendor/scripts/difflib.js',
      'vendor/scripts/diffview.js',
      'vendor/scripts/underscore.js',
      'vendor/scripts/backbone.js',
      'vendor/scripts/visualsearch.js',
      'vendor/scripts/workflow_visualization.js',
      'vendor/scripts/rickshaw.js',
      'vendor/scripts/spin.js',
      'vendor/scripts/jquery.flexibleArea.js',
      'vendor/scripts/FileSaver.js',
      'vendor/scripts/Blob.js',
      'vendor/scripts/moment.min.js',
      'vendor/scripts/moment-timezone-with-data-2020-2030.js',
      'vendor/**/*.js',
      'app/templates/**/*.hbs',
      'app!(assets)/**/!(karma_setup|tests).js',
      'app/assets/test/karma_setup.js',
      {
        pattern: 'app/assets/data/**',
        served: true,
        included: false,
        watched: true
      },
      'test/**/*.js',
      'app/assets/test/tests.js'
    ],
    emberPrecompilerBrunchPreprocessor: {
      jqueryPath: 'vendor/scripts/jquery-3.7.1.js',
      emberPath: 'vendor/scripts/ember-latest.js',
      handlebarsPath: 'vendor/scripts/handlebars-1.0.0.beta.6.js'
    },
    commonRequirePreprocessor: {
      appDir: 'app'
    },
    coverageReporter: {
      type: 'html',
      dir: 'public/coverage/'
    },
    preprocessors: {
      '!(vendor|node_modules|test)/**/!(karma_setup|tests).js': 'coverage',
      'app/templates/**/*.hbs': ['ember-precompiler-brunch', 'common-require'],
      'app!(assets)/**/!(karma_setup|tests).js': ['common-require', 'babel'],
      'test/**/*.js': ['common-require']
    },
    babelPreprocessor: {
      options: {
        presets: ['es2015']
      },
      filename: function (file) {
        return file.originalPath.replace(/\.js$/, '.js');
      },
      sourceFileName: function (file) {
        return file.originalPath;
      }
    },
    exclude: [],
    reporters: ['progress', 'coverage'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    customLaunchers: {
      ChromeHeadlessCustom: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-translate', '--disable-extensions', '--remote-debugging-port=9222', '--disable-background-timer-throttling', '--disable-renderer-backgrounding']
      }
    },
    browsers: ['ChromeHeadlessCustom'],
    captureTimeout: 200000,
    browserNoActivityTimeout: 200000,
    browserDisconnectTimeout: 200000,
    browserDisconnectTolerance: 3,
    singleRun: true
  });
};

