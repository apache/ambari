'use strict';

var path = require('path');
var gulp = require('gulp');
var runSequence = require('run-sequence');
var spawn = require('child_process').spawn;

var runSpawn = function(task, args, done) {
  done = done || function() {};
  var child = spawn(task, args, {stdio: 'inherit'});
  var running = false;
  child.on('close', function(code) {
    if (!running) {
      running = true;
      done(code);
    }
  });
  child.on('error', function(err) {
    if (!running) {
      console.error('gulp encountered a child error');
      running = true;
      done(err || 1);
    }
  });
  return child;
};

// Build
gulp.task('copy', function() {
  return gulp.src(['config.json', 'package.json'])
      .pipe(gulp.dest('built/'));
});

var tsGlobs = ['lib/**/*.ts', '*spec/**/*.ts'];

gulp.task('format:enforce', () => {
  const format = require('gulp-clang-format');
  const clangFormat = require('clang-format');
  return gulp.src(tsGlobs).pipe(
    format.checkFormat('file', clangFormat, {verbose: true, fail: true}));
});

gulp.task('format', () => {
  const format = require('gulp-clang-format');
  const clangFormat = require('clang-format');
  return gulp.src(tsGlobs, { base: '.' }).pipe(
    format.format('file', clangFormat)).pipe(gulp.dest('.'));
});

gulp.task('tsc', function(done) {
  runSpawn(process.execPath, ['node_modules/typescript/bin/tsc'], done);
});

gulp.task('prepublish', function(done) {
  runSequence('tsc', 'copy', done);
});

gulp.task('default', ['prepublish']);
gulp.task('build', ['prepublish']);

// Unit Test Commands
gulp.task('test:unit', ['format', 'build'], function(done) {
  runSpawn(process.execPath, ['node_modules/jasmine/bin/jasmine.js'], done);
});

// e2e test helper commands
var e2e_env = {headless: false, kvm: true};
gulp.task('update', ['build'], function(done) {
  runSpawn(process.execPath, ['bin/webdriver-manager', 'update', '--android',
      '--android-accept-licenses'], done)
});
gulp.task('start', ['build', 'shutdown'], function(done) {
  runSpawn(process.execPath, ['bin/webdriver-manager', 'start', '--detach', '--seleniumPort',
      '4444', '--android', '--appium-port', '4723', 
      '--quiet'].concat(e2e_env.headless ||
          !e2e_env.kvm ? ['--avds', 'none'] : []), done);
});
gulp.task('shutdown', ['build'], function(done) {
  runSpawn(process.execPath, ['bin/webdriver-manager', 'shutdown'], done);
});

gulp.task('test:e2e:inner', ['build'], function(done) {
  var config = e2e_env.headless ? 'headless.json' : e2e_env.kvm ? 'full.json' : 'no_android.json';
  runSpawn(process.execPath, ['node_modules/jasmine/bin/jasmine.js', 'JASMINE_CONFIG_PATH=' +
      path.join('e2e_spec', 'support', config)], done);
});
gulp.task('test:e2e:no_update', function(done) {
  runSequence('start', 'test:e2e:inner', 'shutdown', done);
});
gulp.task('test:e2e', function(done) {
  runSequence('update', 'test:e2e:no_update', done);
});


// Final command
gulp.task('test', ['test:unit', 'test:e2e']);
gulp.task('test:no_update', ['test:unit', 'test:e2e:no_update']);
gulp.task('test:e2e:no_kvm', [], function(done) {
  e2e_env.kvm = false;
  runSequence('test:e2e', done);
});
