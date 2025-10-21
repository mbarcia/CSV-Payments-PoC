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

const fs = require('fs-extra');
const path = require('path');
const Handlebars = require('handlebars');

// Register helper for replacing characters in strings
Handlebars.registerHelper('replace', function(str, find, replace) {
    if (typeof str !== 'string') return str;
    return str.replace(new RegExp(find, 'g'), replace);
});

// Register helper for converting to lowercase
Handlebars.registerHelper('lowercase', function(str) {
    return typeof str === 'string' ? str.toLowerCase() : str;
});

// Register helper for getting the index of an element in an array
Handlebars.registerHelper('indexOf', function(array, value) {
    return Array.isArray(array) ? array.indexOf(value) : -1;
});

// Register helper for checking if a step is the first one
Handlebars.registerHelper('isFirstStep', function(index) {
    return index === 0;
});

// Register helper for checking cardinality types
Handlebars.registerHelper('isExpansion', function(cardinality) {
    return cardinality === 'EXPANSION';
});

Handlebars.registerHelper('isReduction', function(cardinality) {
    return cardinality === 'REDUCTION';
});

Handlebars.registerHelper('isSideEffect', function(cardinality) {
    return cardinality === 'SIDE_EFFECT';
});

// Register helper for checking if a type is a list
Handlebars.registerHelper('isListType', function(type) {
    return type && type.startsWith('List<');
});

// Register helper for extracting list inner type
Handlebars.registerHelper('listInnerType', function(type) {
    if (!type || !type.startsWith('List<') || !type.endsWith('>')) {
        return type;
    }
    return type.substring(5, type.length - 1).toLowerCase();
});

// Register helper for checking if a type is a map
Handlebars.registerHelper('isMapType', function(type) {
    return type && type.startsWith('Map<') && type.includes(', ');
});

// Register helper for extracting map key and value types
// Register helper for extracting map key and value types
Handlebars.registerHelper('mapKeyType', function(type) {
    if (!type || !type.startsWith('Map<') || !type.includes(', ') || !type.endsWith('>')) {
        return 'string';
    }
    const parts = type.substring(4, type.length - 1).split(', ');
    let keyType = parts[0] || 'string';
    // Convert Java types to protobuf types
    switch(keyType) {
        case 'String':
            return 'string';
        case 'Integer':
            return 'int32';
        case 'Long':
            return 'int64';
        case 'Double':
            return 'double';
        case 'Boolean':
            return 'bool';
        case 'UUID':
            return 'string';
        case 'BigDecimal':
            return 'string';
        case 'Currency':
            return 'string';
        case 'Path':
            return 'string';
        case 'LocalDateTime':
            return 'string';
        case 'LocalDate':
            return 'string';
        case 'OffsetDateTime':
            return 'string';
        case 'ZonedDateTime':
            return 'string';
        case 'Instant':
            return 'string';
        case 'Duration':
            return 'string';
        case 'Period':
            return 'string';
        case 'URI':
            return 'string';
        case 'URL':
            return 'string';
        case 'File':
            return 'string';
        case 'BigInteger':
            return 'string';
        case 'AtomicInteger':
            return 'int32';
        case 'AtomicLong':
            return 'int64';
        case 'List<String>':
            return 'string';
        default:
            return keyType.toLowerCase();
    }
});

// Register helper for extracting map value type
Handlebars.registerHelper('mapValueType', function(type) {
    if (!type || !type.startsWith('Map<') || !type.includes(', ') || !type.endsWith('>')) {
        return 'string';
    }
    const parts = type.substring(4, type.length - 1).split(', ');
    let valueType = parts[1] || 'string';
    // Convert Java types to protobuf types
    switch(valueType) {
        case 'String':
            return 'string';
        case 'Integer':
            return 'int32';
        case 'Long':
            return 'int64';
        case 'Double':
            return 'double';
        case 'Boolean':
            return 'bool';
        case 'UUID':
            return 'string';
        case 'BigDecimal':
            return 'string';
        case 'Currency':
            return 'string';
        case 'Path':
            return 'string';
        case 'LocalDateTime':
            return 'string';
        case 'LocalDate':
            return 'string';
        case 'OffsetDateTime':
            return 'string';
        case 'ZonedDateTime':
            return 'string';
        case 'Instant':
            return 'string';
        case 'Duration':
            return 'string';
        case 'Period':
            return 'string';
        case 'URI':
            return 'string';
        case 'URL':
            return 'string';
        case 'File':
            return 'string';
        case 'BigInteger':
            return 'string';
        case 'AtomicInteger':
            return 'int32';
        case 'AtomicLong':
            return 'int64';
        case 'List<String>':
            return 'string';
        default:
            return valueType.toLowerCase();
    }
});

// Register helper for adding import flags
Handlebars.registerHelper('hasDateFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => 
        ['LocalDate', 'LocalDateTime', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period'].includes(field.type)
    );
});

Handlebars.registerHelper('hasBigIntegerFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'BigInteger');
});

Handlebars.registerHelper('hasBigDecimalFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'BigDecimal');
});

Handlebars.registerHelper('hasCurrencyFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'Currency');
});

Handlebars.registerHelper('hasPathFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'Path');
});

Handlebars.registerHelper('hasNetFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => ['URI', 'URL'].includes(field.type));
});

Handlebars.registerHelper('hasIoFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'File');
});

Handlebars.registerHelper('hasAtomicFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => ['AtomicInteger', 'AtomicLong'].includes(field.type));
});

Handlebars.registerHelper('hasUtilFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type === 'List<String>');
});

// Register helper to check if any field is a map type
Handlebars.registerHelper('hasMapFields', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.type.startsWith('Map<'));
});

Handlebars.registerHelper('hasIdField', function(fields) {
    if (!Array.isArray(fields)) return false;
    return fields.some(field => field.name === 'id');
});

// Register helper to convert base package to path format
Handlebars.registerHelper('toPath', function(basePackage) {
    return basePackage.replace(/\./g, '/');
});

// Register helper to format service name for proto classes
Handlebars.registerHelper('formatForProtoClassName', function(serviceName) {
    // Convert service names like "process-customer-svc" to "ProcessCustomerSvc"
    if (!serviceName) return '';
    const parts = serviceName.split('-');
    return parts.map(part => {
        if (!part) return '';
        return part.charAt(0).toUpperCase() + part.slice(1).toLowerCase();
    }).join('');
});

// Register helper to check if a field is an ID field
Handlebars.registerHelper('isIdField', function(fieldName) {
    return fieldName === 'id';
});

// Register helper for sanitizing Java identifiers
Handlebars.registerHelper('sanitizeJavaIdentifier', function(fieldName) {
    if (typeof fieldName !== 'string') return fieldName;
    
    // Reserved words in Java that need to be escaped
    const reservedWords = [
        'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
        'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
        'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
        'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
        'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
        'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null'
    ];
    
    // Check if it's a reserved word
    if (reservedWords.includes(fieldName.toLowerCase())) {
        return fieldName + '_';  // Append underscore to reserved words
    }
    
    // Replace invalid characters with underscore
    let sanitized = fieldName.replace(/[^a-zA-Z0-9_$]/g, '_');
    
    // Ensure it doesn't start with a number
    if (sanitized.length > 0 && /\d/.test(sanitized[0])) {
        sanitized = '_' + sanitized;
    }
    
    // If it became empty (which shouldn't happen with real input), return a default name
    if (sanitized === '') {
        sanitized = 'field';
    }
    
    return sanitized;
});

// Register helper for unless functionality
Handlebars.registerHelper('unless', function(condition, options) {
    if (!condition) {
        return options.fn(this);
    } else {
        return options.inverse(this);
    }
});

class HandlebarsTemplateEngine {
    constructor(templatesPath = './templates') {
        this.templatesPath = templatesPath;
        this.compiledTemplates = new Map();
        this.loadTemplates();
    }

    loadTemplates() {
        const templateFiles = fs.readdirSync(this.templatesPath);
        
        for (const file of templateFiles) {
            if (file.endsWith('.hbs') || file.endsWith('.handlebars')) {
                const templatePath = path.join(this.templatesPath, file);
                const templateName = path.basename(file, path.extname(file));
                const templateContent = fs.readFileSync(templatePath, 'utf8');
                this.compiledTemplates.set(templateName, Handlebars.compile(templateContent));
            }
        }
    }

    render(templateName, context) {
        const template = this.compiledTemplates.get(templateName);
        if (!template) {
            throw new Error(`Template ${templateName} not found`);
        }
        return template(context);
    }

    async generateApplication(appName, basePackage, steps, outputPath) {
        // For sequential pipeline, update input types of steps after the first one
        // to match the output type of the previous step
        for (let i = 1; i < steps.length; i++) {
            const currentStep = steps[i];
            const previousStep = steps[i - 1];
            // Set the input type of the current step to the output type of the previous step
            currentStep.inputTypeName = previousStep.outputTypeName;
            currentStep.inputFields = previousStep.outputFields; // Copy input fields from previous step's outputs
        }

        // Create output directory
        await fs.ensureDir(outputPath);

        // Generate parent POM
        await this.generateParentPom(appName, basePackage, steps, outputPath);

        // Generate common module
        await this.generateCommonModule(appName, basePackage, steps, outputPath);

        // Generate each step service
        for (let i = 0; i < steps.length; i++) {
            await this.generateStepService(appName, basePackage, steps[i], outputPath, i, steps);
        }

        // Generate orchestrator
        await this.generateOrchestrator(appName, basePackage, steps, outputPath);

        // Generate docker-compose
        await this.generateDockerCompose(appName, steps, outputPath);

        // Generate utility scripts
        await this.generateUtilityScripts(outputPath);

        // Generate observability configs
        await this.generateObservabilityConfigs(outputPath);

        // Generate mvnw files
        await this.generateMvNWFiles(outputPath);

        // Generate Maven wrapper files
        await this.generateMavenWrapperFiles(outputPath);

        // Generate other files
        await this.generateOtherFiles(appName, outputPath);
    }

    async generateParentPom(appName, basePackage, steps, outputPath) {
        const context = {
            basePackage,
            artifactId: appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-'),
            name: appName,
            steps
        };

        const rendered = this.render('parent-pom', context);
        const pomPath = path.join(outputPath, 'pom.xml');
        await fs.writeFile(pomPath, rendered);
    }

    async generateCommonModule(appName, basePackage, steps, outputPath) {
        const commonPath = path.join(outputPath, 'common');
        await fs.ensureDir(path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.domain')));
        await fs.ensureDir(path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.dto')));
        await fs.ensureDir(path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.mapper')));
        await fs.ensureDir(path.join(commonPath, 'src/main/proto'));

        // Generate common POM
        await this.generateCommonPom(appName, basePackage, commonPath);

        // Generate proto files for each step
        for (let i = 0; i < steps.length; i++) {
            await this.generateProtoFile(steps[i], basePackage, commonPath, i, steps);
        }

        // Generate entities, DTOs, and mappers for each step
        for (let i = 0; i < steps.length; i++) {
            const step = steps[i];
            await this.generateDomainClasses(step, basePackage, commonPath, i);
            await this.generateDtoClasses(step, basePackage, commonPath, i);
            await this.generateMapperClasses(step, basePackage, commonPath, i);
        }

        // Generate base entity
        await this.generateBaseEntity(basePackage, commonPath);

        // Generate common converters
        await this.generateCommonConverters(basePackage, commonPath);
    }

    async generateCommonPom(appName, basePackage, commonPath) {
        const context = {
            basePackage,
            rootProjectName: appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-')
        };

        const rendered = this.render('common-pom', context);
        const pomPath = path.join(commonPath, 'pom.xml');
        await fs.writeFile(pomPath, rendered);
    }

    async generateProtoFile(step, basePackage, commonPath, stepIndex, allSteps) {
        // Process input fields to add field numbers
        if (step.inputFields && Array.isArray(step.inputFields)) {
            for (let i = 0; i < step.inputFields.length; i++) {
                step.inputFields[i].fieldNumber = (i + 1).toString();
            }
        }

        // Process output fields to add field numbers starting after input fields
        if (step.outputFields && Array.isArray(step.outputFields)) {
            const outputStartNumber = (step.inputFields ? step.inputFields.length : 0) + 1;
            for (let i = 0; i < step.outputFields.length; i++) {
                step.outputFields[i].fieldNumber = (outputStartNumber + i).toString();
            }
        }

        const context = {
            ...step,
            basePackage,
            isExpansion: step.cardinality === 'EXPANSION',
            isReduction: step.cardinality === 'REDUCTION',
            isFirstStep: stepIndex === 0,
            ...(stepIndex > 0 && {
                previousStepName: allSteps[stepIndex - 1].serviceName,
                previousStepOutputTypeName: allSteps[stepIndex - 1].outputTypeName
            }),
            // Format the service name properly for the proto file
            serviceNameFormatted: this.formatForClassName(
                step.name.replace('Process ', '').trim()
            )
        };

        const rendered = this.render('proto', context);
        const protoPath = path.join(commonPath, 'src/main/proto', step.serviceName + '.proto');
        await fs.writeFile(protoPath, rendered);
    }

    async generateDomainClasses(step, basePackage, commonPath, stepIndex) {
        // Process input domain class only for first step
        if (stepIndex === 0 && step.inputFields && step.inputTypeName) {
            const inputContext = {
                ...step,
                basePackage,
                className: step.inputTypeName,
                fields: step.inputFields,
                hasDateFields: this.hasImportFlag(step.inputFields, ['LocalDate', 'LocalDateTime', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period']),
                hasBigIntegerFields: this.hasImportFlag(step.inputFields, ['BigInteger']),
                hasBigDecimalFields: this.hasImportFlag(step.inputFields, ['BigDecimal']),
                hasCurrencyFields: this.hasImportFlag(step.inputFields, ['Currency']),
                hasPathFields: this.hasImportFlag(step.inputFields, ['Path']),
                hasNetFields: this.hasImportFlag(step.inputFields, ['URI', 'URL']),
                hasIoFields: this.hasImportFlag(step.inputFields, ['File']),
                hasAtomicFields: this.hasImportFlag(step.inputFields, ['AtomicInteger', 'AtomicLong']),
                hasUtilFields: this.hasImportFlag(step.inputFields, ['List<String>']),
                hasMapFields: step.inputFields.some(field => field.type.startsWith('Map<')),
                hasIdField: step.inputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('domain', inputContext);
            const inputDomainPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.domain'), step.inputTypeName + '.java');
            await fs.writeFile(inputDomainPath, rendered);
        }

        // Process output domain class for all steps
        if (step.outputFields && step.outputTypeName) {
            const outputContext = {
                ...step,
                basePackage,
                className: step.outputTypeName,
                fields: step.outputFields,
                hasDateFields: this.hasImportFlag(step.outputFields, ['LocalDate', 'LocalDateTime', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period']),
                hasBigIntegerFields: this.hasImportFlag(step.outputFields, ['BigInteger']),
                hasBigDecimalFields: this.hasImportFlag(step.outputFields, ['BigDecimal']),
                hasCurrencyFields: this.hasImportFlag(step.outputFields, ['Currency']),
                hasPathFields: this.hasImportFlag(step.outputFields, ['Path']),
                hasNetFields: this.hasImportFlag(step.outputFields, ['URI', 'URL']),
                hasIoFields: this.hasImportFlag(step.outputFields, ['File']),
                hasAtomicFields: this.hasImportFlag(step.outputFields, ['AtomicInteger', 'AtomicLong']),
                hasUtilFields: this.hasImportFlag(step.outputFields, ['List<String>']),
                hasMapFields: step.outputFields.some(field => field.type.startsWith('Map<')),
                hasIdField: step.outputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('domain', outputContext);
            const outputDomainPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.domain'), step.outputTypeName + '.java');
            await fs.writeFile(outputDomainPath, rendered);
        }
    }

    async generateBaseEntity(basePackage, commonPath) {
        const context = { basePackage };
        const rendered = this.render('base-entity', context);
        const baseEntityPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.domain'), 'BaseEntity.java');
        await fs.writeFile(baseEntityPath, rendered);
    }

    async generateDtoClasses(step, basePackage, commonPath, stepIndex) {
        // Process input DTO class only for first step
        if (stepIndex === 0 && step.inputFields && step.inputTypeName) {
            const inputContext = {
                ...step,
                basePackage,
                className: step.inputTypeName + 'Dto',
                fields: step.inputFields,
                hasDateFields: this.hasImportFlag(step.inputFields, ['LocalDate', 'LocalDateTime', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period']),
                hasBigIntegerFields: this.hasImportFlag(step.inputFields, ['BigInteger']),
                hasBigDecimalFields: this.hasImportFlag(step.inputFields, ['BigDecimal']),
                hasCurrencyFields: this.hasImportFlag(step.inputFields, ['Currency']),
                hasPathFields: this.hasImportFlag(step.inputFields, ['Path']),
                hasNetFields: this.hasImportFlag(step.inputFields, ['URI', 'URL']),
                hasIoFields: this.hasImportFlag(step.inputFields, ['File']),
                hasAtomicFields: this.hasImportFlag(step.inputFields, ['AtomicInteger', 'AtomicLong']),
                hasUtilFields: this.hasImportFlag(step.inputFields, ['List<String>']),
                hasMapFields: step.inputFields.some(field => field.type.startsWith('Map<')),
                hasIdField: step.inputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('dto', inputContext);
            const inputDtoPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.dto'), step.inputTypeName + 'Dto.java');
            await fs.writeFile(inputDtoPath, rendered);
        }

        // Process output DTO class for all steps
        if (step.outputFields && step.outputTypeName) {
            const outputContext = {
                ...step,
                basePackage,
                className: step.outputTypeName + 'Dto',
                fields: step.outputFields,
                hasDateFields: this.hasImportFlag(step.outputFields, ['LocalDate', 'LocalDateTime', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period']),
                hasBigIntegerFields: this.hasImportFlag(step.outputFields, ['BigInteger']),
                hasBigDecimalFields: this.hasImportFlag(step.outputFields, ['BigDecimal']),
                hasCurrencyFields: this.hasImportFlag(step.outputFields, ['Currency']),
                hasPathFields: this.hasImportFlag(step.outputFields, ['Path']),
                hasNetFields: this.hasImportFlag(step.outputFields, ['URI', 'URL']),
                hasIoFields: this.hasImportFlag(step.outputFields, ['File']),
                hasAtomicFields: this.hasImportFlag(step.outputFields, ['AtomicInteger', 'AtomicLong']),
                hasUtilFields: this.hasImportFlag(step.outputFields, ['List<String>']),
                hasMapFields: step.outputFields.some(field => field.type.startsWith('Map<')),
                hasIdField: step.outputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('dto', outputContext);
            const outputDtoPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.dto'), step.outputTypeName + 'Dto.java');
            await fs.writeFile(outputDtoPath, rendered);
        }
    }

    async generateMapperClasses(step, basePackage, commonPath, stepIndex) {
        // Generate input mapper class only for first step (since other steps reference previous step's output)
        if (stepIndex === 0 && step.inputTypeName) {
            await this.generateMapperClass(step.inputTypeName, step, basePackage, commonPath);
        }

        // Generate output mapper class for all steps
        if (step.outputTypeName) {
            await this.generateMapperClass(step.outputTypeName, step, basePackage, commonPath);
        }
    }

    async generateMapperClass(className, step, basePackage, commonPath) {
        const context = {
            ...step,
            basePackage,
            className,
            domainClass: className.replace('Dto', ''),
            dtoClass: className + 'Dto',
            grpcClass: basePackage + '.grpc.' + this.formatForProtoClassName(step.serviceName)
        };

        const rendered = this.render('mapper', context);
        const mapperPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.mapper'), className + 'Mapper.java');
        await fs.writeFile(mapperPath, rendered);
    }

    async generateCommonConverters(basePackage, commonPath) {
        const context = { basePackage };
        const rendered = this.render('common-converters', context);
        const convertersPath = path.join(commonPath, 'src/main/java', this.toPath(basePackage + '.common.mapper'), 'CommonConverters.java');
        await fs.writeFile(convertersPath, rendered);
    }

    async generateStepService(appName, basePackage, step, outputPath, stepIndex, allSteps) {
        const stepPath = path.join(outputPath, step.serviceName);
        // Convert hyphens to underscores for valid Java package names
        const serviceNameForPackage = step.serviceName.replace('-svc', '').replace(/-/g, '_');
        await fs.ensureDir(path.join(stepPath, 'src/main/java', this.toPath(basePackage + '.' + serviceNameForPackage + '.service')));

        // Add rootProjectName to step map
        step.rootProjectName = appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-');

        // Generate step POM
        await this.generateStepPom(step, basePackage, stepPath);

        // Generate the service class
        await this.generateStepServiceClass(appName, basePackage, step, stepPath, stepIndex, allSteps);

        // Generate Dockerfile
        await this.generateDockerfile(step.serviceName, stepPath);
    }

    async generateStepPom(step, basePackage, stepPath) {
        const context = { ...step, basePackage };
        const rendered = this.render('step-pom', context);
        const pomPath = path.join(stepPath, 'pom.xml');
        await fs.writeFile(pomPath, rendered);
    }

    async generateStepServiceClass(appName, basePackage, step, stepPath, stepIndex, allSteps) {
        const context = { ...step };
        context.basePackage = basePackage;
        context.serviceName = step.serviceName.replace('-svc', '');
        // Convert hyphens to underscores for valid Java package names
        context.serviceNameForPackage = step.serviceName.replace('-svc', '').replace(/-/g, '_');

        // Format service name for proto-generated class names
        const protoClassName = this.formatForProtoClassName(step.serviceName);
        context.protoClassName = protoClassName;

        // Determine the inputGrpcType proto class name based on step position
        if (stepIndex === 0) {
            // For the first step, inputGrpcType comes from the same proto file
            context.inputGrpcProtoClassName = protoClassName;
        } else {
            // For subsequent steps, inputGrpcType comes from the previous step's proto file
            const previousStep = allSteps[stepIndex - 1];
            const previousProtoClassName = this.formatForProtoClassName(previousStep.serviceName);
            context.inputGrpcProtoClassName = previousProtoClassName;
        }

        // Use the serviceNameCamel field from the configuration to form the gRPC class names
        const serviceNameCamel = step.serviceNameCamel;
        // Convert camelCase to PascalCase
        const serviceNamePascal = serviceNameCamel.charAt(0).toUpperCase() + serviceNameCamel.slice(1);

        // Extract the entity name from the PascalCase service name to match proto service names
        const entityName = this.extractEntityName(serviceNamePascal);

        // For gRPC class names
        const grpcServiceName = 'MutinyProcess' + entityName + 'ServiceGrpc';
        const grpcStubName = grpcServiceName + '.MutinyProcess' + entityName + 'ServiceStub';
        const grpcImplName = grpcServiceName + '.Process' + entityName + 'ServiceImplBase';

        context.grpcServiceName = grpcServiceName;
        context.grpcStubName = grpcStubName;
        context.grpcImplName = grpcImplName;
        context.serviceNamePascal = serviceNamePascal;
        context.serviceNameFormatted = step.name;

        let reactiveServiceInterface = 'ReactiveService';
        let grpcAdapter = 'GrpcReactiveServiceAdapter';
        let processMethodReturnType = `Uni<${step.outputTypeName}>`;
        let processMethodParamType = step.inputTypeName;
        let returnStatement = 'Uni.createFrom().item(output)';

        if (step.cardinality === 'EXPANSION') {
            reactiveServiceInterface = 'ReactiveStreamingService';
            grpcAdapter = 'GrpcServiceStreamingAdapter';
            processMethodReturnType = `Multi<${step.outputTypeName}>`;
            returnStatement = 'Multi.createFrom().item(output)';
        } else if (step.cardinality === 'REDUCTION') {
            reactiveServiceInterface = 'ReactiveStreamingClientService';
            grpcAdapter = 'GrpcServiceClientStreamingAdapter';
            processMethodParamType = `Multi<${step.inputTypeName}>`;
            returnStatement = 'Uni.createFrom().item(output)';
        } else if (step.cardinality === 'SIDE_EFFECT') {
            reactiveServiceInterface = 'ReactiveService';
            grpcAdapter = 'GrpcReactiveServiceAdapter';
            returnStatement = 'Uni.createFrom().item(input)';
        }

        context.reactiveServiceInterface = reactiveServiceInterface;
        context.grpcAdapter = grpcAdapter;
        context.processMethodReturnType = processMethodReturnType;
        context.processMethodParamType = processMethodParamType;
        context.returnStatement = returnStatement;

        const rendered = this.render('step-service', context);
        const servicePath = path.join(stepPath, 'src/main/java', this.toPath(basePackage + '.' + context.serviceNameForPackage + '.service'), 'Process' + serviceNamePascal + 'Service.java');
        await fs.writeFile(servicePath, rendered);
    }

    async generateDockerfile(serviceName, stepPath) {
        const context = { serviceName };
        const rendered = this.render('dockerfile', context);
        const dockerfilePath = path.join(stepPath, 'Dockerfile');
        await fs.writeFile(dockerfilePath, rendered);
    }

    async generateOrchestrator(appName, basePackage, steps, outputPath) {
        const orchPath = path.join(outputPath, 'orchestrator-svc');
        const classPath = path.join(orchPath, 'src/main/java', this.toPath(basePackage + '.orchestrator.service'));
        await fs.ensureDir(classPath);

        // Generate orchestrator application
        await this.generateOrchestratorApplication(appName, basePackage, classPath, steps[0].inputTypeName);

        // Generate orchestrator POM
        await this.generateOrchestratorPom(appName, basePackage, orchPath);

        // Generate Dockerfile
        await this.generateDockerfile('orchestrator-svc', orchPath);
    }

    async generateOrchestratorPom(appName, basePackage, orchPath) {
        const context = {
            basePackage,
            artifactId: 'orchestrator-svc',
            rootProjectName: appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-')
        };

        const rendered = this.render('orchestrator-pom', context);
        const pomPath = path.join(orchPath, 'pom.xml');
        await fs.writeFile(pomPath, rendered);
    }

    async generateOrchestratorApplication(appName, basePackage, classPath, firstInputTypeName) {
        const context = {
            appName,
            basePackage,
            classPath,
            firstInputTypeName
        };

        const rendered = this.render('orchestrator-application', context);
        const mainAppPath = path.join(classPath, 'OrchestratorApplication.java');
        await fs.writeFile(mainAppPath, rendered);
    }

    async generateDockerCompose(appName, steps, outputPath) {
        // Process steps to add additional properties
        const processedSteps = steps.map((step, i) => ({
            ...step,
            portNumber: 8444 + i,
            serviceNameUpperCase: step.serviceName.toUpperCase().replace(/-/g, '_')
        }));

        const context = {
            appName,
            steps: processedSteps
        };

        const rendered = this.render('docker-compose', context);
        const composePath = path.join(outputPath, 'docker-compose.yml');
        await fs.writeFile(composePath, rendered);
    }

    async generateUtilityScripts(outputPath) {
        // Generate up-docker.sh
        const context = {};
        const upDockerContent = this.render('up-docker', context);
        const upDockerPath = path.join(outputPath, 'up-docker.sh');
        await fs.writeFile(upDockerPath, upDockerContent);
        await fs.chmod(upDockerPath, '755'); // Make executable

        // Generate down-docker.sh
        const downDockerContent = this.render('down-docker', context);
        const downDockerPath = path.join(outputPath, 'down-docker.sh');
        await fs.writeFile(downDockerPath, downDockerContent);
        await fs.chmod(downDockerPath, '755'); // Make executable

        // Generate up-local.sh
        const upLocalContent = this.render('up-local', context);
        const upLocalPath = path.join(outputPath, 'up-local.sh');
        await fs.writeFile(upLocalPath, upLocalContent);

        // Generate down-local.sh
        const downLocalContent = this.render('down-local', context);
        const downLocalPath = path.join(outputPath, 'down-local.sh');
        await fs.writeFile(downLocalPath, downLocalContent);
    }

    async generateObservabilityConfigs(outputPath) {
        // Generate otel-collector-config.yaml
        const context = {};
        const otelContent = this.render('otel-collector-config', context);
        const otelPath = path.join(outputPath, 'otel-collector-config.yaml');
        await fs.writeFile(otelPath, otelContent);

        // Generate prometheus.yml
        const prometheusContent = this.render('prometheus', context);
        const prometheusPath = path.join(outputPath, 'prometheus.yml');
        await fs.writeFile(prometheusPath, prometheusContent);

        // Generate grafana datasources
        const grafanaDatasourcesContent = this.render('grafana-datasources', context);
        const grafanaDatasourcesPath = path.join(outputPath, 'grafana-datasources.yaml');
        await fs.writeFile(grafanaDatasourcesPath, grafanaDatasourcesContent);

        // Generate grafana dashboards
        const grafanaDashboardsContent = this.render('grafana-dashboards', context);
        const grafanaDashboardsPath = path.join(outputPath, 'grafana-dashboards.yaml');
        await fs.writeFile(grafanaDashboardsPath, grafanaDashboardsContent);

        // Generate tempo config
        const tempoContent = this.render('tempo', context);
        const tempoPath = path.join(outputPath, 'tempo.yaml');
        await fs.writeFile(tempoPath, tempoContent);
    }

    async generateMvNWFiles(outputPath) {
        // Create mvnw (Unix)
        const context = {};
        const mvnwContent = this.render('mvnw', context);
        const mvnwPath = path.join(outputPath, 'mvnw');
        await fs.writeFile(mvnwPath, mvnwContent);
        await fs.chmod(mvnwPath, '755'); // Make executable

        // Create mvnw.cmd (Windows)
        const mvnwCmdContent = this.render('mvnw-cmd', context);
        const mvnwCmdPath = path.join(outputPath, 'mvnw.cmd');
        await fs.writeFile(mvnwCmdPath, mvnwCmdContent);
    }

    async generateMavenWrapperFiles(outputPath) {
        // Create .mvn/wrapper directory
        const wrapperDir = path.join(outputPath, '.mvn', 'wrapper');
        await fs.ensureDir(wrapperDir);

        // Copy maven-wrapper.properties (we'll use a template)
        // In a real implementation, we'd have this file in our templates directory
        const mavenWrapperProperties = `# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
`;
        await fs.writeFile(path.join(wrapperDir, 'maven-wrapper.properties'), mavenWrapperProperties);
    }

    async generateOtherFiles(appName, outputPath) {
        // Create README
        const readmeContext = { appName };
        const readmeContent = this.render('readme', readmeContext);
        const readmePath = path.join(outputPath, 'README.md');
        await fs.writeFile(readmePath, readmeContent);

        // Create .gitignore
        const gitignoreContent = this.render('gitignore', {});
        const gitignorePath = path.join(outputPath, '.gitignore');
        await fs.writeFile(gitignorePath, gitignoreContent);
    }

    // Utility methods
    toPath(packageName) {
        return packageName.replace(/\./g, '/');
    }

    hasImportFlag(fields, types) {
        if (!Array.isArray(fields)) return false;
        return fields.some(field => types.includes(field.type));
    }

    hasMapType(fields) {
        if (!Array.isArray(fields)) return false;
        return fields.some(field => field.type.startsWith('Map<'));
    }

    formatForClassName(input) {
        if (!input) return '';
        // Split by spaces and capitalize each word
        const parts = input.split(' ');
        return parts
            .filter(part => part)
            .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
            .join('');
    }

    formatForProtoClassName(input) {
        if (!input) return '';
        // Convert service names like "process-customer-svc" to "ProcessCustomerSvc"
        const parts = input.split('-');
        return parts
            .filter(part => part)
            .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
            .join('');
    }

    extractEntityName(serviceNamePascal) {
        // If it starts with "Process", return everything after "Process"
        if (serviceNamePascal.startsWith('Process')) {
            return serviceNamePascal.substring('Process'.length);
        }
        // For other cases, we'll default to the whole string
        return serviceNamePascal;
    }
}

module.exports = HandlebarsTemplateEngine;