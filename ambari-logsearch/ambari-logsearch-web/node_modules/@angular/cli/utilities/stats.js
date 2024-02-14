"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const common_tags_1 = require("common-tags");
// Force basic color support on terminals with no color support.
// Chalk typings don't have the correct constructor parameters.
const chalkCtx = new chalk.constructor(chalk.supportsColor ? {} : { level: 1 });
const { bold, green, red, reset, white, yellow } = chalkCtx;
function _formatSize(size) {
    if (size <= 0) {
        return '0 bytes';
    }
    const abbreviations = ['bytes', 'kB', 'MB', 'GB'];
    const index = Math.floor(Math.log(size) / Math.log(1000));
    return `${+(size / Math.pow(1000, index)).toPrecision(3)} ${abbreviations[index]}`;
}
function statsToString(json, statsConfig) {
    const colors = statsConfig.colors;
    const rs = (x) => colors ? reset(x) : x;
    const w = (x) => colors ? bold(white(x)) : x;
    const g = (x) => colors ? bold(green(x)) : x;
    const y = (x) => colors ? bold(yellow(x)) : x;
    return rs(common_tags_1.stripIndents `
    Date: ${w(new Date().toISOString())}
    Hash: ${w(json.hash)}
    Time: ${w('' + json.time)}ms
    ${json.chunks.map((chunk) => {
        const asset = json.assets.filter((x) => x.name == chunk.files[0])[0];
        const size = asset ? ` ${_formatSize(asset.size)}` : '';
        const files = chunk.files.join(', ');
        const names = chunk.names ? ` (${chunk.names.join(', ')})` : '';
        const parents = chunk.parents.map((id) => ` {${y(id)}}`).join('');
        const initial = y(chunk.entry ? '[entry]' : chunk.initial ? '[initial]' : '');
        const flags = ['rendered', 'recorded']
            .map(f => f && chunk[f] ? g(` [${f}]`) : '')
            .join('');
        return `chunk {${y(chunk.id)}} ${g(files)}${names}${size}${parents} ${initial}${flags}`;
    }).join('\n')}
    `);
}
exports.statsToString = statsToString;
function statsWarningsToString(json, statsConfig) {
    const colors = statsConfig.colors;
    const rs = (x) => colors ? reset(x) : x;
    const y = (x) => colors ? bold(yellow(x)) : x;
    return rs('\n' + json.warnings.map((warning) => y(`WARNING in ${warning}`)).join('\n\n'));
}
exports.statsWarningsToString = statsWarningsToString;
function statsErrorsToString(json, statsConfig) {
    const colors = statsConfig.colors;
    const rs = (x) => colors ? reset(x) : x;
    const r = (x) => colors ? bold(red(x)) : x;
    return rs('\n' + json.errors.map((error) => r(`ERROR in ${error}`)).join('\n'));
}
exports.statsErrorsToString = statsErrorsToString;
//# sourceMappingURL=/users/hansl/sources/angular-cli/utilities/stats.js.map