import { WebpackConfigOptions } from '../webpack-config';
/**
 * Returns a partial specific to creating a bundle for node
 * @param _wco Options which are include the build options and app config
 */
export declare const getServerConfig: (_wco: WebpackConfigOptions) => {
    target: string;
    output: {
        libraryTarget: string;
    };
    externals: (RegExp | ((_: any, request: any, callback: (error?: any, result?: any) => void) => void))[];
};
