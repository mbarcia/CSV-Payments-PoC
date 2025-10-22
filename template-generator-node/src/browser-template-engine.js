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

// Import Handlebars (this would be included via a script tag in the browser)
// For Node.js compatibility, we'll handle both cases
let Handlebars;
if (typeof window !== 'undefined' && window.Handlebars) {
    // Browser environment
    Handlebars = window.Handlebars;
} else {
    // Node.js environment
    Handlebars = require('handlebars');
}

// Register helper for replacing characters in strings
Handlebars.registerHelper('replace', function(str, find, repl) {
  if (typeof str !== 'string') return str;
  if (typeof find !== 'string') return str;
  if (typeof str.replaceAll === 'function') return str.replaceAll(find, repl);
  return str.split(find).join(repl);
});
// Register helper for converting to lowercase
Handlebars.registerHelper('lowercase', function(str) {
    return typeof str === 'string' ? str.toLowerCase() : str;
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
    if (!type || typeof type !== 'string') return false;
    
    // Simple list check (for basic List)
    if (type === 'List') return true;
    
    // Pattern check for generic List (e.g. List<String>, List<MyCustomType>)
    return type.startsWith('List<');
});

// Register helper for extracting list inner type
Handlebars.registerHelper('listInnerType', function(type) {
    if (!type || !type.startsWith('List<') || !type.endsWith('>')) {
        return type;
    }
    return type.substring(5, type.length - 1).trim();
});

// Register helper for checking if a type is a map
Handlebars.registerHelper('isMapType', function(type) {
    if (!type || typeof type !== 'string') return false;
    
    // Simple map check (for basic Map)
    if (type === 'Map') return true;
    
    // Pattern check for generic Map (e.g. Map<String,Integer>, Map<MyKey,MyValue>)
    return type.startsWith('Map<');
});

// Register helper for checking various import flags
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

class BrowserTemplateEngine {
    constructor(templates) {
        this.templates = templates || {};
        this.compiledTemplates = new Map();
        this.loadTemplates();
    }

    loadTemplates() {
        // In browser environment, templates are passed in as an object
        // Each template is a string that needs to be compiled
        for (const [name, templateStr] of Object.entries(this.templates)) {
            this.compiledTemplates.set(name, Handlebars.compile(templateStr));
        }
    }

    render(templateName, context) {
        const template = this.compiledTemplates.get(templateName);
        if (!template) {
            throw new Error(`Template ${templateName} not found`);
        }
        return template(context);
    }

    async generateApplication(appName, basePackage, steps, fileCallback) {
        // For sequential pipeline, update input types of steps after the first one
        // to match the output type of the previous step
        for (let i = 1; i < steps.length; i++) {
            const currentStep = steps[i];
            const previousStep = steps[i - 1];
            // Set the input type of the current step to the output type of the previous step
            currentStep.inputTypeName = previousStep.outputTypeName;
            currentStep.inputFields = Array.isArray(previousStep.outputFields)
              ? previousStep.outputFields.slice()
              : previousStep.outputFields; // Shallow copy input fields from previous step's outputs
        }

        // Generate parent POM
        await this.generateParentPom(appName, basePackage, steps, fileCallback);

        // Generate common module
        await this.generateCommonModule(appName, basePackage, steps, fileCallback);

        // Generate each step service
        for (let i = 0; i < steps.length; i++) {
            await this.generateStepService(appName, basePackage, steps[i], i, steps, fileCallback);
        }

        // Generate orchestrator
        await this.generateOrchestrator(appName, basePackage, steps, fileCallback);

        // Generate docker-compose
        await this.generateDockerCompose(appName, steps, fileCallback);

        // Generate utility scripts
        await this.generateUtilityScripts(fileCallback);

        // Generate observability configs
        await this.generateObservabilityConfigs(fileCallback);

        // Generate mvnw files
        await this.generateMvNWFiles(fileCallback);

        // Generate Maven wrapper files
        await this.generateMavenWrapperFiles(fileCallback);

        // Generate other files
        await this.generateOtherFiles(appName, fileCallback);
    }

    async generateParentPom(appName, basePackage, steps, fileCallback) {
        const context = {
            basePackage,
            artifactId: appName
              .toLowerCase()
              .replace(/[^a-z0-9]+/g, '-')
              .replace(/^-+|-+$/g, ''),
            name: appName,
            steps
        };

        const rendered = this.render('parent-pom', context);
        await fileCallback('pom.xml', rendered);
    }

    async generateCommonModule(appName, basePackage, steps, fileCallback) {
        // Generate common POM
        await this.generateCommonPom(appName, basePackage, fileCallback);

        // Generate proto files for each step
        for (let i = 0; i < steps.length; i++) {
            await this.generateProtoFile(steps[i], basePackage, i, steps, fileCallback);
        }

        // Generate entities, DTOs, and mappers for each step
        for (let i = 0; i < steps.length; i++) {
            const step = steps[i];
            await this.generateDomainClasses(step, basePackage, i, fileCallback);
            await this.generateDtoClasses(step, basePackage, i, fileCallback);
            await this.generateMapperClasses(step, basePackage, i, fileCallback);
        }

        // Generate base entity
        await this.generateBaseEntity(basePackage, fileCallback);

        // Generate common converters
        await this.generateCommonConverters(basePackage, fileCallback);
    }

    async generateCommonPom(appName, basePackage, fileCallback) {
        const context = {
            basePackage,
            rootProjectName: appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-')
        };

        const rendered = this.render('common-pom', context);
        await fileCallback('common/pom.xml', rendered);
    }

    async generateProtoFile(step, basePackage, stepIndex, allSteps, fileCallback) {
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
        await fileCallback(`common/src/main/proto/${step.serviceName}.proto`, rendered);
    }

    async generateDomainClasses(step, basePackage, stepIndex, fileCallback) {
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
                hasIdField: step.inputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('domain', inputContext);
            const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.domain')}/${step.inputTypeName}.java`;
            await fileCallback(filePath, rendered);
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
                hasIdField: step.outputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('domain', outputContext);
            const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.domain')}/${step.outputTypeName}.java`;
            await fileCallback(filePath, rendered);
        }
    }

    async generateBaseEntity(basePackage, fileCallback) {
        const context = { basePackage };
        const rendered = this.render('base-entity', context);
        const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.domain')}/BaseEntity.java`;
        await fileCallback(filePath, rendered);
    }

    async generateDtoClasses(step, basePackage, stepIndex, fileCallback) {
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
                hasIdField: step.inputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('dto', inputContext);
            const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.dto')}/${step.inputTypeName}Dto.java`;
            await fileCallback(filePath, rendered);
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
                hasIdField: step.outputFields.some(field => field.name === 'id')
            };

            const rendered = this.render('dto', outputContext);
            const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.dto')}/${step.outputTypeName}Dto.java`;
            await fileCallback(filePath, rendered);
        }
    }

    async generateMapperClasses(step, basePackage, stepIndex, fileCallback) {
        // Generate input mapper class only for first step (since other steps reference previous step's output)
        if (stepIndex === 0 && step.inputTypeName) {
            await this.generateMapperClass(step.inputTypeName, step, basePackage, fileCallback);
        }

        // Generate output mapper class for all steps
        if (step.outputTypeName) {
            await this.generateMapperClass(step.outputTypeName, step, basePackage, fileCallback);
        }
    }

    async generateMapperClass(className, step, basePackage, fileCallback) {
        const context = {
            ...step,
            basePackage,
            className,
            domainClass: className.replace('Dto', ''),
            dtoClass: className + 'Dto',
            grpcClass: basePackage + '.grpc.' + this.formatForProtoClassName(step.serviceName)
        };

        const rendered = this.render('mapper', context);
        const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.mapper')}/${className}Mapper.java`;
        await fileCallback(filePath, rendered);
    }

    async generateCommonConverters(basePackage, fileCallback) {
        const context = { basePackage };
        const rendered = this.render('common-converters', context);
        const filePath = `common/src/main/java/${this.toPath(basePackage + '.common.mapper')}/CommonConverters.java`;
        await fileCallback(filePath, rendered);
    }

    async generateStepService(appName, basePackage, step, stepIndex, allSteps, fileCallback) {
        // noinspection JSUnusedLocalSymbols
        const serviceNameForPackage = step.serviceName.replace('-svc', '').replace(/-/g, '_');

        // Add rootProjectName to step map
        step.rootProjectName = appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-');

        // Generate step POM
        await this.generateStepPom(step, basePackage, fileCallback);

        // Generate the service class
        await this.generateStepServiceClass(appName, basePackage, step, stepIndex, allSteps, fileCallback);

        // Generate Dockerfile
        await this.generateDockerfile(step.serviceName, fileCallback);
    }

    async generateStepPom(step, basePackage, fileCallback) {
        const context = { ...step, basePackage };
        const rendered = this.render('step-pom', context);
        const filePath = `${step.serviceName}/pom.xml`;
        await fileCallback(filePath, rendered);
    }

    async generateStepServiceClass(appName, basePackage, step, stepIndex, allSteps, fileCallback) {
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
        const serviceNameCamel = step.serviceNameCamel ?? (step.serviceName || '').replace(/-svc$/, '').replace(/-([a-z])/g, (_, c) => c.toUpperCase());
        // Convert camelCase to PascalCase
        const serviceNamePascal = serviceNameCamel
          ? serviceNameCamel.charAt(0).toUpperCase() + serviceNameCamel.slice(1)
          : this.formatForProtoClassName(step.serviceName);

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
        const filePath = `${step.serviceName}/src/main/java/${this.toPath(basePackage + '.' + context.serviceNameForPackage + '.service')}/Process${serviceNamePascal}Service.java`;
        await fileCallback(filePath, rendered);
    }

    async generateDockerfile(serviceName, fileCallback) {
        const context = { serviceName };
        const rendered = this.render('dockerfile', context);
        await fileCallback(`${serviceName}/Dockerfile`, rendered);
    }

    async generateOrchestrator(appName, basePackage, steps, fileCallback) {
        // Generate orchestrator POM
        await this.generateOrchestratorPom(appName, basePackage, fileCallback);

        // Generate Dockerfile
        await this.generateDockerfile('orchestrator-svc', fileCallback);
    }

    async generateOrchestratorPom(appName, basePackage, fileCallback) {
        const context = {
            basePackage,
            artifactId: 'orchestrator-svc',
            rootProjectName: appName.toLowerCase().replace(/[^a-zA-Z0-9]/g, '-')
        };

        const rendered = this.render('orchestrator-pom', context);
        await fileCallback('orchestrator-svc/pom.xml', rendered);
    }

    async generateDockerCompose(appName, steps, fileCallback) {
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
        await fileCallback('docker-compose.yml', rendered);
    }

    async generateUtilityScripts(fileCallback) {
        // Generate up-docker.sh
        const context = {};
        const upDockerContent = this.render('up-docker', context);
        await fileCallback('up-docker.sh', upDockerContent);

        // Generate down-docker.sh
        const downDockerContent = this.render('down-docker', context);
        await fileCallback('down-docker.sh', downDockerContent);

        // Generate up-local.sh
        const upLocalContent = this.render('up-local', context);
        await fileCallback('up-local.sh', upLocalContent);

        // Generate down-local.sh
        const downLocalContent = this.render('down-local', context);
        await fileCallback('down-local.sh', downLocalContent);
    }

    async generateObservabilityConfigs(fileCallback) {
        // Generate otel-collector-config.yaml
        const context = {};
        const otelContent = this.render('otel-collector-config', context);
        await fileCallback('otel-collector-config.yaml', otelContent);

        // Generate prometheus.yml
        const prometheusContent = this.render('prometheus', context);
        await fileCallback('prometheus.yml', prometheusContent);

        // Generate grafana datasources
        const grafanaDatasourcesContent = this.render('grafana-datasources', context);
        await fileCallback('grafana-datasources.yaml', grafanaDatasourcesContent);

        // Generate grafana dashboards
        const grafanaDashboardsContent = this.render('grafana-dashboards', context);
        await fileCallback('grafana-dashboards.yaml', grafanaDashboardsContent);

        // Generate tempo config
        const tempoContent = this.render('tempo', context);
        await fileCallback('tempo.yaml', tempoContent);
    }

    async generateMvNWFiles(fileCallback) {
        // Create mvnw (Unix)
        const context = {};
        const mvnwContent = this.render('mvnw', context);
        await fileCallback('mvnw', mvnwContent);

        // Create mvnw.cmd (Windows)
        const mvnwCmdContent = this.render('mvnw-cmd', context);
        await fileCallback('mvnw.cmd', mvnwCmdContent);
    }

    async generateMavenWrapperFiles(fileCallback) {
        // Create .mvn/wrapper directory and maven-wrapper.properties
        // This is a simple content for the maven wrapper properties
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
        await fileCallback('.mvn/wrapper/maven-wrapper.properties', mavenWrapperProperties);
    }

    async generateOtherFiles(appName, fileCallback) {
        // Create README
        const readmeContext = { appName };
        const readmeContent = this.render('readme', readmeContext);
        await fileCallback('README.md', readmeContent);

        // Create .gitignore
        const gitignoreContent = this.render('gitignore', {});
        await fileCallback('.gitignore', gitignoreContent);
    }

    // Utility methods
    toPath(packageName) {
        return packageName.replace(/\./g, '/');
    }

    hasImportFlag(fields, types) {
        if (!Array.isArray(fields)) return false;
        return fields.some(field => types.includes(field.type));
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

// Export for both Node.js and browser environments
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    module.exports = BrowserTemplateEngine;
} else {
    // Browser environment
    window.BrowserTemplateEngine = BrowserTemplateEngine;
}