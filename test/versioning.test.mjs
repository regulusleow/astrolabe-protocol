import assert from "node:assert/strict";
import test from "node:test";

import {
  assertReleaseVersion,
  compareReleaseVersions
} from "../scripts/versioning.mjs";

test("accepts stable and prerelease SemVer values", () => {
  assert.equal(assertReleaseVersion("2.0.0"), "2.0.0");
  assert.equal(assertReleaseVersion("2.0.0-rc.1"), "2.0.0-rc.1");
  assert.equal(assertReleaseVersion("2.0.0-beta.2"), "2.0.0-beta.2");
  assert.equal(assertReleaseVersion("2.0.0-1a"), "2.0.0-1a");
});

test("rejects invalid or ambiguous release versions", () => {
  for (const version of ["v2.0.0", "2.0", "02.0.0", "2.0.0-01", "2.0.0+"]) {
    assert.throws(() => assertReleaseVersion(version));
  }
});

test("orders prerelease versions according to SemVer", () => {
  assert.equal(compareReleaseVersions("2.0.0-rc.1", "2.0.0-beta.2"), 1);
  assert.equal(compareReleaseVersions("2.0.0-rc.2", "2.0.0-rc.1"), 1);
  assert.equal(compareReleaseVersions("2.0.0", "2.0.0-rc.2"), 1);
  assert.equal(compareReleaseVersions("2.0.0-rc.1", "2.0.0-rc.1"), 0);
  assert.equal(
    compareReleaseVersions("9007199254740993.0.0", "9007199254740992.0.0"),
    1
  );
});
