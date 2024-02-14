## Circular Dependency Plugin

Detect modules with circular dependencies when bundling with webpack.

Circular dependencies are often a necessity in complex software, the presence of a circular dependency doesn't always imply a bug, but in the case where the you believe a bug exists, this module may help find it.

### Usage

```js
// webpack.config.js
let CircularDependencyPlugin = require('circular-dependency-plugin')

module.exports = {
  entry: "./src/index",
  plugins: [
    new CircularDependencyPlugin({
      // exclude detection of files based on a RegExp
      exclude: /a\.js|node_modules/,
      // add errors to webpack instead of warnings
      failOnError: true
    })
  ]
}
```
