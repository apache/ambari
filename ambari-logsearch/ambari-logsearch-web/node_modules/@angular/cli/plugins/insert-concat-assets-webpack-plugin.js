"use strict";
// Add assets from `ConcatPlugin` to index.html.
Object.defineProperty(exports, "__esModule", { value: true });
class InsertConcatAssetsWebpackPlugin {
    constructor(entryNames) {
        this.entryNames = entryNames;
        // Priority list of where to insert asset.
        this.insertAfter = [
            /polyfills(\.[0-9a-f]{20})?\.bundle\.js/,
            /inline(\.[0-9a-f]{20})?\.bundle\.js/,
        ];
    }
    apply(compiler) {
        compiler.plugin('compilation', (compilation) => {
            compilation.plugin('html-webpack-plugin-before-html-generation', (htmlPluginData, callback) => {
                const fileNames = this.entryNames.map((entryName) => {
                    const fileName = htmlPluginData.assets.webpackConcat
                        && htmlPluginData.assets.webpackConcat[entryName];
                    if (!fileName) {
                        // Something went wrong and the asset was not correctly added.
                        throw new Error(`Cannot find file for ${entryName} script.`);
                    }
                    if (htmlPluginData.assets.publicPath) {
                        if (htmlPluginData.assets.publicPath.endsWith('/')) {
                            return htmlPluginData.assets.publicPath + fileName;
                        }
                        return htmlPluginData.assets.publicPath + '/' + fileName;
                    }
                    return fileName;
                });
                let insertAt = 0;
                // TODO: try to figure out if there are duplicate bundle names when adding and throw
                for (let el of this.insertAfter) {
                    const jsIdx = htmlPluginData.assets.js.findIndex((js) => js.match(el));
                    if (jsIdx !== -1) {
                        insertAt = jsIdx + 1;
                        break;
                    }
                }
                htmlPluginData.assets.js.splice(insertAt, 0, ...fileNames);
                callback(null, htmlPluginData);
            });
        });
    }
}
exports.InsertConcatAssetsWebpackPlugin = InsertConcatAssetsWebpackPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/plugins/insert-concat-assets-webpack-plugin.js.map