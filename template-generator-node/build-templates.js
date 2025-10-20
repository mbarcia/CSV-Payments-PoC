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

// This script needs to package all templates into the JavaScript file
// It will transform the .hbs files into a JavaScript object

const fs = require('fs');
const path = require('path');

// Read all .hbs files from the templates directory
const templatesDir = path.join(__dirname, './templates');
const templateFiles = fs.readdirSync(templatesDir);

// Create a JavaScript object with all templates
const templates = {};
for (const file of templateFiles) {
    if (file.endsWith('.hbs')) {
        const templateName = path.basename(file, '.hbs');
        const templateContent = fs.readFileSync(path.join(templatesDir, file), 'utf8');
        templates[templateName] = templateContent;
    }
}

// Generate the JavaScript code as a string
const jsContent = `
// Generated template collection
const TEMPLATES = ${JSON.stringify(templates, null, 2)};
`;

// Write the templates file
const outputPath = path.join(__dirname, './dist/templates.js');
fs.writeFileSync(outputPath, jsContent);

console.log('Templates have been packaged into dist/templates.js');