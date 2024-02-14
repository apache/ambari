var webpack                  = require('webpack');
var assert                   = require('assert');
var sinon                    = require('sinon');
var path                     = require('path');
var MemoryFS                 = require("memory-fs");
var CircularDependencyPlugin = require('../index');

describe('circular dependency', () => {
  var sandbox;
  beforeEach(() => {
    sandbox = sinon.sandbox.create();
  });

  afterEach(() => {
    sandbox.restore();
  });

  it('detects circular dependencies from a -> b -> c -> b', (done) => {
    var fs = new MemoryFS();
    var c = webpack({
      entry: path.join(__dirname, 'deps/a.js'),
      output: { path: __dirname },
      plugins: [ new CircularDependencyPlugin() ]
    });

    c.outputFileSystem = fs;

    c.run(function(err, stats){
      if (err) {
        assert(false, err);
        done();
      } else {
        stats = stats.toJson()
        assert(stats.warnings[0].includes('__tests__/deps/b.js -> __tests__/deps/c.js -> __tests__/deps/b.js'))
        assert(stats.warnings[0].match(/Circular/))
        assert(stats.warnings[1].match(/b\.js/))
        assert(stats.warnings[1].match(/c\.js/))
        done();
      }
    });
  });

  it('detects circular dependencies from d -> e -> f -> g -> e', (done) => {
    var fs = new MemoryFS();
    var c = webpack({
      entry: path.join(__dirname, 'deps/d.js'),
      output: { path: __dirname },
      plugins: [ new CircularDependencyPlugin() ]
    });

    c.outputFileSystem = fs;

    c.run(function(err, stats){
      if (err) {
        assert(false, err);
        done();
      } else {
        stats = stats.toJson()
        assert(stats.warnings[0].includes('__tests__/deps/e.js -> __tests__/deps/f.js -> __tests__/deps/g.js -> __tests__/deps/e.js'))
        assert(stats.warnings[0].match(/Circular/))
        assert(stats.warnings[1].match(/e\.js/))
        assert(stats.warnings[1].match(/f\.js/))
        assert(stats.warnings[1].match(/g\.js/))
        done();
      }
    });
  });

  it('uses errors instead of warnings with failOnError', (done) => {
    var fs = new MemoryFS();
    var c = webpack({
      entry: path.join(__dirname, 'deps/d.js'),
      output: { path: __dirname },
      plugins: [ new CircularDependencyPlugin({
        failOnError: true
      }) ]
    });

    c.outputFileSystem = fs;

    c.run(function(err, stats){
      if (err) {
        assert(false, err);
        done();
      } else {
        stats = stats.toJson()
        assert(stats.errors[0].includes('__tests__/deps/e.js -> __tests__/deps/f.js -> __tests__/deps/g.js -> __tests__/deps/e.js'))
        assert(stats.errors[0].match(/Circular/))
        assert(stats.errors[1].match(/e\.js/))
        assert(stats.errors[1].match(/f\.js/))
        assert(stats.errors[1].match(/g\.js/))
        done();
      }
    });
  });

  it('can exclude cyclical deps from being output', (done) => {
    var s = sandbox.stub(console, 'warn', console.warn);
    var fs = new MemoryFS();
    var c = webpack({
      entry: path.join(__dirname, 'deps/d.js'),
      output: { path: __dirname },
      plugins: [
        new CircularDependencyPlugin({
          exclude: /f\.js/
        })
      ]
    });

    c.outputFileSystem = fs;

    c.run(function(err, stats){
      if (err) {
        assert(false, err);
        done();
      } else {
        stats = stats.toJson()
        assert(stats.warnings[0].match(/Circular/))
        assert(stats.warnings[1].match(/e\.js/))
        assert(stats.warnings[1].match(/g\.js/))
        done();
      }
    });
  });


  it(`can handle context modules that have an undefined resource h -> i -> a -> i`, (done) => {
    var s = sandbox.stub(console, 'warn', console.warn);
    var fs = new MemoryFS();
    var c = webpack({
      entry: path.join(__dirname, 'deps/h.js'),
      output: { path: __dirname },
      plugins: [
        new CircularDependencyPlugin()
      ]
    });

    c.outputFileSystem = fs;

    c.run(function(err, stats){
      if (err) {
        assert(false, err);
        done();
      } else {
        done();
      }
    });
  });
});
