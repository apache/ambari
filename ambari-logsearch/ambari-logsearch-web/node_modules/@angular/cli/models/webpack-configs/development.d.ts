import { NamedModulesPlugin } from 'webpack';
import { WebpackConfigOptions } from '../webpack-config';
export declare const getDevConfig: (_wco: WebpackConfigOptions) => {
    plugins: NamedModulesPlugin[];
};
