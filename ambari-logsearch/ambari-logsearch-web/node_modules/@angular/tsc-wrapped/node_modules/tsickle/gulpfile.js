/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */

require('source-map-support').install();

var clangFormat = require('clang-format');
var formatter = require('gulp-clang-format');
var fs = require('fs');
var gulp = require('gulp');
var gutil = require('gulp-util');
var merge = require('merge2');
var mocha = require('gulp-mocha');
var sourcemaps = require('gulp-sourcemaps');
var ts = require('gulp-typescript');
var tslint = require('gulp-tslint');
var typescript = require('typescript');

var tsProject = ts.createProject('tsconfig.json', {
  // Specify the TypeScript version we're using.
  typescript: typescript,
});

const formatted = ['*.js', 'src/**/*.ts', 'test/**/*.ts'];

gulp.task('format', function() {
  return gulp.src(formatted, {base: '.'})
      .pipe(formatter.format('file', clangFormat))
      .pipe(gulp.dest('.'));
});

gulp.task('test.check-format', function() {
  return gulp.src(formatted)
      .pipe(formatter.checkFormat('file', clangFormat, {verbose: true}))
      .on('warning', onError);
});

gulp.task('test.check-lint', function() {
  return gulp.src(['src/**/*.ts', 'test/**/*.ts'])
      .pipe(tslint({formatter: 'verbose'}))
      .pipe(tslint.report())
      .on('warning', onError);
});

var hasError;
var failOnError = true;

var onError = function(err) {
  hasError = true;
  if (failOnError) {
    process.exit(1);
  }
};

gulp.task('compile', function() {
  hasError = false;
  var tsResult =
      gulp.src(['src/**/*.ts']).pipe(sourcemaps.init()).pipe(tsProject()).on('error', onError);
  return merge([
    tsResult.dts.pipe(gulp.dest('build/definitions')),
    // Write external sourcemap next to the js file
    tsResult.js.pipe(sourcemaps.write('.', {includeContent: false, sourceRoot: '../../src'}))
        .pipe(gulp.dest('build/src')),
    tsResult.js.pipe(gulp.dest('build/src')),
  ]);
});

gulp.task('test.compile', ['compile'], function(done) {
  if (hasError) {
    done();
    return;
  }
  return gulp.src(['test/*.ts'], {base: '.'})
      .pipe(sourcemaps.init())
      .pipe(tsProject())
      .on('error', onError)
      .js.pipe(sourcemaps.write('.', {includeContent: false, sourceRoot: '../..'}))
      .pipe(gulp.dest('build/'));  // '/test/' comes from base above.
});

gulp.task('test.unit', ['test.compile'], function(done) {
  if (hasError) {
    done();
    return;
  }
  return gulp.src(['build/test/**/*.js', '!build/test/**/e2e*.js']).pipe(mocha({timeout: 1000}));
});

gulp.task('test.e2e', ['test.compile'], function(done) {
  if (hasError) {
    done();
    return;
  }
  return gulp.src(['build/test/**/e2e*.js']).pipe(mocha({timeout: 25000}));
});

gulp.task('test', ['test.unit', 'test.e2e', 'test.check-format', 'test.check-lint']);

gulp.task('watch', function() {
  failOnError = false;
  gulp.start(['test.unit']);  // Trigger initial build.
  return gulp.watch(['src/**/*.ts', 'test/**/*.ts', 'test_files/**'], ['test.unit']);
});

gulp.task('default', ['compile']);
