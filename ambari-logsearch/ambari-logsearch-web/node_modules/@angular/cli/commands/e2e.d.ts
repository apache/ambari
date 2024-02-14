import { ServeTaskOptions } from './serve';
export interface E2eTaskOptions extends ServeTaskOptions {
    config: string;
    serve: boolean;
    webdriverUpdate: boolean;
    specs: string[];
    elementExplorer: boolean;
}
declare const E2eCommand: any;
export default E2eCommand;
