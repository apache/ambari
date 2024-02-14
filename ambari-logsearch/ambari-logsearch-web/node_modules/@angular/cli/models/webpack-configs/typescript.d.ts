import { AotPlugin } from '@ngtools/webpack';
import { WebpackConfigOptions } from '../webpack-config';
export declare const getNonAotConfig: (wco: WebpackConfigOptions) => {
    module: {
        rules: {
            test: RegExp;
            loader: string;
        }[];
    };
    plugins: AotPlugin[];
};
export declare const getAotConfig: (wco: WebpackConfigOptions) => {
    module: {
        rules: {
            test: RegExp;
            use: any[];
        }[];
    };
    plugins: AotPlugin[];
};
export declare const getNonAotTestConfig: (wco: WebpackConfigOptions) => {
    module: {
        rules: {
            test: RegExp;
            loader: string;
        }[];
    };
    plugins: AotPlugin[];
};
