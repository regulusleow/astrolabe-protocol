import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const metadataPath = "Sources/AstrolabeProtocol/Core/RuntimeProtocolMetadata.swift";
const gradlePropertiesPath = "gradle.properties";

export const versionedPaths = Object.freeze([
  gradlePropertiesPath,
  metadataPath,
  "README.md",
  "README.zh-CN.md",
  "package-lock.json",
  "package.json"
]);

const prereleaseIdentifier = "(?:0|[1-9]\\d*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)";
const releaseVersionPattern = new RegExp(
  `^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-(${prereleaseIdentifier}(?:\\.${prereleaseIdentifier})*))?$`
);
const swiftVersionPattern = /static let packageVersion = "([^"]+)"/g;
const documentationVersionPattern = /exact: "([^"]+)"/g;
const gradleVersionPattern = /^astrolabeVersion=(.+)$/gm;

export function assertReleaseVersion(version) {
  if (!releaseVersionPattern.test(version)) {
    throw new Error(`Invalid version format: ${version}; expected SemVer 2.0.0 format`);
  }
  return version;
}

export function compareReleaseVersions(left, right) {
  const leftVersion = parseVersion(left);
  const rightVersion = parseVersion(right);
  for (const key of ["major", "minor", "patch"]) {
    if (leftVersion[key] !== rightVersion[key]) {
      return leftVersion[key] > rightVersion[key] ? 1 : -1;
    }
  }
  return comparePrerelease(leftVersion.prerelease, rightVersion.prerelease);
}

export function canonicalRepositoryVersion(projectRoot) {
  return assertReleaseVersion(readJSON(join(projectRoot, "package.json")).version);
}

export function synchronizeRepositoryVersion(projectRoot, version) {
  assertReleaseVersion(version);
  const updates = [
    packageVersionUpdate(join(projectRoot, "package.json"), version),
    packageLockVersionUpdate(join(projectRoot, "package-lock.json"), version),
    textVersionUpdate(
      join(projectRoot, metadataPath),
      swiftVersionPattern,
      `static let packageVersion = "${version}"`
    ),
    textVersionUpdate(
      join(projectRoot, "README.md"),
      documentationVersionPattern,
      `exact: "${version}"`
    ),
    textVersionUpdate(
      join(projectRoot, "README.zh-CN.md"),
      documentationVersionPattern,
      `exact: "${version}"`
    ),
    textVersionUpdate(
      join(projectRoot, gradlePropertiesPath),
      gradleVersionPattern,
      `astrolabeVersion=${version}`
    )
  ];
  updates.forEach(({ path, content }) => writeFileSync(path, content));

  const issues = versionConsistencyIssues(projectRoot);
  if (issues.length > 0) {
    throw new Error(`Version mismatch remains after synchronization:\n${issues.join("\n")}`);
  }
  return [...versionedPaths];
}

export function versionConsistencyIssues(projectRoot) {
  const expectedVersion = canonicalRepositoryVersion(projectRoot);
  const issues = [];
  inspectJSONVersion(
    join(projectRoot, "package-lock.json"),
    "package-lock.json",
    expectedVersion,
    issues
  );
  inspectTextVersion(
    join(projectRoot, metadataPath),
    metadataPath,
    swiftVersionPattern,
    expectedVersion,
    issues
  );
  inspectTextVersion(
    join(projectRoot, "README.md"),
    "README.md",
    documentationVersionPattern,
    expectedVersion,
    issues
  );
  inspectTextVersion(
    join(projectRoot, "README.zh-CN.md"),
    "README.zh-CN.md",
    documentationVersionPattern,
    expectedVersion,
    issues
  );
  inspectTextVersion(
    join(projectRoot, gradlePropertiesPath),
    gradlePropertiesPath,
    gradleVersionPattern,
    expectedVersion,
    issues
  );
  return issues;
}

function parseVersion(version) {
  const match = assertReleaseVersion(version).match(releaseVersionPattern);
  if (!match) {
    throw new Error(`Unable to parse version: ${version}`);
  }
  return {
    major: BigInt(match[1]),
    minor: BigInt(match[2]),
    patch: BigInt(match[3]),
    prerelease: match[4]?.split(".") ?? []
  };
}

function comparePrerelease(left, right) {
  if (left.length === 0 || right.length === 0) {
    return left.length === right.length ? 0 : left.length === 0 ? 1 : -1;
  }
  const length = Math.max(left.length, right.length);
  for (let index = 0; index < length; index += 1) {
    const leftValue = left[index];
    const rightValue = right[index];
    if (leftValue === undefined || rightValue === undefined) {
      return leftValue === rightValue ? 0 : leftValue === undefined ? -1 : 1;
    }
    if (leftValue === rightValue) continue;
    const leftNumeric = /^\d+$/.test(leftValue);
    const rightNumeric = /^\d+$/.test(rightValue);
    if (leftNumeric && rightNumeric) {
      return BigInt(leftValue) > BigInt(rightValue) ? 1 : -1;
    }
    if (leftNumeric !== rightNumeric) return leftNumeric ? -1 : 1;
    return leftValue > rightValue ? 1 : -1;
  }
  return 0;
}

function packageVersionUpdate(path, version) {
  const value = readJSON(path);
  value.version = version;
  return { path, content: jsonString(value) };
}

function packageLockVersionUpdate(path, version) {
  const value = readJSON(path);
  if (!value.packages?.[""]) {
    throw new Error(`Missing root package metadata in npm lockfile: ${path}`);
  }
  value.version = version;
  value.packages[""].version = version;
  return { path, content: jsonString(value) };
}

function textVersionUpdate(path, pattern, replacement) {
  const source = readFileSync(path, "utf8");
  const matches = [...source.matchAll(pattern)];
  if (matches.length !== 1) {
    throw new Error(`Unexpected version field count in ${path}: expected 1, found ${matches.length}`);
  }
  return { path, content: source.replace(pattern, replacement) };
}

function inspectJSONVersion(path, displayPath, expectedVersion, issues) {
  const value = readJSON(path);
  appendVersionIssue(displayPath, value.version, expectedVersion, issues);
  appendVersionIssue(
    `${displayPath}#packages[\"\"]`,
    value.packages?.[""]?.version,
    expectedVersion,
    issues
  );
}

function inspectTextVersion(path, displayPath, pattern, expectedVersion, issues) {
  const matches = [...readFileSync(path, "utf8").matchAll(pattern)];
  if (matches.length !== 1) {
    issues.push(`${displayPath}: expected one version field, found ${matches.length}`);
    return;
  }
  appendVersionIssue(displayPath, matches[0][1], expectedVersion, issues);
}

function appendVersionIssue(path, actualVersion, expectedVersion, issues) {
  if (actualVersion !== expectedVersion) {
    issues.push(`${path}: expected ${expectedVersion}, found ${actualVersion ?? "missing"}`);
  }
}

function readJSON(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function jsonString(value) {
  return `${JSON.stringify(value, null, 2)}\n`;
}
