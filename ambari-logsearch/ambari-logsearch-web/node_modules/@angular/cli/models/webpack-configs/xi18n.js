"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const webpack_1 = require("@ngtools/webpack");
exports.getWebpackExtractI18nConfig = function (projectRoot, appConfig, genDir, i18nFormat, locale, outFile) {
    let exclude = [];
    if (appConfig.test) {
        exclude.push(path.join(projectRoot, appConfig.root, appConfig.test));
    }
    return {
        plugins: [
            new webpack_1.ExtractI18nPlugin({
                tsConfigPath: path.resolve(projectRoot, appConfig.root, appConfig.tsconfig),
                exclude: exclude,
                genDir: genDir,
                i18nFormat: i18nFormat,
                locale: locale,
                outFile: outFile,
            })
        ]
    };
};
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/xi18n.js.map