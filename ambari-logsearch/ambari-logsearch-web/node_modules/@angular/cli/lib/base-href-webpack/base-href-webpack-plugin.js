"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class BaseHrefWebpackPlugin {
    constructor(options) {
        this.options = options;
    }
    apply(compiler) {
        // Ignore if baseHref is not passed
        if (!this.options.baseHref && this.options.baseHref !== '') {
            return;
        }
        compiler.plugin('compilation', (compilation) => {
            compilation.plugin('html-webpack-plugin-before-html-processing', (htmlPluginData, callback) => {
                // Check if base tag already exists
                const baseTagRegex = /<base.*?>/i;
                const baseTagMatches = htmlPluginData.html.match(baseTagRegex);
                if (!baseTagMatches) {
                    // Insert it in top of the head if not exist
                    htmlPluginData.html = htmlPluginData.html.replace(/<head>/i, '$&' + `<base href="${this.options.baseHref}">`);
                }
                else {
                    // Replace only href attribute if exists
                    const modifiedBaseTag = baseTagMatches[0].replace(/href="\S+"/i, `href="${this.options.baseHref}"`);
                    htmlPluginData.html = htmlPluginData.html.replace(baseTagRegex, modifiedBaseTag);
                }
                callback(null, htmlPluginData);
            });
        });
    }
}
exports.BaseHrefWebpackPlugin = BaseHrefWebpackPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/lib/base-href-webpack/base-href-webpack-plugin.js.map