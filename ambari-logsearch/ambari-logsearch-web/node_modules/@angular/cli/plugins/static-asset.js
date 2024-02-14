"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class StaticAssetPlugin {
    constructor(name, contents) {
        this.name = name;
        this.contents = contents;
    }
    apply(compiler) {
        compiler.plugin('emit', (compilation, cb) => {
            compilation.assets[this.name] = {
                size: () => this.contents.length,
                source: () => this.contents,
            };
            cb();
        });
    }
}
exports.StaticAssetPlugin = StaticAssetPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/plugins/static-asset.js.map