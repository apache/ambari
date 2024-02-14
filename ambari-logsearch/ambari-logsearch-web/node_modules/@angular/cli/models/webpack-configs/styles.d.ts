import * as webpack from 'webpack';
import { WebpackConfigOptions } from '../webpack-config';
/**
 * Enumerate loaders and their dependencies from this file to let the dependency validator
 * know they are used.
 *
 * require('exports-loader')
 * require('style-loader')
 * require('postcss-loader')
 * require('css-loader')
 * require('stylus')
 * require('stylus-loader')
 * require('less')
 * require('less-loader')
 * require('node-sass')
 * require('sass-loader')
 */
export declare function getStylesConfig(wco: WebpackConfigOptions): {
    entry: {
        [key: string]: string[];
    };
    module: {
        rules: webpack.Rule[];
    };
    plugins: any[];
};
