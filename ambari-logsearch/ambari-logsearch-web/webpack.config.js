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

const fs = require('fs');
const path = require('path');
const ConcatPlugin = require('webpack-concat-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const ProgressPlugin = require('webpack/lib/ProgressPlugin');
const CircularDependencyPlugin = require('circular-dependency-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const autoprefixer = require('autoprefixer');
const postcssUrl = require('postcss-url');
const cssnano = require('cssnano');

const {NoEmitOnErrorsPlugin, SourceMapDevToolPlugin, NamedModulesPlugin} = require('webpack');
const {InsertConcatAssetsWebpackPlugin, NamedLazyChunksWebpackPlugin, BaseHrefWebpackPlugin} = require('@angular/cli/plugins/webpack');
const {CommonsChunkPlugin} = require('webpack').optimize;
const {AotPlugin} = require('@ngtools/webpack');

const nodeModules = path.join(process.cwd(), 'node_modules');
const realNodeModules = fs.realpathSync(nodeModules);
const genDirNodeModules = path.join(process.cwd(), 'src', '$$_gendir', 'node_modules');
const entryPoints = ["inline","polyfills","sw-register","styles","vendor","main"];
const minimizeCss = false;
const baseHref = "";
const deployUrl = "";
const postcssPlugins = function () {
        // safe settings based on: https://github.com/ben-eb/cssnano/issues/358#issuecomment-283696193
        const importantCommentRe = /@preserve|@license|[@#]\s*source(?:Mapping)?URL|^!/i;
        const minimizeOptions = {
            autoprefixer: false,
            safe: true,
            mergeLonghand: false,
            discardComments: { remove: (comment) => !importantCommentRe.test(comment) }
        };
        return [
            postcssUrl({
                url: (URL) => {
                    // Only convert root relative URLs, which CSS-Loader won't process into require().
                    if (!URL.startsWith('/') || URL.startsWith('//')) {
                        return URL;
                    }
                    if (deployUrl.match(/:\/\//)) {
                        // If deployUrl contains a scheme, ignore baseHref use deployUrl as is.
                        return `${deployUrl.replace(/\/$/, '')}${URL}`;
                    }
                    else if (baseHref.match(/:\/\//)) {
                        // If baseHref contains a scheme, include it as is.
                        return baseHref.replace(/\/$/, '') +
                            `/${deployUrl}/${URL}`.replace(/\/\/+/g, '/');
                    }
                    else {
                        // Join together base-href, deploy-url and the original URL.
                        // Also dedupe multiple slashes into single ones.
                        return `/${baseHref}/${deployUrl}/${URL}`.replace(/\/\/+/g, '/');
                    }
                }
            }),
            autoprefixer(),
        ].concat(minimizeCss ? [cssnano(minimizeOptions)] : []);
    };

const resourcesDirName = 'resources';

module.exports = {
  "resolve": {
    "extensions": [
      ".ts",
      ".js",
      ".less"
    ],
    "modules": [
      "node_modules"
    ],
    "symlinks": true
  },
  "resolveLoader": {
    "modules": [
      "node_modules"
    ]
  },
  "entry": {
    "main": [
      "./src/main.ts"
    ],
    "polyfills": [
      "./src/polyfills.ts"
    ],
    "styles": [
      "./node_modules/bootstrap/dist/css/bootstrap.min.css",
      "./node_modules/font-awesome/css/font-awesome.min.css",
      "./src/vendor/css/bootstrap-logsearch.min.css",
      "./src/vendor/css/bootstrap-datetimepicker.min.css",
      "./src/styles.less"
    ]
  },
  "output": {
    "path": path.join(process.cwd(), "dist"),
    "publicPath": "",
    "filename": `${resourcesDirName}/[name].bundle.js`,
    "chunkFilename": "[id].chunk.js"
  },
  "module": {
    "rules": [
      {
        "enforce": "pre",
        "test": /\.js$/,
        "loader": "source-map-loader",
        "exclude": [
          /(\\|\/)node_modules(\\|\/)/
        ]
      },
      {
        "test": /\.html$/,
        "loader": "raw-loader"
      },
      {
        "test": /\.(eot|svg|cur)$/,
        "loader": `file-loader?name=${resourcesDirName}/[name].[ext]`
      },
      {
        "test": /\.(jpg|png|webp|gif|otf|ttf|woff|woff2|ani)$/,
        "loader": `url-loader?name=${resourcesDirName}/[name].[ext]&limit=10000`
      },
      {
        "exclude": [
          path.join(process.cwd(), "src/styles.less"),
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css")
        ],
        "test": /\.css$/,
        "use": [
          "exports-loader?module.exports.toString()",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          }
        ]
      },
      {
        "exclude": [
          path.join(process.cwd(), "src/styles.less"),
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css")
        ],
        "test": /\.scss$|\.sass$/,
        "use": [
          "exports-loader?module.exports.toString()",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "sass-loader",
            "options": {
              "sourceMap": false,
              "precision": 8,
              "includePaths": []
            }
          }
        ]
      },
      {
        "exclude": [
          path.join(process.cwd(), "src/styles.less"),
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css")
        ],
        "test": /\.less$/,
        "use": [
          "exports-loader?module.exports.toString()",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "less-loader",
            "options": {
              "sourceMap": false,
              "paths": [
                "./node_modules"
              ]
            }
          }
        ]
      },
      {
        "exclude": [
          path.join(process.cwd(), "src/styles.less"),
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css")
        ],
        "test": /\.styl$/,
        "use": [
          "exports-loader?module.exports.toString()",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "stylus-loader",
            "options": {
              "sourceMap": false,
              "paths": []
            }
          }
        ]
      },
      {
        "include": [
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css"),
          path.join(process.cwd(), "src/styles.less")
        ],
        "test": /\.css$/,
        "use": [
          "style-loader",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          }
        ]
      },
      {
        "include": [
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css"),
          path.join(process.cwd(), "src/styles.less")
        ],
        "test": /\.scss$|\.sass$/,
        "use": [
          "style-loader",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "sass-loader",
            "options": {
              "sourceMap": false,
              "precision": 8,
              "includePaths": []
            }
          }
        ]
      },
      {
        "include": [
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css"),
          path.join(process.cwd(), "src/styles.less")
        ],
        "test": /\.less$/,
        "use": [
          "style-loader",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "less-loader",
            "options": {
              "sourceMap": false,
              "paths": ["./node_modules"]
            }
          }
        ]
      },
      {
        "include": [
          path.join(process.cwd(), "node_modules/bootstrap/dist/css/bootstrap.min.css"),
          path.join(process.cwd(), "node_modules/font-awesome/css/font-awesome.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-logsearch.min.css"),
          path.join(process.cwd(), "src/vendor/css/bootstrap-datetimepicker.min.css"),
          path.join(process.cwd(), "src/styles.less")
        ],
        "test": /\.styl$/,
        "use": [
          "style-loader",
          {
            "loader": "css-loader",
            "options": {
              "sourceMap": false,
              "importLoaders": 1
            }
          },
          {
            "loader": "postcss-loader",
            "options": {
              "ident": "postcss",
              "plugins": postcssPlugins
            }
          },
          {
            "loader": "stylus-loader",
            "options": {
              "sourceMap": false,
              "paths": []
            }
          }
        ]
      },
      {
        "test": /\.ts$/,
        "loader": "@ngtools/webpack"
      }
    ]
  },
  "plugins": [
    new NoEmitOnErrorsPlugin(),
    new ConcatPlugin({
      "uglify": false,
      "sourceMap": true,
      "name": "scripts",
      "fileName": `${resourcesDirName}/[name].bundle.js`,
      "filesToConcat": [
        "node_modules/jquery/dist/jquery.min.js",
        "node_modules/bootstrap/dist/js/bootstrap.min.js",
        "src/vendor/js/bootstrap-logsearch.min.js"
      ]
    }),
    new InsertConcatAssetsWebpackPlugin([
      "scripts"
    ]),
    new CopyWebpackPlugin([
      {
        "context": "src/",
        "to": resourcesDirName,
        "from": {
          "glob": "assets/**/*",
          "dot": true
        }
      },
      {
        "context": "src/",
        "to": "favicon.ico",
        "from": {
          "glob": "favicon.ico",
          "dot": true
        }
      }
    ], {
      "ignore": [
        ".gitkeep"
      ],
      "debug": "warning"
    }),
    new ProgressPlugin(),
    new CircularDependencyPlugin({
      "exclude": /(\\|\/)node_modules(\\|\/)/,
      "failOnError": false
    }),
    new NamedLazyChunksWebpackPlugin(),
    new HtmlWebpackPlugin({
      "template": "./src/index.html",
      "filename": "index.html",
      "hash": false,
      "inject": true,
      "compile": true,
      "favicon": false,
      "minify": false,
      "cache": true,
      "showErrors": true,
      "chunks": "all",
      "excludeChunks": [],
      "title": "Webpack App",
      "xhtml": true,
      "chunksSortMode": function sort(left, right) {
        let leftIndex = entryPoints.indexOf(left.names[0]);
        let rightindex = entryPoints.indexOf(right.names[0]);
        if (leftIndex > rightindex) {
            return 1;
        }
        else if (leftIndex < rightindex) {
            return -1;
        }
        else {
            return 0;
        }
    }
    }),
    new BaseHrefWebpackPlugin({}),
    new CommonsChunkPlugin({
      "name": [
        "inline"
      ],
      "minChunks": null
    }),
    new CommonsChunkPlugin({
      "name": [
        "vendor"
      ],
      "minChunks": (module) => {
                return module.resource
                    && (module.resource.startsWith(nodeModules)
                        || module.resource.startsWith(genDirNodeModules)
                        || module.resource.startsWith(realNodeModules));
            },
      "chunks": [
        "main"
      ]
    }),
    new SourceMapDevToolPlugin({
      "filename": "[file].map[query]",
      "moduleFilenameTemplate": "[resource-path]",
      "fallbackModuleFilenameTemplate": "[resource-path]?[hash]",
      "sourceRoot": "webpack:///"
    }),
    new CommonsChunkPlugin({
      "name": [
        "main"
      ],
      "minChunks": 2,
      "async": "common"
    }),
    new NamedModulesPlugin({}),
    new AotPlugin({
      "mainPath": "main.ts",
      "replaceExport": false,
      "hostReplacementPaths": {
        "environments/environment.ts": `environments/environment${process.env.NODE_ENV === "production" ? ".prod" : ""}.ts`
      },
      "exclude": [],
      "tsConfigPath": "src/tsconfig.app.json",
      "skipCodeGeneration": true
    })
  ],
  "node": {
    "fs": "empty",
    "global": true,
    "crypto": "empty",
    "tls": "empty",
    "net": "empty",
    "process": true,
    "module": false,
    "clearImmediate": false,
    "setImmediate": false
  },
  "devServer": {
    "historyApiFallback": true
  }
};
