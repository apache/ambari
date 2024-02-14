# karma-coverage-istanbul-reporter
[![Build Status](https://travis-ci.org/mattlewis92/karma-coverage-istanbul-reporter.svg?branch=master)](https://travis-ci.org/mattlewis92/karma-coverage-istanbul-reporter)
[![codecov](https://codecov.io/gh/mattlewis92/karma-coverage-istanbul-reporter/branch/master/graph/badge.svg)](https://codecov.io/gh/mattlewis92/karma-coverage-istanbul-reporter)
[![npm version](https://badge.fury.io/js/karma-coverage-istanbul-reporter.svg)](http://badge.fury.io/js/karma-coverage-istanbul-reporter)
[![npm](https://img.shields.io/npm/dm/karma-coverage-istanbul-reporter.svg)](http://badge.fury.io/js/karma-coverage-istanbul-reporter)
[![GitHub issues](https://img.shields.io/github/issues/mattlewis92/karma-coverage-istanbul-reporter.svg)](https://github.com/mattlewis92/karma-coverage-istanbul-reporter/issues)
[![GitHub stars](https://img.shields.io/github/stars/mattlewis92/karma-coverage-istanbul-reporter.svg)](https://github.com/mattlewis92/karma-coverage-istanbul-reporter/stargazers)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/mattlewis92/karma-coverage-istanbul-reporter/master/LICENSE)

> A karma reporter that uses the latest istanbul 1.x APIs (with full sourcemap support) to report coverage.

## About
This is a reporter only and does not perform the actual instrumentation of your code. Webpack users should use the [istanbul-instrumenter-loader](https://github.com/deepsweet/istanbul-instrumenter-loader) and then use this karma reporter to do the actual reporting. See the [test config](https://github.com/mattlewis92/karma-coverage-istanbul-reporter/blob/master/test/karma.conf.js) for an e2e example of how to combine them.

## Installation

```bash
npm install karma-coverage-istanbul-reporter --save-dev
```

## Configuration

```js
// karma.conf.js
module.exports = function (config) {

  config.set({

    // ... rest of karma config

    // anything named karma-* is normally auto included so you probably dont need this
    plugins: ['karma-coverage-istanbul-reporter'],

    reporters: ['coverage-istanbul'],

    // any of these options are valid: https://github.com/istanbuljs/istanbul-api/blob/47b7803fbf7ca2fb4e4a15f3813a8884891ba272/lib/config.js#L33-L38
    coverageIstanbulReporter: {

       // reports can be any that are listed here: https://github.com/istanbuljs/istanbul-reports/tree/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib
      reports: ['html', 'lcovonly', 'text-summary'],

       // base output directory
      dir: './coverage',

       // if using webpack and pre-loaders, work around webpack breaking the source path
      fixWebpackSourcePaths: true,

       // Most reporters accept additional config options. You can pass these through the `report-config` option
      'report-config': {

        // all options available at: https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/html/index.js#L135-L137
        html: {
          // outputs the report in ./coverage/html
          subdir: 'html'
        }

      }

    }

  });

}
```

### List of reporters and options
* [clover](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/clover/index.js#L9)
* [cobertura](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/cobertura/index.js#L9-L10)
* [html](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/html/index.js#L135-L137)
* [json-summary](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/json-summary/index.js#L8)
* [json](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/json/index.js#L8)
* lcov
* [lcovonly](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/lcovonly/index.js#L8)
* none
* [teamcity](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/teamcity/index.js#L9-L10)
* [text-lcov](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/text-lcov/index.js#L9)
* [text-summary](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/text-summary/index.js#L9)
* [text](https://github.com/istanbuljs/istanbul-reports/blob/590e6b0089f67b723a1fdf57bc7ccc080ff189d7/lib/text/index.js#L141-L142)

## Credits
* [Original karma-coverage source](https://github.com/karma-runner/karma-coverage/blob/master/lib/reporter.js)
* [Example of using the new reporter API](https://github.com/facebook/jest/blob/master/scripts/mapCoverage.js)
* [Karma remap istanbul](https://github.com/marcules/karma-remap-istanbul)

## License
MIT
