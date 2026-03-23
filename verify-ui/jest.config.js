/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  rootDir: './',
  testPathIgnorePatterns: ["<rootDir>/node_modules"],
  testMatch: ["<rootDir>/src/__tests__/**/*.spec.ts", "<rootDir>/src/__tests__/**/*.spec.tsx", "<rootDir>/src/__tests__/**/*.spec.js"],
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.ts"],
  extensionsToTreatAsEsm: [".ts", ".tsx"],
  transform: {
    '^.+\\.m?[tj]sx?$': ['ts-jest', { useESM: true }],
    "\\.svg$": "<rootDir>/__mocks__/svgFileTransformer.js",
    '.+\\.(png|jpg|jpeg)$': "<rootDir>/__mocks__/svgFileTransformer.js"
  },
  transformIgnorePatterns: [
    "node_modules/(?!(iso-639-3|@injistack)/)"
  ]
};
