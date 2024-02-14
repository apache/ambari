# angular-moment-timezone

moment-timezone.js pipes for Angular

[![Build Status](https://travis-ci.org/saaadel/angular-moment-timezone.svg?branch=master)](https://travis-ci.org/saaadel/angular-moment-timezone)

This module works with Angular 2.0 and above.

Installation
------------

`npm install --save angular-moment-timezone`

If you use typescript 1.8, and [typings](https://github.com/typings/typings), you may also need to install typings for moment.js:

`typings install --save moment moment-timezone`

### For System.js users:

First you need to install moment:

`npm install --save moment moment-timezone`

Don´t forget to update your systemjs.config.js:

```
packages: {
            app: {
                main: './main.js',
                defaultExtension: 'js'
            },
            'moment': {
                main: './moment.js',
                defaultExtension: 'js'
            },
            'moment-timezone': {
                main: './moment-timezone.js',
                defaultExtension: 'js'
            },
            'angular-moment-timezone': {
                main: './index.js',
                defaultExtension: 'js'
            }
        }
```

Usage
-----

Import `MomentTimezoneModule` into your app's modules:

``` typescript
import {MomentTimezoneModule} from 'angular-moment-timezone';

@NgModule({
  imports: [
    MomentTimezoneModule
  ]
})
```

This makes all the `angular-moment-timezone` pipes available for use in your app components.


Available pipes
---------------

Note: [angular2-moment](https://github.com/urish/angular2-moment) pipes also used in examples.


## amTz pipe
Takes an optional `parseInZone` argument that defaults to `false`.

``` typescript
@Component({
  selector: 'app',
  template: `
    Last updated: {{myDate | amTz:'America/New_York' | amDateFormat }}
  `
})
```

Prints date in "America/New_York" timezone.


Complete Example
----------------

``` typescript
import {NgModule, Component} from 'angular2/core';
import {BrowserModule} from '@angular/platform-browser';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {MomentModule} from 'angular2-moment';
import {MomentTimezoneModule} from 'angular-moment-timezone';

@Component({
  selector: 'app',
  template: `
    Last updated: <b>{{myDate  | amTz:'America/New_York' | amDateFormat }}</b>
  `
})
export class AppComponent {
  myDate: Date;

  constructor() {
    this.myDate = new Date();
  }
}

@NgModule({
  imports: [
    BrowserModule,
    MomentModule,
    MomentTimezoneModule
  ],
  declarations: [ AppComponent ]
  bootstrap: [ AppComponent ]
})
class AppModule {}

platformBrowserDynamic().bootstrapModule(AppModule);
```
