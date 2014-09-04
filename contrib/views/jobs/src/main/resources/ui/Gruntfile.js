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
// Generated on 2014-06-24 using generator-ember 0.8.4
'use strict';
var LIVERELOAD_PORT = 35729;
var lrSnippet = require('connect-livereload')({port: LIVERELOAD_PORT});
var mountFolder = function (connect, dir) {
  return connect.static(require('path').resolve(dir));
};

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to match all subfolders:
// 'test/spec/**/*.js'

module.exports = function (grunt) {
  // show elapsed time at the end
  require('time-grunt')(grunt);
  // load all grunt tasks
  require('load-grunt-tasks')(grunt);

  // configurable paths
  var yeomanConfig = {
    app: 'app',
    dist: 'dist'
  };

  grunt.initConfig({
    yeoman: yeomanConfig,
    watch: {
      emberTemplates: {
        files: '<%= yeoman.app %>/templates/**/*.hbs',
        tasks: ['emberTemplates']
      },
      neuter: {
        files: ['<%= yeoman.app %>/scripts/{,*/}*.js'],
        tasks: ['neuter']
      },
      less: {
        files: '<%= yeoman.app %>/styles/**/*.less',
        tasks: ['less:development']
      },
      livereload: {
        options: {
          livereload: LIVERELOAD_PORT
        },
        files: [
          '.tmp/scripts/*.js',
          '<%= yeoman.app %>/*.html',
          '{.tmp,<%= yeoman.app %>}/styles/{,*/}*.css',
          '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    connect: {
      options: {
        port: 9000,
        // change this to '0.0.0.0' to access the server from outside
        hostname: 'localhost'
      },
      livereload: {
        options: {
          middleware: function (connect) {
            return [
              lrSnippet,
              mountFolder(connect, '.tmp'),
              mountFolder(connect, yeomanConfig.app)
            ];
          }
        }
      },
      test: {
        options: {
          middleware: function (connect) {
            return [
              mountFolder(connect, 'test'),
              mountFolder(connect, '.tmp')
            ];
          }
        }
      },
      dist: {
        options: {
          middleware: function (connect) {
            return [
              mountFolder(connect, yeomanConfig.dist)
            ];
          }
        }
      }
    },
    open: {
      server: {
        path: 'http://localhost:<%= connect.options.port %>'
      }
    },
    clean: {
      dist: {
        files: [
          {
            dot: true,
            src: [
              '.tmp',
              '<%= yeoman.dist %>/*',
              '!<%= yeoman.dist %>/.git*'
            ]
          }
        ]
      },
      server: '.tmp'
    },
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: [
        'Gruntfile.js',
        '<%= yeoman.app %>/scripts/{,*/}*.js',
        '!<%= yeoman.app %>/scripts/vendor/*',
        'test/spec/{,*/}*.js'
      ]
    },
    mocha: {
      all: {
        options: {
          run: true,
          urls: ['http://localhost:<%= connect.options.port %>/index.html']
        }
      }
    },
    // not used since Uglify task does concat,
    // but still available if needed
    /*concat: {
     dist: {}
     },*/
    // not enabled since usemin task does concat and uglify
    // check index.html to edit your build targets
    // enable this task if you prefer defining your build targets here
    /*uglify: {
     dist: {}
     },*/
    rev: {
      dist: {
        files: {
          src: [
            '<%= yeoman.dist %>/scripts/{,*/}*.js',
            '<%= yeoman.dist %>/styles/{,*/}*.css',
            '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp}',
            '<%= yeoman.dist %>/styles/fonts/*'
          ]
        }
      }
    },
    useminPrepare: {
      html: '.tmp/index.html',
      options: {
        dest: '<%= yeoman.dist %>'
      }
    },
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      options: {
        dirs: ['<%= yeoman.dist %>']
      }
    },
    svgmin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>/images',
            src: '{,*/}*.svg',
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    cssmin: {
      dist: {
        files: {
          '<%= yeoman.dist %>/styles/main.css': [
            '.tmp/styles/{,*/}*.css',
            '<%= yeoman.app %>/styles/{,*/}*.css'
          ]
        }
      }
    },
    htmlmin: {
      dist: {
        options: {
          /*removeCommentsFromCDATA: true,
           // https://github.com/yeoman/grunt-usemin/issues/44
           //collapseWhitespace: true,
           collapseBooleanAttributes: true,
           removeAttributeQuotes: true,
           removeRedundantAttributes: true,
           useShortDoctype: true,
           removeEmptyAttributes: true,
           removeOptionalTags: true*/
        },
        files: [
          {
            expand: true,
            cwd: '<%= yeoman.app %>',
            src: '*.html',
            dest: '<%= yeoman.dist %>'
          }
        ]
      }
    },
    replace: {
      app: {
        options: {
          variables: {
            ember: 'bower_components/ember/ember.js',
            ember_data: 'bower_components/ember-data/ember-data.js'
          }
        },
        files: [
          {src: '<%= yeoman.app %>/index.html', dest: '.tmp/index.html'}
        ]
      },
      dist: {
        options: {
          variables: {
            ember: 'bower_components/ember/ember.prod.js',
            ember_data: 'bower_components/ember-data/ember-data.prod.js'
          }
        },
        files: [
          {src: '<%= yeoman.app %>/index.html', dest: '.tmp/index.html'}
        ]
      }
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: '<%= yeoman.app %>',
            dest: '<%= yeoman.dist %>',
            src: [
              '*.{ico,txt}',
              '.htaccess',
              'img/*',
              'styles/fonts/*',
              'scripts/assets/**/*'
            ]
          },
          {
            expand: true,
            flatten: true,
            src: '<%= yeoman.app %>/bower_components/jquery-ui/themes/base/images/*',
            dest: '<%= yeoman.dist %>/styles/images/'
          },
          {
            expand: true,
            flatten: true,
            src: '<%= yeoman.app %>/bower_components/font-awesome/font/*',
            dest: '<%= yeoman.dist %>/font/'
          }
        ]
      }
    },
    concurrent: {
      server: [
        'emberTemplates'
      ],
      test: [
        'emberTemplates'
      ],
      dist: [
        'emberTemplates',
        'svgmin',
        'htmlmin'
      ]
    },
    emberTemplates: {
      options: {
        templateName: function (sourceFile) {
          var templatePath = yeomanConfig.app + '/templates/';
          return sourceFile.replace(templatePath, '');
        }
      },
      dist: {
        files: {
          '.tmp/scripts/compiled-templates.js': '<%= yeoman.app %>/templates/**/*.hbs'
        }
      }
    },

    less: {
      development: {
        options: {
          paths: ["<%= yeoman.app %>/styles"]
        },
        files: {
          ".tmp/styles/styles.css": "<%= yeoman.app %>/styles/**/*.less"
        }
      },
      production: {
        options: {
          paths: ["<%= yeoman.app %>/styles"],
          cleancss: true
        },
        files: {
          ".tmp/styles/styles.css": "<%= yeoman.app %>/styles/**/*.less"
        }
      }
    },

    neuter: {
      app: {
        options: {
          filepathTransform: function (filepath) {
            return yeomanConfig.app + '/' + filepath;
          }
        },
        src: '<%= yeoman.app %>/scripts/app.js',
        dest: '.tmp/scripts/combined-scripts.js'
      }
    }
  });

  grunt.registerTask('server', function (target) {
    grunt.log.warn('The `server` task has been deprecated. Use `grunt serve` to start a server.');
    grunt.task.run(['serve:' + target]);
  });

  grunt.registerTask('serve', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'open', 'connect:dist:keepalive']);
    }

    grunt.task.run([
      'clean:server',
      'replace:app',
      'concurrent:server',
      'neuter:app',
      'less:development',
      'connect:livereload',
      'open',
      'watch'
    ]);
  });

  grunt.registerTask('test', [
    'clean:server',
    'replace:app',
    'concurrent:test',
    'connect:test',
    'neuter:app',
    'mocha'
  ]);

  grunt.registerTask('build', [
    'clean:dist',
    'replace:app',
    'useminPrepare',
    'concurrent:dist',
    'neuter:app',
    'less:production',
    'concat',
    'cssmin',
    //'uglify',
    'copy:dist',
    //'rev',
    'usemin'
  ]);

  grunt.registerTask('default', [
    'jshint',
    'test',
    'build'
  ]);
};