# webpack-concat-plugin
[![npm package](https://img.shields.io/npm/v/webpack-concat-plugin.svg)](https://www.npmjs.org/package/webpack-concat-plugin)
[![npm downloads](http://img.shields.io/npm/dm/webpack-concat-plugin.svg)](https://www.npmjs.org/package/webpack-concat-plugin)
> a plugin to help webpack to concat js and inject to html-webpack-plugin
### why
webpack is really powerful, but when I just want to concat the static file and inject to html without webpack JSONP code wrapper. After all days search, it seems impossible to do that without other tool's help.

### install
```
npm install webpack-concat-plugin --save-dev
```

### features
* concat
* inject to html(with html-webpack-plugin)

### usage
```
const ConcatPlugin = require('webpack-concat-plugin');

new ConcatPlugin({
    uglify: true, // or you can set uglifyjs options
    useHash: true, // md5 file
    sourceMap: true, // generate sourceMap
    name: 'flexible', // used in html-webpack-plugin
    fileName: '[name].[hash].bundle.js', // would output to 'flexible.d41d8cd98f00b204e980.bundle.js'
    filesToConcat: ['./src/lib/flexible.js', './src/lib/makegrid.js']
});

```
### inject to html(by hand)
```
doctype html
...
    script(src=htmlWebpackPlugin.files.webpackConcat.flexible)
...
```

### todo
* add css support
* auto inject to html
