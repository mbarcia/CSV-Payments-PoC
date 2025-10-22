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

const HandlebarsTemplateEngine = require('./handlebars-template-engine');
const fs = require('fs-extra');
const path = require('path');
const YAML = require('js-yaml');
const Ajv = require('ajv');
const schema = require('./pipeline-template-schema.json');

class PipelineGenerator {
    constructor() {
        this.engine = new HandlebarsTemplateEngine(path.join(__dirname, '../templates'));
    }

    /**
     * Generates a multi-module Java project from templates for the given application and pipeline steps.
     * 
     * @param {string} configPath Path to the YAML configuration file
     * @param {string} outputPath Path where the generated project will be written
     * @returns {Promise<void>}
     */
    async generateFromConfig(configPath, outputPath) {
        const config = this.loadConfig(configPath);
        const { appName, basePackage, steps } = config;
        await this.engine.generateApplication(appName, basePackage, steps, outputPath);
    }

    /**
     * Generates a sample configuration file
     * @param {string} outputPath Path where the sample config will be written
     * @returns {Promise<void>}
     */
    async generateSampleConfig(outputPath) {
        const config = {
            appName: 'Sample Pipeline App',
            basePackage: 'com.example.sample',
            steps: [
                {
                    name: 'Process Customer',
                    cardinality: 'ONE_TO_ONE',
                    inputTypeName: 'CustomerInput',
                    inputFields: [
                        { name: 'id', type: 'UUID', protoType: 'string' },
                        { name: 'name', type: 'String', protoType: 'string' },
                        { name: 'email', type: 'String', protoType: 'string' },
                        { name: 'createdAt', type: 'LocalDateTime', protoType: 'string' }
                    ],
                    outputTypeName: 'CustomerOutput',
                    outputFields: [
                        { name: 'id', type: 'UUID', protoType: 'string' },
                        { name: 'name', type: 'String', protoType: 'string' },
                        { name: 'status', type: 'String', protoType: 'string' },
                        { name: 'processedAt', type: 'String', protoType: 'string' }
                    ],
                    batchSize: 10,
                    batchTimeoutMs: 1000,
                    parallel: false
                },
                {
                    name: 'Validate Order',
                    cardinality: 'ONE_TO_ONE',
                    inputTypeName: 'OrderInput',
                    inputFields: [
                        { name: 'id', type: 'UUID', protoType: 'string' },
                        { name: 'customerId', type: 'UUID', protoType: 'string' },
                        { name: 'amount', type: 'Double', protoType: 'double' }
                    ],
                    outputTypeName: 'ValidationOutput',
                    outputFields: [
                        { name: 'id', type: 'UUID', protoType: 'string' },
                        { name: 'isValid', type: 'Boolean', protoType: 'bool' },
                        { name: 'message', type: 'String', protoType: 'string' }
                    ],
                    batchSize: 10,
                    batchTimeoutMs: 1000,
                    parallel: false
                }
            ]
        };

        await this.saveConfig(config, outputPath);
    }

    /**
     * Loads configuration from a YAML file
     * @param {string} configPath Path to the YAML configuration file
     * @returns {object} The parsed configuration object
     */
    loadConfig(configPath) {
        const yamlStr = fs.readFileSync(configPath, 'utf8');
        const config = YAML.load(yamlStr);
        
        // Validate the configuration against the schema
        const ajv = new Ajv();
        const validate = ajv.compile(schema);
        const valid = validate(config);
        
        if (!valid) {
            const errors = validate.errors;
            const errorMessages = errors.map(error => 
                `Property '${error.instancePath || 'root'}': ${error.message}`
            ).join('\n');
            throw new Error(`Configuration validation failed:\n${errorMessages}`);
        }
        
        // Process steps to add missing properties that are normally added by interactive mode
        config.steps = this.processSteps(config.steps);
        
        return config;
    }

    /**
     * Saves configuration to a YAML file
     * @param {object} config The configuration object to save
     * @param {string} outputPath Path where the config file will be written
     * @returns {Promise<void>}
     */
    async saveConfig(config, outputPath) {
        const yamlStr = YAML.dump(config, { lineWidth: -1 });
        await fs.writeFile(outputPath, yamlStr);
    }

    /**
     * Processes steps to add missing properties that are normally added by interactive mode
     * @param {Array} steps The array of step configurations
     * @returns {Array} Processed steps with additional properties
     */
    processSteps(steps) {
        return steps.map((step, i) => {
            const processedStep = { ...step };
            
            // Add missing properties if not already present
            if (!processedStep.serviceName) {
                processedStep.serviceName = step.name.replace(/[^a-zA-Z0-9]/g, '-').toLowerCase() + '-svc';
            }
            
            if (!processedStep.serviceNameCamel) {
                // Extract entity name from step name (e.g., "Process Customer" -> "Customer", "Validate Order" -> "Order")
                let entityName = step.name
                    .replace('Process ', '')
                    .replace('Validate ', '')
                    .replace('Enrich ', '')
                    .trim();
                entityName = entityName.replace(/[^a-zA-Z0-9]/g, ' ').trim();
                
                // Convert to camelCase
                const camelCaseName = this.toCamelCase(entityName);
                processedStep.serviceNameCamel = camelCaseName.charAt(0).toUpperCase() + camelCaseName.slice(1);
            }
            
            if (!processedStep.serviceNameTitleCase) {
                processedStep.serviceNameTitleCase = 
                    this.toTitleCase(processedStep.serviceName.replace(/-svc$/, '')) + 'Svc';
            }
            
            if (!processedStep.inputTypeSimpleName) {
                processedStep.inputTypeSimpleName = step.inputTypeName ? 
                    step.inputTypeName.replace(/.*\./, '') : '';
            }
            
            if (!processedStep.outputTypeSimpleName) {
                processedStep.outputTypeSimpleName = step.outputTypeName ?
                    step.outputTypeName.replace(/.*\./, '') : '';
            }
            
            processedStep.order = i + 1;
            
            if (!processedStep.grpcClientName) {
                processedStep.grpcClientName = step.name.replace(/[^a-zA-Z0-9]/g, '') + 'Svc';
            }
            
            // Determine stepType based on cardinality if not already present
            if (!processedStep.stepType) {
                processedStep.stepType = this.getStepTypeForCardinality(step.cardinality);
            }
            
            // Ensure optional parameters have default values as per schema
            if (processedStep.batchSize === undefined) {
                processedStep.batchSize = 10; // default from schema
            }
            
            if (processedStep.batchTimeoutMs === undefined) {
                processedStep.batchTimeoutMs = 1000; // default from schema
            }
            
            if (processedStep.parallel === undefined) {
                processedStep.parallel = false; // default from schema
            }
            
            return processedStep;
        });
    }

    /**
     * Maps cardinality to step type
     * @param {string} cardinality The step cardinality
     * @returns {string} The corresponding step type
     */
    getStepTypeForCardinality(cardinality) {
        switch (cardinality) {
            case 'ONE_TO_ONE':
                return 'StepOneToOne';
            case 'EXPANSION':
                return 'StepOneToMany';
            case 'REDUCTION':
                return 'StepManyToOne';
            case 'SIDE_EFFECT':
                return 'StepSideEffect';
            default:
                return 'StepOneToOne'; // default
        }
    }

    /**
     * Converts a string to camelCase
     * @param {string} input The input string
     * @returns {string} The camelCase version
     */
    toCamelCase(input) {
        const parts = input.trim().split(/\s+/);
        let result = '';
        
        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];
            if (part.length > 0) {
                if (i === 0) {
                    result += part.charAt(0).toLowerCase();
                } else {
                    result += part.charAt(0).toUpperCase();
                }
                result += part.slice(1).toLowerCase();
            }
        }
        
        return result;
    }

    /**
     * Converts a string to TitleCase
     * @param {string} input The input string
     * @returns {string} The TitleCase version
     */
    toTitleCase(input) {
        // Convert hyphens to spaces for proper title casing
        const normalizedInput = input.replace(/-/g, ' ');
        const parts = normalizedInput.trim().split(/\s+/);
        let result = '';
        
        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];
            if (part.length > 0) {
                result += part.charAt(0).toUpperCase();
                if (part.length > 1) {
                    result += part.slice(1).toLowerCase();
                }
            }
        }
        
        return result;
    }
}

module.exports = PipelineGenerator;