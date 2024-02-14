import { BuildOptions } from '../models/build-options';
export declare const baseEjectCommandOptions: any;
export interface EjectTaskOptions extends BuildOptions {
    force?: boolean;
    app?: string;
}
declare const EjectCommand: any;
export default EjectCommand;
