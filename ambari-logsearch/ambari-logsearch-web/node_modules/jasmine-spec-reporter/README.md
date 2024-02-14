jasmine-spec-reporter
=====================

[![Build Status](https://travis-ci.org/bcaudan/jasmine-spec-reporter.svg?branch=master)](https://travis-ci.org/bcaudan/jasmine-spec-reporter)
[![Coverage Status](https://coveralls.io/repos/bcaudan/jasmine-spec-reporter/badge.svg?branch=master)](https://coveralls.io/r/bcaudan/jasmine-spec-reporter?branch=master)

Real time console spec reporter for jasmine behavior-driven development testing framework.

![](screenshot.png)

# Usage

## Installation
Install `jasmine-spec-reporter` via npm:

    npm install jasmine-spec-reporter --save-dev

## Examples
* [Jasmine node tests](examples/node)
* [Protractor tests](examples/protractor)
* [TypeScript support](examples/typescript)

## Configuration
See full configuration and features: [configuration.ts](src/configuration.ts)

## Custom output
You can customize the output of the reporter yourself: [see how](docs/customize-output.md).

# Developement

* install dependencies: `npm install`
* compile sources: `npm run tsc`
* launch unit tests: `npm test`
* launch integration tests: `npm test:integration`

# Contribution

## Bug

If you find a bug in the source code or a mistake in the documentation, feel free to submit an issue or even better a pull request with a fix and **appropriate test cases**.

## Feature

If you want a new feature, you can do the same but some types of features would probably not be integrated:

* options to tweak output format
* options to add extra information to specific sections (spec, suite, summary)

There is already a lot of options, so to add your specific behavior to the reporter, please consider to [build and use your own display processor](docs/customize-output.md).

