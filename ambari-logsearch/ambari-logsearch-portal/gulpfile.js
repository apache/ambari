/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

var gulp = require('gulp'),
    minifyHTML = require('gulp-minify-html'),
    minifyCss = require('gulp-minify-css'),
    Del = require('del'),
    Yargs = require('yargs'),
    RunSequence = require('run-sequence'),
    shell = require('gulp-shell');


var argv = Yargs.argv;


/**
 * Build Settings
 */
var settings = {

    /*
     * Environment to build our application for
     *
     * If we have passed an environment via a
     * CLI option, then use that. If not attempt
     * to use the NODE_ENV. If not set, use production.
     */
    environment: !!argv.env ? argv.env : process.env.NODE_ENV || 'p',

    productionFolder: 'target/webapp-build',

    devFolder: 'src/main/webapp'

};



/**
 * Clean Task
 *
 * Clears the build folder from our
 * previous builds files.
 */
gulp.task('clean', function(cb) {
    return Del([
        settings.productionFolder
    ], cb);

});

/**
 * minify JS Task
 *
 */
gulp.task('minify-js', ['clean'], shell.task([
    'node production/r.js -o production/build.js'
]));


/**
 * minify CSS Task
 *
 */

gulp.task('minify-css', ['minify-js'], function() {
    return gulp.src(settings.productionFolder+'/**/*.css')
        .pipe(minifyCss({
            compatibility: 'ie8'
        }))
        .pipe(gulp.dest(settings.productionFolder+'/'));
});
/**
 * minify HTML Task
 *
 */
// gulp.task('minify-html', function() {
//     return gulp.src(settings.productionFolder+'/**/*.html')
//         .pipe(minifyHTML({
//             empty: true
//         }))
//         .pipe(gulp.dest(settings.productionFolder+'/'));
// });

/**
 * Build Task
 *
 */
/*gulp.task('builProduction', ['minify-css'], shell.task([
    'mvn clean compile package -Denv=' + settings.productionFolder
]));

gulp.task('runProduction', ['builProduction'], shell.task([
    'mvn exec:java -Denv=' + settings.productionFolder
]));

gulp.task('runDev', ['buildDev'], shell.task([
    'mvn exec:java -DwebAppDir=' + settings.devFolder
]));

gulp.task('buildDev', shell.task([
    'mvn clean compile package -DwebAppDir=' + settings.devFolder
]));*/
/**
 * Default Task
 *
 * Run the above tasks in the correct order
 */
gulp.task('default', function(cb) {

/*    if (settings.environment) {
        if (settings.environment == "p") {
            gulp.run(['runProduction']);
        }
        if (settings.environment == "d") {
            gulp.run(['runDev']);
        }
    }
    return gutil.log("All Done!");*/
});
