# @ngx-translate/http-loader [![Build Status](https://travis-ci.org/ngx-translate/http-loader.svg?branch=master)](https://travis-ci.org/ngx-translate/http-loader) [![npm version](https://img.shields.io/npm/v/@ngx-translate/http-loader.svg)](https://www.npmjs.com/package/@ngx-translate/http-loader)

A loader for [ngx-translate](https://github.com/ngx-translate/core) that loads translation using http.

Get the complete changelog here: https://github.com/ngx-translate/http-loader/releases

* [Installation](#installation)
* [Usage](#usage)

## Installation

We assume that you already installed [ngx-translate](https://github.com/ngx-translate/core).

Now you need to install the npm module for `TranslateHttpLoader`:

```sh
npm install @ngx-translate/http-loader --save
```

## Usage

#### 1. Setup the `TranslateModule` to use the `TranslateHttpLoader`:

The `TranslateHttpLoader` uses Http to load translations, which means that you have to import the HttpModule from `@angular/http` before the `TranslateModule`:

```ts
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {HttpModule, Http} from '@angular/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {AppComponent} from "./app";

// AoT requires an exported function for factories
export function HttpLoaderFactory(http: Http) {
    return new TranslateHttpLoader(http);
}

@NgModule({
    imports: [
        BrowserModule,
        HttpModule,
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: HttpLoaderFactory,
                deps: [Http]
            }
        })
    ],
    bootstrap: [AppComponent]
})
export class AppModule { }
```

The `TranslateHttpLoader` also has two optional parameters:
- prefix: string = "/assets/i18n/"
- suffix: string = ".json"

By using those default parameters, it will load your translations files for the lang "en" from: `/assets/i18n/en.json`.

You can change those in the `HttpLoaderFactory` method that we just defined. For example if you want to load the "en" translations from `/public/lang-files/en-lang.json` you would use:

```ts
export function HttpLoaderFactory(http: Http) {
    return new TranslateHttpLoader(http, "/public/lang-files/", "-lang.json");
}
```

For now this loader only support the json format.
