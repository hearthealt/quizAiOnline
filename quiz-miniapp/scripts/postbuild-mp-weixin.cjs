const fs = require("node:fs");
const path = require("node:path");

const projectRoot = path.resolve(__dirname, "..");
const outputRoot = path.join(projectRoot, "dist", "build", "mp-weixin");
const appJsonPath = path.join(outputRoot, "app.json");
const apiRoot = path.join(outputRoot, "api");

const apiTargets = {
  "exam.js": ["pages/bank", "pages/exam"],
  "favorite.js": ["pages/favorite", "pages/practice"],
  "practice.js": ["pages/bank", "pages/practice", "pages/wrong"],
  "record.js": ["pages/record"],
  "search.js": ["pages/search"],
  "vip.js": ["pages/vip"],
  "wrong.js": ["pages/wrong"]
};

const pageRewrites = {
  "pages/bank/detail.js": {
    "../../api/practice.js": "./api/practice.js",
    "../../api/exam.js": "./api/exam.js"
  },
  "pages/exam/index.js": {
    "../../api/exam.js": "./api/exam.js"
  },
  "pages/exam/list.js": {
    "../../api/exam.js": "./api/exam.js"
  },
  "pages/exam/result.js": {
    "../../api/exam.js": "./api/exam.js"
  },
  "pages/favorite/index.js": {
    "../../api/favorite.js": "./api/favorite.js"
  },
  "pages/practice/index.js": {
    "../../api/practice.js": "./api/practice.js",
    "../../api/favorite.js": "./api/favorite.js"
  },
  "pages/record/detail.js": {
    "../../api/record.js": "./api/record.js"
  },
  "pages/record/index.js": {
    "../../api/record.js": "./api/record.js"
  },
  "pages/search/index.js": {
    "../../api/search.js": "./api/search.js"
  },
  "pages/vip/orders.js": {
    "../../api/vip.js": "./api/vip.js"
  },
  "pages/wrong/index.js": {
    "../../api/practice.js": "./api/practice.js",
    "../../api/wrong.js": "./api/wrong.js"
  }
};

function assertFileExists(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Missing file: ${filePath}`);
  }
}

function readText(filePath) {
  assertFileExists(filePath);
  return fs.readFileSync(filePath, "utf8");
}

function writeText(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content, "utf8");
}

function rewriteApiModule(content) {
  return content.replaceAll('require("../', 'require("../../../');
}

function distributeSubpackageApis() {
  for (const [apiFile, targets] of Object.entries(apiTargets)) {
    const sourcePath = path.join(apiRoot, apiFile);
    if (!fs.existsSync(sourcePath)) {
      continue;
    }
    const sourceContent = rewriteApiModule(readText(sourcePath));

    for (const targetDir of targets) {
      const targetPath = path.join(outputRoot, targetDir, "api", apiFile);
      writeText(targetPath, sourceContent);
    }
  }
}

function rewritePageImports() {
  for (const [relativePagePath, replacements] of Object.entries(pageRewrites)) {
    const pagePath = path.join(outputRoot, relativePagePath);
    if (!fs.existsSync(pagePath)) {
      continue;
    }
    let content = readText(pagePath);

    for (const [from, to] of Object.entries(replacements)) {
      if (!content.includes(from)) {
        continue;
      }
      content = content.replaceAll(from, to);
    }

    writeText(pagePath, content);
  }
}

function removeRootApis() {
  for (const apiFile of Object.keys(apiTargets)) {
    const sourcePath = path.join(apiRoot, apiFile);
    if (fs.existsSync(sourcePath)) {
      fs.unlinkSync(sourcePath);
    }
  }
}

function patchAppJson() {
  const appJson = JSON.parse(readText(appJsonPath));
  appJson.lazyCodeLoading = "requiredComponents";
  writeText(appJsonPath, `${JSON.stringify(appJson, null, 2)}\n`);
}

function verifyOutput() {
  for (const apiFile of Object.keys(apiTargets)) {
    const removedPath = path.join(apiRoot, apiFile);
    if (fs.existsSync(removedPath)) {
      throw new Error(`Root api file still exists: ${removedPath}`);
    }
  }

  for (const [relativePagePath, replacements] of Object.entries(pageRewrites)) {
    const pagePath = path.join(outputRoot, relativePagePath);
    if (!fs.existsSync(pagePath)) {
      continue;
    }
    const content = readText(pagePath);
    for (const from of Object.keys(replacements)) {
      if (content.includes(from)) {
        throw new Error(`Page import rewrite failed: ${relativePagePath} still contains ${from}`);
      }
    }
  }
}

function main() {
  assertFileExists(appJsonPath);
  distributeSubpackageApis();
  rewritePageImports();
  removeRootApis();
  patchAppJson();
  verifyOutput();
  console.log("postbuild-mp-weixin: optimized subpackage-only APIs and enabled lazyCodeLoading.");
}

main();
