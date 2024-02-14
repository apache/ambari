export interface BaseHrefWebpackPluginOptions {
    baseHref: string;
}
export declare class BaseHrefWebpackPlugin {
    readonly options: BaseHrefWebpackPluginOptions;
    constructor(options: BaseHrefWebpackPluginOptions);
    apply(compiler: any): void;
}
