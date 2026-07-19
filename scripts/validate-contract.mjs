import { readFileSync, readdirSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const schemaDirectory = join(repositoryRoot, "Schemas/v2");
const fixtureDirectory = join(repositoryRoot, "Fixtures/v2");
const manifest = readJSON(join(fixtureDirectory, "manifest.json"));
const ajv = new Ajv2020({ allErrors: true, strict: true, allowUnionTypes: true });
addFormats(ajv);

for (const fileName of readdirSync(schemaDirectory).filter((name) => name.endsWith(".json"))) {
  ajv.addSchema(readJSON(join(schemaDirectory, fileName)));
}

const validateManifest = assertSchemaExists(
  "https://astrolabe.dev/schemas/v2/conformance-manifest.schema.json"
);
if (!validateManifest(manifest)) {
  throw new Error(
    `Invalid V2 conformance manifest: ${ajv.errorsText(validateManifest.errors)}`
  );
}

assertUnique(manifest.methods.map((method) => method.name), "Method names");
assertUnique(manifest.cases.map((contractCase) => contractCase.name), "Fixture case names");
assertUnique(manifest.cases.map((contractCase) => contractCase.fixture), "Fixture paths");

for (const method of manifest.methods) {
  assertSchemaExists(method.requestSchema);
  assertSchemaExists(method.successResponseSchema);
  const validateFailure = assertSchemaExists(method.failureResponseSchema);
  const failureFixture = {
    requestID: "ffffffff-ffff-4fff-8fff-ffffffffffff",
    protocolVersion: { major: 2, minor: 0 },
    method: method.name,
    status: "failure",
    error: {
      code: "internalFailure",
      message: "Conformance failure response.",
      recoverySuggestion: null
    }
  };
  if (!validateFailure(failureFixture)) {
    throw new Error(`Failure response schema does not accept the standard error for method: ${method.name}`);
  }
}

const manifestedFixtures = new Set();
const validSchemaReferences = new Set();
for (const contractCase of manifest.cases) {
  const fixturePath = join(repositoryRoot, contractCase.fixture);
  const fixture = readJSON(fixturePath);
  const validate = assertSchemaExists(contractCase.schema);
  const schemaValid = validate(fixture);
  const semanticValid = contractCase.semanticRule
    ? validateSemanticRule(contractCase.semanticRule, fixture)
    : true;
  const valid = schemaValid && semanticValid;
  const expectedValid = contractCase.expectation === "valid";

  if (!["valid", "invalid"].includes(contractCase.expectation)) {
    throw new Error(`Invalid fixture expectation: ${contractCase.name}`);
  }
  if (contractCase.expectation === "invalid" && !contractCase.reason) {
    throw new Error(`Invalid fixture is missing a rejection reason: ${contractCase.name}`);
  }
  if (contractCase.semanticRule && !schemaValid) {
    throw new Error(`Semantic fixture was rejected by its schema before semantic validation: ${contractCase.name}`);
  }
  if (expectedValid) {
    validSchemaReferences.add(contractCase.schema);
  }
  manifestedFixtures.add(contractCase.fixture);
  if (valid !== expectedValid) {
    const schemaErrors = ajv.errorsText(validate.errors, { separator: "; " });
    throw new Error(
      `Contract fixture result does not match the manifest: ${contractCase.name}; ` +
      `expected ${contractCase.expectation}; schema errors: ${schemaErrors}`
    );
  }
}

for (const method of manifest.methods) {
  for (const reference of [method.requestSchema, method.successResponseSchema]) {
    if (!validSchemaReferences.has(reference)) {
      throw new Error(`Method is missing a valid fixture: ${method.name}, ${reference}`);
    }
  }
}

const fixtureFiles = ["valid", "invalid"].flatMap((directory) =>
  readdirSync(join(fixtureDirectory, directory))
    .filter((name) => name.endsWith(".json"))
    .map((name) => relative(repositoryRoot, join(fixtureDirectory, directory, name)))
);
const unmanifestedFixtures = fixtureFiles.filter((path) => !manifestedFixtures.has(path));
if (unmanifestedFixtures.length > 0) {
  throw new Error(`Unregistered V2 fixtures found: ${unmanifestedFixtures.join(", ")}`);
}

function assertSchemaExists(reference) {
  const validate = ajv.getSchema(reference);
  if (!validate) {
    throw new Error(`Schema not found: ${reference}`);
  }
  return validate;
}

function assertUnique(values, label) {
  if (new Set(values).size !== values.length) {
    throw new Error(`${label} contain duplicate values`);
  }
}

function validateSemanticRule(rule, fixture) {
  switch (rule) {
  case "ascendingProtocolRange": {
    const range = fixture.parameters?.supportedProtocolRange;
    const minimum = range?.minimum;
    const maximum = range?.maximum;
    if (!minimum || !maximum || minimum.major !== maximum.major) {
      return false;
    }
    return minimum.major < maximum.major ||
      (minimum.major === maximum.major && minimum.minor <= maximum.minor);
  }
  case "derivedVisibility": {
    const visible = !fixture.hidden &&
      !fixture.hiddenByAncestor &&
      fixture.effectiveOpacity > 0.01 &&
      fixture.intersectsViewport &&
      !fixture.fullyClippedByAncestor;
    return fixture.onscreen === visible;
  }
  default:
    throw new Error(`Unsupported semantic validation rule: ${rule}`);
  }
}

function readJSON(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}
