"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
exports.ngAppResolve = (resolvePath) => {
    return path.resolve(process.cwd(), resolvePath);
};
const webpackOutputOptions = {
    colors: true,
    hash: true,
    timings: true,
    chunks: true,
    chunkModules: false,
    children: false,
    modules: false,
    reasons: false,
    warnings: true,
    assets: false,
    version: false
};
const verboseWebpackOutputOptions = {
    children: true,
    assets: true,
    version: true,
    reasons: true,
    chunkModules: false // TODO: set to true when console to file output is fixed
};
function getWebpackStatsConfig(verbose = false) {
    return verbose
        ? Object.assign(webpackOutputOptions, verboseWebpackOutputOptions)
        : webpackOutputOptions;
}
exports.getWebpackStatsConfig = getWebpackStatsConfig;
// Filter extra entries out of a arran of extraEntries
function lazyChunksFilter(extraEntries) {
    return extraEntries
        .filter(extraEntry => extraEntry.lazy)
        .map(extraEntry => extraEntry.entry);
}
exports.lazyChunksFilter = lazyChunksFilter;
// convert all extra entries into the object representation, fill in defaults
function extraEntryParser(extraEntries, appRoot, defaultEntry) {
    return extraEntries
        .map((extraEntry) => typeof extraEntry === 'string' ? { input: extraEntry } : extraEntry)
        .map((extraEntry) => {
        extraEntry.path = path.resolve(appRoot, extraEntry.input);
        if (extraEntry.output) {
            extraEntry.entry = extraEntry.output.replace(/\.(js|css)$/i, '');
        }
        else if (extraEntry.lazy) {
            extraEntry.entry = extraEntry.input.replace(/\.(js|css|scss|sass|less|styl)$/i, '');
        }
        else {
            extraEntry.entry = defaultEntry;
        }
        return extraEntry;
    });
}
exports.extraEntryParser = extraEntryParser;
function getOutputHashFormat(option, length = 20) {
    /* tslint:disable:max-line-length */
    const hashFormats = {
        none: { chunk: '', extract: '', file: '', script: '' },
        media: { chunk: '', extract: '', file: `.[hash:${length}]`, script: '' },
        bundles: { chunk: `.[chunkhash:${length}]`, extract: `.[contenthash:${length}]`, file: '', script: '.[hash]' },
        all: { chunk: `.[chunkhash:${length}]`, extract: `.[contenthash:${length}]`, file: `.[hash:${length}]`, script: '.[hash]' },
    };
    /* tslint:enable:max-line-length */
    return hashFormats[option] || hashFormats['none'];
}
exports.getOutputHashFormat = getOutputHashFormat;
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/utils.js.map