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

test("Kotlin publication can retry an immutable release tag", async () => {
  const workflow = await readFile(".github/workflows/ci.yml", "utf8");

  assert.match(workflow, /release_tag:/);
  assert.match(workflow, /inputs\.release_tag != ''/);
  assert.match(workflow, /ref: \$\{\{ inputs\.release_tag \|\| github\.ref \}\}/);
  assert.match(
    workflow,
    /RELEASE_TAG: \$\{\{ inputs\.release_tag \|\| github\.ref_name \}\}/
  );
});
