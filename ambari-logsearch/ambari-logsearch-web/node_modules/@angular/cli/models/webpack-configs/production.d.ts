import { WebpackConfigOptions } from '../webpack-config';
export declare const getProdConfig: (wco: WebpackConfigOptions) => {
    entry: {
        [key: string]: string[];
    };
    plugins: any[];
};
