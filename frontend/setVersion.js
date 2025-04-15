const fs = require('fs');
const path = require('path');

const newVersion = process.argv[2];
const packageJsonPath = path.resolve(__dirname, '../frontend/package.json');

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
packageJson.version = newVersion;

fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
console.log(`Version updated to ${newVersion}`);