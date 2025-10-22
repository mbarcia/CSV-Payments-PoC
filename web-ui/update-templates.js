/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Script to update the browser bundle with templates from the Node.js generator
// This ensures the web UI uses the same templates as the Node.js CLI tool

import fs from 'fs';
import path from 'path';
import {fileURLToPath} from 'url';

// Define paths
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const nodeGeneratorPath = path.join(__dirname, '../template-generator-node');
const templatesDir = path.join(nodeGeneratorPath, './templates');
const webUiBundlePath = path.join(__dirname, './static/browser-bundle.js');

// Read all .hbs files from the Node.js generator templates directory
let templates = {};
try {
const templateFiles = fs.readdirSync(templatesDir);

// Create a JavaScript object with all templates
for (const file of templateFiles) {
    if (file.endsWith('.hbs')) {
        const templateName = path.basename(file, '.hbs');
        const templateContent = fs.readFileSync(path.join(templatesDir, file), 'utf8');
        templates[templateName] = templateContent;
    }
}
} catch (error) {
  console.error(`Error reading templates: ${error.message}`);
  process.exit(1);
}

// Read the existing browser bundle
let existingBundle;
try {
    existingBundle = fs.readFileSync(webUiBundlePath, 'utf8');
} catch (error) {
    console.error(`Error reading browser bundle file '${webUiBundlePath}': ${error.message}`);
    console.error('Full error:', error);
    process.exit(1);
}

// Generate the template section of the JavaScript code
const templatesJsContent = `// The templates are embedded as a JS object\nconst TEMPLATES = ${JSON.stringify(templates, null, 2)};`;

// Find the existing templates section and replace it
const templatesStart = existingBundle.indexOf('// The templates are embedded as a JS object');
const templatesEnd = existingBundle.indexOf('// Handlebars template engine for the browser');

if (templatesStart === -1 || templatesEnd === -1) {
    console.error('Could not find the templates section in the existing bundle');
    process.exit(1);
}

// Extract the parts before and after the templates section
const beforeTemplates = existingBundle.substring(0, templatesStart);
const afterTemplates = existingBundle.substring(templatesEnd);

// Combine the parts with the updated templates
const newBundleContent = `${beforeTemplates}${templatesJsContent}\n\n${afterTemplates}`;

// Write the updated bundle
try {
    fs.writeFileSync(webUiBundlePath, newBundleContent);
} catch (error) {
    console.error(`Error writing to browser bundle file '${webUiBundlePath}': ${error.message}`);
    console.error('Full error:', error);
    process.exit(1);
}

console.log('Templates have been updated in the browser bundle!');