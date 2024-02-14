import { SpecReporter } from "jasmine-spec-reporter";
import { DisplayProcessor } from "jasmine-spec-reporter";
const Jasmine = require("jasmine");

class CustomProcessor extends DisplayProcessor {
    public displayJasmineStarted(info: any, log: String): String {
        return `TypeScript ${log}`;
    }
}

const jrunner = new Jasmine();
jrunner.env.clearReporters();
jrunner.addReporter(new SpecReporter({
    customProcessors: [CustomProcessor],
}));
jrunner.loadConfigFile();
jrunner.execute();
