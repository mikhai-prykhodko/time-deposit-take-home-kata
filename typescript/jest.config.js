// unit: fast, no Docker (yarn test). integration: *.it.test.ts, needs Docker (yarn test:it).
module.exports = {
  projects: [
    {
      displayName: 'unit',
      testMatch: ['<rootDir>/src/**/*.test.ts'],
      testPathIgnorePatterns: ['\\.it\\.test\\.ts$'],
    },
    {
      displayName: 'integration',
      testMatch: ['<rootDir>/src/**/*.it.test.ts'],
    },
  ],
}
