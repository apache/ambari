import { Configuration } from "./configuration";
export declare class SpecReporter {
    private started;
    private finished;
    private display;
    private metrics;
    private configuration;
    constructor(configuration?: Configuration);
    jasmineStarted(info: any): void;
    jasmineDone(info: any): void;
    suiteStarted(suite: any): void;
    suiteDone(): void;
    specStarted(spec: any): void;
    specDone(spec: any): void;
}
