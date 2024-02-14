# postcss-url

[![Travis Build Status](https://img.shields.io/travis/postcss/postcss-url/master.svg?label=unix%20build)](https://travis-ci.org/postcss/postcss-url)
[![AppVeyor Build Status](https://img.shields.io/appveyor/ci/MoOx/postcss-url/master.svg?label=windows%20build)](https://ci.appveyor.com/project/MoOx/postcss-url)

> [PostCSS](https://github.com/postcss/postcss) plugin to rebase, inline or copy on url().

## Installation

```console
$ npm install postcss-url
```

## Usage

```js
// dependencies
var fs = require("fs")
var postcss = require("postcss")
var url = require("postcss-url")

// css to be processed
var css = fs.readFileSync("input.css", "utf8")

// process css
var output = postcss()
  .use(url({
    url: "rebase" // or "inline" or "copy"
  }))
  .process(css, {
    // "rebase" mode need at least one of those options
    // "inline" mode might need `from` option only
    // "copy" mode need `from` and `to` option to work
    from: "src/stylesheet/index.css",
    to: "dist/index.css"
  })
  .css
```

Checkout [tests](test) for examples.

### Options

#### `url`

_(default: `"rebase"`)_

##### `url: "rebase"`

Allow you to fix `url()` according to postcss `to` and/or `from` options (rebase to `to` first if available, otherwise `from` or `process.cwd()`).

##### `url: "inline"`

Allow you to inline assets using base64 encoding. Can use postcss `from` option to find ressources.

##### `url: "copy"`

Allow you to copy and rebase assets according to postcss `to`, `assetsPath` and `from` options (`assetsPath` is relative to the option `to`).

##### `url: {Function}`

Custom transform function. Takes following arguments:
* `URL` – original url
* `decl` - related postcss declaration object
* `from` - from postcss option
* `dirname` – dirname of processing file
* `to` – from postcss option
* `options` – plugin options
* `result` – postcss result object

And should return the transformed url.
You can use this option to adjust urls for CDN.

#### `maxSize`
_(default: `14`)_

Specify the maximum file size to inline (in kbytes)

#### `filter`

A regular expression e.g. `/\.svg$/`, a [minimatch string](https://github.com/isaacs/minimatch) e.g. `'**/*.svg'` or a custom filter function to determine wether a file should be inlined.

#### `fallback`

The url fallback method to use if max size is exceeded or url contains a hash.
Custom transform functions are supported.

#### `basePath`

Specify the base path where to search images from

#### `assetsPath`

_(default: `false`)_

If you specify an `assetsPath`, the assets files will be copied in that
destination

#### `useHash`

_(default: `false`)_

If set to `true` the copy method is going to rename the path of the files by a hash name

---

## Contributing

Work on a branch, install dev-dependencies, respect coding style & run tests before submitting a bug fix or a feature.

```console
$ git clone https://github.com/postcss/postcss-url.git
$ git checkout -b patch-1
$ npm install
$ npm test
```

## [Changelog](CHANGELOG.md)

## [License](LICENSE)
