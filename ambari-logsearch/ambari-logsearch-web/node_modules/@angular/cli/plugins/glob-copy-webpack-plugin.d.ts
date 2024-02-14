import { AssetPattern } from '../models/webpack-configs/utils';
export interface GlobCopyWebpackPluginOptions {
    patterns: (string | AssetPattern)[];
    globOptions: any;
}
export declare class GlobCopyWebpackPlugin {
    private options;
    constructor(options: GlobCopyWebpackPluginOptions);
    apply(compiler: any): void;
}
