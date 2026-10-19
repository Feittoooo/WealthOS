import { defineConfig } from 'vitest/config';

// Unit tests for plain TypeScript (formatting, message parsing, calculations).
// Components are checked by the Angular compiler with strictTemplates.
export default defineConfig({
  test: {
    include: ['src/**/*.spec.ts'],
    environment: 'node'
  }
});
