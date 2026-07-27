import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

test("Kotlin publication disables the unsupported configuration cache", async () => {
  const workflow = await readFile(".github/workflows/ci.yml", "utf8");

  assert.match(
    workflow,
    /--no-configuration-cache\s+:AstrolabeProtocolKotlin:publishAndReleaseToMavenCentral/
  );
});
