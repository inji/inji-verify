describe("Promise.withResolvers polyfill", () => {
  const originalDescriptor = Object.getOwnPropertyDescriptor(Promise, "withResolvers");

  afterEach(() => {
    if (originalDescriptor) {
      Object.defineProperty(Promise, "withResolvers", originalDescriptor);
    } else {
      delete (Promise as any).withResolvers;
    }
    jest.resetModules();
  });

  it("adds Promise.withResolvers when the browser does not provide it", async () => {
    Object.defineProperty(Promise, "withResolvers", {
      configurable: true,
      value: undefined,
      writable: true,
    });

    jest.isolateModules(() => {
      require("../../src/utils/promisePolyfill");
    });

    const deferred = (Promise as any).withResolvers();
    deferred.resolve("ready");

    await expect(deferred.promise).resolves.toBe("ready");
  });
});
