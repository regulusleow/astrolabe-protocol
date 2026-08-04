import { lstatSync, readdirSync } from "node:fs";
import { isAbsolute, join, relative, resolve, sep } from "node:path";

export function discoverFixturePaths(repositoryRoot, fixtureRoots) {
  const resolvedRepositoryRoot = resolve(repositoryRoot);
  const fixturePaths = [];
  const discoveredPaths = new Set();

  for (const fixtureRoot of fixtureRoots) {
    const resolvedFixtureRoot = resolve(resolvedRepositoryRoot, fixtureRoot);
    assertWithinRepository(resolvedRepositoryRoot, resolvedFixtureRoot, fixtureRoot);
    const rootStatus = fixtureRootStatus(resolvedFixtureRoot, fixtureRoot);
    if (rootStatus.isSymbolicLink()) {
      throw new Error(`Fixture discovery does not allow symbolic links: ${fixtureRoot}`);
    }
    if (!rootStatus.isDirectory()) {
      throw new Error(`Fixture root is not a directory: ${fixtureRoot}`);
    }
    visitDirectory(
      resolvedFixtureRoot,
      resolvedRepositoryRoot,
      fixturePaths,
      discoveredPaths
    );
  }

  return fixturePaths.sort();
}

function visitDirectory(directory, repositoryRoot, fixturePaths, discoveredPaths) {
  const entries = readdirSync(directory, { withFileTypes: true });
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(
        `Fixture discovery does not allow symbolic links: ${repositoryRelativePath(repositoryRoot, path)}`
      );
    }
    if (entry.isDirectory()) {
      visitDirectory(path, repositoryRoot, fixturePaths, discoveredPaths);
    } else if (entry.isFile() && entry.name.endsWith(".json")) {
      const fixturePath = repositoryRelativePath(repositoryRoot, path);
      if (discoveredPaths.has(fixturePath)) {
        throw new Error(`Fixture discovered through multiple roots: ${fixturePath}`);
      }
      discoveredPaths.add(fixturePath);
      fixturePaths.push(fixturePath);
    }
  }
}

function fixtureRootStatus(path, displayPath) {
  try {
    return lstatSync(path);
  } catch (error) {
    if (error?.code === "ENOENT") {
      throw new Error(`Fixture root does not exist: ${displayPath}`);
    }
    throw error;
  }
}

function assertWithinRepository(repositoryRoot, path, displayPath) {
  const repositoryRelative = relative(repositoryRoot, path);
  if (
    repositoryRelative === ".." ||
    repositoryRelative.startsWith(`..${sep}`) ||
    isAbsolute(repositoryRelative)
  ) {
    throw new Error(`Fixture root resolves outside repository: ${displayPath}`);
  }
}

function repositoryRelativePath(repositoryRoot, path) {
  return relative(repositoryRoot, path).split(sep).join("/");
}
