import 'rxjs/add/observable/empty';
import { Logger } from './logger';
export declare class NullLogger extends Logger {
    constructor(parent?: Logger | null);
}
