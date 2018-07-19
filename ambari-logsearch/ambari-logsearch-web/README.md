# AmbariLogsearchWeb

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 1.0.0.

## Development server

Run `npm start` or `yarn start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Webpack Development Config
In order to use Webpack Devserver proxy without changing the main config file and commit accidentally we can use a `webpack.config.dev.js` file for that. So you can set a service URL in order to test the UI against real data.

The content of the `webpack.config.dev.js` can be:
```
const merge = require('webpack-merge');
const baseConfig = require('./webpack.config.js');

module.exports = merge(baseConfig, {
  devServer: {
    historyApiFallback: true,
    proxy: {
      '/': 'http://c7401.ambari.apache.org:61888'
    }
  }
});
```
And you can start it that way: `NODE_ENV=production yarn start --config webpack.config.dev.js`
You have to set the `NODE_ENV` to production in order to skip the mock data usage

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive/pipe/service/class/module`.

## Build

Run `npm run build` or `yarn build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `npm run build-prod` or `yarn build-prod` command for a production build.

## Running unit tests

Run `npm test` or `yarn test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `npm run e2e` or `yarn e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).
Before running the tests make sure you are serving the app via `npm start` or `yarn start`.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
