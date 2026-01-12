global.window = global.window || {};
window._env_ = {
  DEFAULT_THEME: "default_theme",
  INTERNET_CONNECTIVITY_CHECK_ENDPOINT: "https://dns.google/",
  INTERNET_CONNECTIVITY_CHECK_TIMEOUT: "3000",
};

if (!global.fetch) {
  global.fetch = jest.fn(() =>
    Promise.resolve({
      json: () => Promise.resolve({}),
    })
  );
}
