import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

import { parseReleaseArgs, prepareRelease } from "../scripts/release-prepare.mjs";
import { versionedPaths } from "../scripts/versioning.mjs";

test("release preparation accepts one prerelease SemVer", () => {
  assert.deepEqual(parseReleaseArgs(["2.0.0-rc.2"]), { version: "2.0.0-rc.2" });
});

test("release preparation rejects missing or extra arguments", () => {
  assert.throws(() => parseReleaseArgs([]));
  assert.throws(() => parseReleaseArgs(["2.0.0", "extra"]));
});

test("release preparation synchronizes, verifies, commits, and tags a prerelease", () => {
  const projectRoot = mkdtempSync(join(tmpdir(), "astrolabe-protocol-release-"));
  const commands = [];
  try {
    writeFileSync(
      join(projectRoot, "package.json"),
      `${JSON.stringify({ version: "2.0.0-rc.1" }, null, 2)}\n`
    );
    writeFileSync(
      join(projectRoot, "package-lock.json"),
      `${JSON.stringify({ version: "2.0.0-rc.1", packages: { "": { version: "2.0.0-rc.1" } } }, null, 2)}\n`
    );
    writeFileSync(
      join(projectRoot, "README.md"),
      '.package(url: "example", exact: "2.0.0-rc.1")\n'
    );
    writeFileSync(join(projectRoot, "gradle.properties"), "astrolabeVersion=2.0.0-rc.1\n");
    const sourceDirectory = join(projectRoot, "Sources/AstrolabeProtocol/Core");
    mkdirSync(sourceDirectory, { recursive: true });
    writeFileSync(
      join(sourceDirectory, "RuntimeProtocolMetadata.swift"),
      'static let packageVersion = "2.0.0-rc.1"\n'
    );

    const result = prepareRelease({
      projectRoot,
      version: "2.0.0-rc.2",
      commandRunner: (command, args) => {
        commands.push([command, ...args]);
        const joined = [command, ...args].join(" ");
        if (joined === "git branch --show-current") return success("develop\n");
        if (joined === "git rev-parse HEAD") return success("abc123\n");
        if (joined.startsWith("git ls-remote --heads")) {
          return success("abc123\trefs/heads/develop\n");
        }
        if (joined === "git diff HEAD --name-only") {
          return success(`${versionedPaths.join("\n")}\n`);
        }
        return success();
      }
    });

    assert.equal(result.version, "2.0.0-rc.2");
    assert.equal(JSON.parse(readFileSync(join(projectRoot, "package.json"))).version, "2.0.0-rc.2");
    assert.match(
      readFileSync(join(projectRoot, "gradle.properties"), "utf8"),
      /^astrolabeVersion=2\.0\.0-rc\.2$/m
    );
    assert.ok(commands.some((command) => command.join(" ") === "swift build -c release"));
    assert.ok(
      commands.some(
        (command) => command.join(" ") === "./gradlew :AstrolabeProtocolKotlin:build"
      )
    );
    assert.ok(commands.some((command) => command.join(" ") === "git tag -a 2.0.0-rc.2 -m Astrolabe Protocol 2.0.0-rc.2"));
  } finally {
    rmSync(projectRoot, { recursive: true, force: true });
  }
});

function success(stdout = "") {
  return { status: 0, stdout, stderr: "" };
}
