// setVersion.js
const fs = require('fs');
const path = require('path');

const versionFile = path.join(__dirname, 'buildVersion.txt');

let version = 1;
if (fs.existsSync(versionFile)) {
  version = parseInt(fs.readFileSync(versionFile).toString(), 10) + 1;
}
fs.writeFileSync(versionFile, version.toString());

console.log('빌드 버전:', version);
