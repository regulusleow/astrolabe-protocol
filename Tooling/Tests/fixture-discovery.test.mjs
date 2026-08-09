import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import {
  mkdirSync,
  mkdtempSync,
  rmSync,
  symlinkSync,
  writeFileSync
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

import { discoverFixturePaths } from "../ContractValidation/fixture-discovery.mjs";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const validatorPath = join(
  repositoryRoot,
  "Tooling/ContractValidation/validate-contract.mjs"
);

test("contract validation discovers nested unregistered fixtures", () => {
  const fixtureDirectory = mkdtempSync(
    join(repositoryRoot, "Contract/v2/Fixtures/valid/fixture-discovery-test-")
  );
  const fixturePath = join(fixtureDirectory, "unregistered.json");

  try {
    mkdirSync(fixtureDirectory, { recursive: true });
    writeFileSync(fixturePath, "{}\n");

    const result = spawnSync(process.execPath, [validatorPath], {
      cwd: repositoryRoot,
      encoding: "utf8"
    });

    assert.notEqual(result.status, 0);
    assert.match(
      `${result.stdout}\n${result.stderr}`,
      /Unregistered V2 fixtures found: .*fixture-discovery-test-[^/]+\/unregistered\.json/
    );
  } finally {
    rmSync(fixtureDirectory, { recursive: true, force: true });
  }
});

test("fixture discovery returns nested JSON paths in deterministic order", (context) => {
  const root = temporaryRepository(context);
  mkdirSync(join(root, "fixtures/nested"), { recursive: true });
  writeFileSync(join(root, "fixtures/z.json"), "{}\n");
  writeFileSync(join(root, "fixtures/nested/a.json"), "{}\n");
  writeFileSync(join(root, "fixtures/nested/ignored.txt"), "ignored\n");

  assert.deepEqual(discoverFixturePaths(root, ["fixtures"]), [
    "fixtures/nested/a.json",
    "fixtures/z.json"
  ]);
});

test("fixture discovery rejects symbolic links", (context) => {
  const root = temporaryRepository(context);
  mkdirSync(join(root, "fixtures"), { recursive: true });
  writeFileSync(join(root, "target.json"), "{}\n");
  symlinkSync(join(root, "target.json"), join(root, "fixtures/linked.json"));

  assert.throws(
    () => discoverFixturePaths(root, ["fixtures"]),
    /Fixture discovery does not allow symbolic links: fixtures\/linked\.json/
  );
});

test("fixture discovery rejects overlapping roots", (context) => {
  const root = temporaryRepository(context);
  mkdirSync(join(root, "fixtures/nested"), { recursive: true });
  writeFileSync(join(root, "fixtures/nested/value.json"), "{}\n");

  assert.throws(
    () => discoverFixturePaths(root, ["fixtures", "fixtures/nested"]),
    /Fixture discovered through multiple roots: fixtures\/nested\/value\.json/
  );
});

test("fixture discovery rejects roots outside the repository", (context) => {
  const root = temporaryRepository(context);
  const outsideRoot = temporaryRepository(context);
  mkdirSync(join(outsideRoot, "fixtures"), { recursive: true });

  assert.throws(
    () => discoverFixturePaths(root, [join(outsideRoot, "fixtures")]),
    /Fixture root resolves outside repository/
  );
});

test("fixture discovery reports missing roots", (context) => {
  const root = temporaryRepository(context);

  assert.throws(
    () => discoverFixturePaths(root, ["missing"]),
    /Fixture root does not exist: missing/
  );
});

test("fixture discovery rejects non-directory roots", (context) => {
  const root = temporaryRepository(context);
  writeFileSync(join(root, "fixture.json"), "{}\n");

  assert.throws(
    () => discoverFixturePaths(root, ["fixture.json"]),
    /Fixture root is not a directory: fixture\.json/
  );
});

function temporaryRepository(context) {
  const root = mkdtempSync(join(tmpdir(), "astrolabe-fixture-discovery-"));
  context.after(() => rmSync(root, { recursive: true, force: true }));
  return root;
}
