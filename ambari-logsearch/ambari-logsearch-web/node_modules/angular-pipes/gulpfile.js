'use strict';

const gulp = require('gulp');
const clean = require('gulp-clean');

gulp.task('clean', function () {
   
   return gulp.src([
     'esm',
     'coverage',
     'bundles',
     'src/**/*.js',
     'src/**/*.d.ts',
     'src/**/*.js.map',
     'src/**/*.metadata.json',
     'src/**/*.ngsummary.json'
   ]).pipe(clean()); 
});

