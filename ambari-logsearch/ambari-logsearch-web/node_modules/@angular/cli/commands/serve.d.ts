import { BuildOptions } from '../models/build-options';
export interface ServeTaskOptions extends BuildOptions {
    port?: number;
    host?: string;
    proxyConfig?: string;
    liveReload?: boolean;
    publicHost?: string;
    disableHostCheck?: boolean;
    ssl?: boolean;
    sslKey?: string;
    sslCert?: string;
    open?: boolean;
    hmr?: boolean;
    servePath?: string;
}
export declare const baseServeCommandOptions: any;
declare const ServeCommand: any;
export default ServeCommand;
