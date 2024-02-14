"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs-extra");
const path = require("path");
const chalk = require("chalk");
const webpack = require("webpack");
const url = require("url");
const common_tags_1 = require("common-tags");
const utils_1 = require("../models/webpack-configs/utils");
const webpack_config_1 = require("../models/webpack-config");
const config_1 = require("../models/config");
const app_utils_1 = require("../utilities/app-utils");
const stats_1 = require("../utilities/stats");
const WebpackDevServer = require('webpack-dev-server');
const Task = require('../ember-cli/lib/models/task');
const SilentError = require('silent-error');
const opn = require('opn');
const yellow = require('chalk').yellow;
function findDefaultServePath(baseHref, deployUrl) {
    if (!baseHref && !deployUrl) {
        return '';
    }
    if (/^(\w+:)?\/\//.test(baseHref) || /^(\w+:)?\/\//.test(deployUrl)) {
        // If baseHref or deployUrl is absolute, unsupported by ng serve
        return null;
    }
    // normalize baseHref
    // for ng serve the starting base is always `/` so a relative
    // and root relative value are identical
    const baseHrefParts = (baseHref || '')
        .split('/')
        .filter(part => part !== '');
    if (baseHref && !baseHref.endsWith('/')) {
        baseHrefParts.pop();
    }
    const normalizedBaseHref = baseHrefParts.length === 0 ? '/' : `/${baseHrefParts.join('/')}/`;
    if (deployUrl && deployUrl[0] === '/') {
        if (baseHref && baseHref[0] === '/' && normalizedBaseHref !== deployUrl) {
            // If baseHref and deployUrl are root relative and not equivalent, unsupported by ng serve
            return null;
        }
        return deployUrl;
    }
    // Join together baseHref and deployUrl
    return `${normalizedBaseHref}${deployUrl || ''}`;
}
exports.default = Task.extend({
    run: function (serveTaskOptions, rebuildDoneCb) {
        const ui = this.ui;
        let webpackCompiler;
        const projectConfig = config_1.CliConfig.fromProject().config;
        const appConfig = app_utils_1.getAppFromConfig(serveTaskOptions.app);
        const outputPath = serveTaskOptions.outputPath || appConfig.outDir;
        if (this.project.root === path.resolve(outputPath)) {
            throw new SilentError('Output path MUST not be project root directory!');
        }
        if (projectConfig.project && projectConfig.project.ejected) {
            throw new SilentError('An ejected project cannot use the build command anymore.');
        }
        if (appConfig.platform === 'server') {
            throw new SilentError('ng serve for platform server applications is coming soon!');
        }
        if (serveTaskOptions.deleteOutputPath) {
            fs.removeSync(path.resolve(this.project.root, outputPath));
        }
        const serveDefaults = {
            // default deployUrl to '' on serve to prevent the default from .angular-cli.json
            deployUrl: ''
        };
        serveTaskOptions = Object.assign({}, serveDefaults, serveTaskOptions);
        let webpackConfig = new webpack_config_1.NgCliWebpackConfig(serveTaskOptions, appConfig).buildConfig();
        const serverAddress = url.format({
            protocol: serveTaskOptions.ssl ? 'https' : 'http',
            hostname: serveTaskOptions.host === '0.0.0.0' ? 'localhost' : serveTaskOptions.host,
            port: serveTaskOptions.port.toString()
        });
        if (serveTaskOptions.disableHostCheck) {
            ui.writeLine(common_tags_1.oneLine `
          ${yellow('WARNING')} Running a server with --disable-host-check is a security risk.
          See https://medium.com/webpack/webpack-dev-server-middleware-security-issues-1489d950874a
          for more information.
        `);
        }
        let clientAddress = serverAddress;
        if (serveTaskOptions.publicHost) {
            let publicHost = serveTaskOptions.publicHost;
            if (!/^\w+:\/\//.test(publicHost)) {
                publicHost = `${serveTaskOptions.ssl ? 'https' : 'http'}://${publicHost}`;
            }
            const clientUrl = url.parse(publicHost);
            serveTaskOptions.publicHost = clientUrl.host;
            clientAddress = url.format(clientUrl);
        }
        if (serveTaskOptions.liveReload) {
            // This allows for live reload of page when changes are made to repo.
            // https://webpack.github.io/docs/webpack-dev-server.html#inline-mode
            let entryPoints = [
                `webpack-dev-server/client?${clientAddress}`
            ];
            if (serveTaskOptions.hmr) {
                const webpackHmrLink = 'https://webpack.github.io/docs/hot-module-replacement.html';
                ui.writeLine(common_tags_1.oneLine `
          ${yellow('NOTICE')} Hot Module Replacement (HMR) is enabled for the dev server.
        `);
                const showWarning = config_1.CliConfig.fromGlobal().get('warnings.hmrWarning');
                if (showWarning) {
                    ui.writeLine('  The project will still live reload when HMR is enabled,');
                    ui.writeLine('  but to take advantage of HMR additional application code is required');
                    ui.writeLine('  (not included in an Angular CLI project by default).');
                    ui.writeLine(`  See ${chalk.blue(webpackHmrLink)}`);
                    ui.writeLine('  for information on working with HMR for Webpack.');
                    ui.writeLine(common_tags_1.oneLine `
            ${yellow('To disable this warning use "ng set --global warnings.hmrWarning=false"')}
          `);
                }
                entryPoints.push('webpack/hot/dev-server');
                webpackConfig.plugins.push(new webpack.HotModuleReplacementPlugin());
                if (serveTaskOptions.extractCss) {
                    ui.writeLine(common_tags_1.oneLine `
            ${yellow('NOTICE')} (HMR) does not allow for CSS hot reload when used
            together with '--extract-css'.
          `);
                }
            }
            if (!webpackConfig.entry.main) {
                webpackConfig.entry.main = [];
            }
            webpackConfig.entry.main.unshift(...entryPoints);
        }
        else if (serveTaskOptions.hmr) {
            ui.writeLine(yellow('Live reload is disabled. HMR option ignored.'));
        }
        if (!serveTaskOptions.watch) {
            // There's no option to turn off file watching in webpack-dev-server, but
            // we can override the file watcher instead.
            webpackConfig.plugins.unshift({
                apply: (compiler) => {
                    compiler.plugin('after-environment', () => {
                        compiler.watchFileSystem = { watch: () => { } };
                    });
                }
            });
        }
        webpackCompiler = webpack(webpackConfig);
        if (rebuildDoneCb) {
            webpackCompiler.plugin('done', rebuildDoneCb);
        }
        const statsConfig = utils_1.getWebpackStatsConfig(serveTaskOptions.verbose);
        let proxyConfig = {};
        if (serveTaskOptions.proxyConfig) {
            const proxyPath = path.resolve(this.project.root, serveTaskOptions.proxyConfig);
            if (fs.existsSync(proxyPath)) {
                proxyConfig = require(proxyPath);
            }
            else {
                const message = 'Proxy config file ' + proxyPath + ' does not exist.';
                return Promise.reject(new SilentError(message));
            }
        }
        let sslKey = null;
        let sslCert = null;
        if (serveTaskOptions.ssl) {
            const keyPath = path.resolve(this.project.root, serveTaskOptions.sslKey);
            if (fs.existsSync(keyPath)) {
                sslKey = fs.readFileSync(keyPath, 'utf-8');
            }
            const certPath = path.resolve(this.project.root, serveTaskOptions.sslCert);
            if (fs.existsSync(certPath)) {
                sslCert = fs.readFileSync(certPath, 'utf-8');
            }
        }
        let servePath = serveTaskOptions.servePath;
        if (!servePath && servePath !== '') {
            const defaultServePath = findDefaultServePath(serveTaskOptions.baseHref, serveTaskOptions.deployUrl);
            if (defaultServePath == null) {
                ui.writeLine(common_tags_1.oneLine `
            ${chalk.yellow('WARNING')} --deploy-url and/or --base-href contain
            unsupported values for ng serve.  Default serve path of '/' used.
            Use --serve-path to override.
          `);
            }
            servePath = defaultServePath || '';
        }
        if (servePath.endsWith('/')) {
            servePath = servePath.substr(0, servePath.length - 1);
        }
        if (!servePath.startsWith('/')) {
            servePath = `/${servePath}`;
        }
        const webpackDevServerConfiguration = {
            headers: { 'Access-Control-Allow-Origin': '*' },
            historyApiFallback: {
                index: `${servePath}/${appConfig.index}`,
                disableDotRule: true,
                htmlAcceptHeaders: ['text/html', 'application/xhtml+xml']
            },
            stats: serveTaskOptions.verbose ? statsConfig : 'none',
            inline: true,
            proxy: proxyConfig,
            compress: serveTaskOptions.target === 'production',
            watchOptions: {
                poll: serveTaskOptions.poll
            },
            https: serveTaskOptions.ssl,
            overlay: {
                errors: serveTaskOptions.target === 'development',
                warnings: false
            },
            contentBase: false,
            public: serveTaskOptions.publicHost,
            disableHostCheck: serveTaskOptions.disableHostCheck,
            publicPath: servePath
        };
        if (sslKey != null && sslCert != null) {
            webpackDevServerConfiguration.key = sslKey;
            webpackDevServerConfiguration.cert = sslCert;
        }
        webpackDevServerConfiguration.hot = serveTaskOptions.hmr;
        if (serveTaskOptions.target === 'production') {
            ui.writeLine(chalk.red(common_tags_1.stripIndents `
        ****************************************************************************************
        This is a simple server for use in testing or debugging Angular applications locally.
        It hasn't been reviewed for security issues.

        DON'T USE IT FOR PRODUCTION USE!
        ****************************************************************************************
      `));
        }
        ui.writeLine(chalk.green(common_tags_1.oneLine `
      **
      NG Live Development Server is listening on ${serveTaskOptions.host}:${serveTaskOptions.port},
      open your browser on ${serverAddress}${servePath}
      **
    `));
        const server = new WebpackDevServer(webpackCompiler, webpackDevServerConfiguration);
        if (!serveTaskOptions.verbose) {
            webpackCompiler.plugin('done', (stats) => {
                const json = stats.toJson('verbose');
                this.ui.writeLine(stats_1.statsToString(json, statsConfig));
                if (stats.hasWarnings()) {
                    this.ui.writeLine(stats_1.statsWarningsToString(json, statsConfig));
                }
                if (stats.hasErrors()) {
                    this.ui.writeError(stats_1.statsErrorsToString(json, statsConfig));
                }
            });
        }
        return new Promise((_resolve, reject) => {
            const httpServer = server.listen(serveTaskOptions.port, serveTaskOptions.host, (err, _stats) => {
                if (err) {
                    return reject(err);
                }
                if (serveTaskOptions.open) {
                    opn(serverAddress);
                }
            });
            // Node 8.0 - 8.4 has a keepAliveTimeout bug which doesn't respect active connections.
            // Connections will end after ~5 seconds (arbitrary), often not letting the full download
            // of large pieces of content, such as a vendor javascript file.  This results in browsers
            // throwing a "net::ERR_CONTENT_LENGTH_MISMATCH" error.
            // https://github.com/angular/angular-cli/issues/7197
            // https://github.com/nodejs/node/issues/13391
            // https://github.com/nodejs/node/commit/2cb6f2b281eb96a7abe16d58af6ebc9ce23d2e96
            if (/^v8.[0-4].\d+$/.test(process.version)) {
                httpServer.keepAliveTimeout = 30000; // 30 seconds
            }
        })
            .catch((err) => {
            if (err) {
                this.ui.writeError('\nAn error occured during the build:\n' + ((err && err.stack) || err));
            }
            throw err;
        });
    }
});
//# sourceMappingURL=/users/hansl/sources/angular-cli/tasks/serve.js.map