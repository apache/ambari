[![Build Status](https://travis-ci.org/flauc/angular2-notifications.svg?branch=master)](https://travis-ci.org/flauc/angular2-notifications)
[![NPM Version](https://img.shields.io/badge/npm-0.4.46-brightgreen.svg?style=flat)](https://www.npmjs.com/package/angular2-notifications)
# Angular2-Notifications
A light and easy to use notifications library for Angular 2. It features both regular page notifications (toasts) and push notifications. 

## Example
Take a look at the live demo here: [Live Demo](http://flauc.github.io/angular2-notifications)
You can also clone this repository and check out the example folder.

## Setup
Download the library with npm
```
npm install --save angular2-notifications
```

Map the library in your `system.config.js` if you're using SystemJs.
```js
var map = {
    'angular2-notifications': 'node_modules/angular2-notifications'
}

var packages = {
    'angular2-notifications': { main: 'components.js', defaultExtension: 'js' }
}
```

## Documentation 

* [Toast Notifications Documentation](https://github.com/flauc/angular2-notifications/tree/master/docs/toastNotifications.md)
* [Push Notifications Documentation](https://github.com/flauc/angular2-notifications/tree/master/docs/pushNotifications.md)

