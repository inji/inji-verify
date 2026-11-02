import { afterEach, describe, expect, jest, test } from "@jest/globals";

const consoleMethods = { log: console.log, info: console.info, debug: console.debug, assert: console.assert };

afterEach(() => {
  Object.assign(console, consoleMethods);
  jest.restoreAllMocks();
});

describe("console initialization", () => {
  test.each(["production", "test"])("suppresses verbose console methods in %s", (environment) => {
    jest.replaceProperty(process, "env", { ...process.env, NODE_ENV: environment });
    const error = console.error;
    const warn = console.warn;
    jest.isolateModules(() => require("../setupEnvironment"));
    for (const method of ["log", "info", "debug", "assert"] as const) {
      expect(console[method]).not.toBe(consoleMethods[method]);
    }
    expect(console.error).toBe(error);
    expect(console.warn).toBe(warn);
  });

  test("preserves development console methods", () => {
    jest.replaceProperty(process, "env", { ...process.env, NODE_ENV: "development" });
    jest.isolateModules(() => require("../setupEnvironment"));
    for (const method of ["log", "info", "debug", "assert"] as const) {
      expect(console[method]).toBe(consoleMethods[method]);
    }
  });
});
