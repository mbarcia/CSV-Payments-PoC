<script>
  import { onMount } from 'svelte';
  import { saveAs } from 'file-saver';
  import { load } from 'js-yaml';
  import StepArrow from '$lib/components/StepArrow.svelte';
  import FieldForm from '$lib/components/FieldForm.svelte';
  import CombinedFieldForm from '$lib/components/CombinedFieldForm.svelte';
  import Connector from '$lib/components/Connector.svelte';
  import ConfirmationDialog from '$lib/components/ConfirmationDialog.svelte';

    // Initialize with an empty configuration
  let config = {
    appName: 'My Pipeline App',
    basePackage: 'com.example.mypipeline',
    steps: []
  };

  // State for field forms
  let showInputForm = false;
  let currentStepIndex = -1;
  let currentFormType = '';
  
  // State for confirmation dialog
  let showConfirmationDialog = false;
  let confirmationTitle = '';
  let confirmationMessage = '';
  let confirmationWarning = false;
  let pendingStepIndex = -1;
  let pendingCardinality = '';
  
  // State for animations
  let animatedStepIndex = -1;
  let animationTimeout;
  
  // Cardinality options
  const cardinalityOptions = [
    { value: 'ONE_TO_ONE', label: '1-1 (One-to-One)' },
    { value: 'EXPANSION', label: 'Expansion (1-Many)' },
    { value: 'REDUCTION', label: 'Reduction (Many-1)' },
    { value: 'SIDE_EFFECT', label: 'Side-effect' }
  ];

  // Protobuf scalar types
  const validProtoTypes = ['string', 'int32', 'int64', 'float', 'double', 'bool', 'bytes', 'Enum'];
  
  // Field types for dropdowns (using Protobuf scalar types)
  const fieldTypes = [
    'string', 'int32', 'int64', 'float', 'double', 'bool', 'bytes', 'Enum'
  ];
  
  // Function to check if a type is valid (scalar, Enum, or defined in previous steps)
  function isValidFieldType(type, currentStepIndex) {
    // First check if it's a scalar or Enum
    if (validProtoTypes.includes(type)) {
      return true;
    }
    
    // If not scalar or Enum, check if it's defined in any previous step as an input or output type
    for (let i = 0; i < currentStepIndex; i++) {
      const step = config.steps[i];
      if (step.inputTypeName === type || step.outputTypeName === type) {
        return true;
      }
    }
    
    return false;
  }
  
  // Function to get all available field types including custom message types
  function getAvailableFieldTypes(currentStepIndex) {
    // Start with basic scalar types and Enum
    let allTypes = [...validProtoTypes];
    
    // Add custom message types from previous steps
    if (config && config.steps && Array.isArray(config.steps)) {
      for (let i = 0; i < currentStepIndex && i < config.steps.length; i++) {
        const step = config.steps[i];
        if (step.inputTypeName && !allTypes.includes(step.inputTypeName)) {
          allTypes.push(step.inputTypeName);
        }
        if (step.outputTypeName && !allTypes.includes(step.outputTypeName)) {
          allTypes.push(step.outputTypeName);
        }
      }
    }
    
    return allTypes;
  }

  // Add a new step
  function addStep() {
    const isSideEffect = false; // New steps default to ONE_TO_ONE
    const newStep = {
      name: `Step ${config.steps.length + 1}`,
      serviceName: `step-${config.steps.length + 1}-svc`,
      serviceNameCamel: `step${config.steps.length + 1}`,
      cardinality: 'ONE_TO_ONE',
      stepType: 'StepOneToOne',
      inputTypeName: config.steps.length === 0 ? 'InputType' : config.steps[config.steps.length - 1].outputTypeName, // Pipeline connection
      inputTypeSimpleName: config.steps.length === 0 ? 'InputType' : config.steps[config.steps.length - 1].outputTypeSimpleName,
      inputFields: config.steps.length === 0 ? [] : [...config.steps[config.steps.length - 1].outputFields], // Copy output fields from previous step
      outputTypeName: isSideEffect ? (config.steps.length === 0 ? 'InputType' : config.steps[config.steps.length - 1].outputTypeName) : `OutputType${config.steps.length + 1}`,
      outputTypeSimpleName: isSideEffect ? (config.steps.length === 0 ? 'InputType' : config.steps[config.steps.length - 1].outputTypeSimpleName) : `OutputType${config.steps.length + 1}`,
      outputFields: isSideEffect ? (config.steps.length === 0 ? [] : [...config.steps[config.steps.length - 1].outputFields]) : [],
      order: config.steps.length + 1,
      grpcClientName: `Step${config.steps.length + 1}Svc`
    };
    config.steps = [...config.steps, newStep];
    
    // Set up animation for the new step
    const newIndex = config.steps.length - 1;
    animatedStepIndex = newIndex;
    
    // Clear any existing animation timeout
    if (animationTimeout) {
      clearTimeout(animationTimeout);
    }
    
    // Set timeout to remove animation after 2 seconds
    animationTimeout = setTimeout(() => {
      animatedStepIndex = -1;
    }, 2000);
    
    // Automatically open the form for the new step
    currentStepIndex = newIndex;
    currentFormType = 'both';
    showInputForm = true;
  }

  // Remove a step
  function removeStep(index) {
    config.steps = config.steps.filter((_, i) => i !== index);
    // Update order numbers after removal
    config.steps.forEach((step, i) => {
      step.order = i + 1;
      // Update input types for subsequent steps
      if (i > 0) {
        step.inputTypeName = config.steps[i - 1].outputTypeName;
        step.inputTypeSimpleName = config.steps[i - 1].outputTypeSimpleName;
      }
    });
  }

  // Add a field to a step
  function addField(stepIndex, type) {
    const step = config.steps[stepIndex];
    const fields = type === 'input' ? step.inputFields : step.outputFields;
    
    const newField = {
      name: `${type}Field${fields.length + 1}`,
      type: 'string',  // Using protobuf type directly
      protoType: 'string'
    };
    
    fields.push(newField);
    
    // Handle Side-Effect steps - keep input and output fields in sync
    if (step.cardinality === 'SIDE_EFFECT') {
      if (type === 'input') {
        step.outputFields.push({ ...newField });
      } else {
        step.inputFields.push({ ...newField });
      }
    }
    
    // Bidirectional synchronization
    if (type === 'output') {
      // If we're adding an output field, also add it to the input fields of the next step
      if (stepIndex < config.steps.length - 1) {
        const nextStep = config.steps[stepIndex + 1];
        nextStep.inputFields.push({ ...newField });
        config.steps[stepIndex + 1] = { ...nextStep };
      }
    } else if (type === 'input' && stepIndex > 0) {
      // If we're adding an input field, also add it to the output fields of the previous step
      const prevStep = config.steps[stepIndex - 1];
      prevStep.outputFields.push({ ...newField });
      config.steps[stepIndex - 1] = { ...prevStep };
    }
    
    // Update the step in the array
    config.steps[stepIndex] = { ...step };
    config = { ...config };
  }

  // Remove a field from a step
  function removeField(stepIndex, type, fieldIndex) {
    const step = config.steps[stepIndex];
    const fields = type === 'input' ? step.inputFields : step.outputFields;
    
    fields.splice(fieldIndex, 1);
    
    // Handle Side-Effect steps - keep input and output fields in sync
    if (step.cardinality === 'SIDE_EFFECT') {
      if (type === 'input') {
        step.outputFields.splice(fieldIndex, 1);
      } else {
        step.inputFields.splice(fieldIndex, 1);
      }
    }
    
    // Bidirectional synchronization
    if (type === 'output') {
      // If we're removing an output field, also remove it from the input fields of the next step
      if (stepIndex < config.steps.length - 1) {
        const nextStep = config.steps[stepIndex + 1];
        if (nextStep.inputFields && nextStep.inputFields[fieldIndex]) {
          nextStep.inputFields.splice(fieldIndex, 1);
          config.steps[stepIndex + 1] = { ...nextStep };
        }
      }
    } else if (type === 'input' && stepIndex > 0) {
      // If we're removing an input field, also remove it from the output fields of the previous step
      const prevStep = config.steps[stepIndex - 1];
      if (prevStep.outputFields && prevStep.outputFields[fieldIndex]) {
        prevStep.outputFields.splice(fieldIndex, 1);
        config.steps[stepIndex - 1] = { ...prevStep };
      }
    }
    
    // Update the step in the array
    config.steps[stepIndex] = { ...step };
    config = { ...config };
  }

  // Update a field's property
  function updateField(stepIndex, type, fieldIndex, property, value) {
    const step = config.steps[stepIndex];
    const fields = type === 'input' ? step.inputFields : step.outputFields;
    
    // Validate type if the property being updated is 'type'
    if (property === 'type' && !isValidFieldType(value, stepIndex)) {
      alert(`Invalid type '${value}'. Valid types are: ${validProtoTypes.join(', ')}, or custom message types defined in previous steps.`);
      return; // Don't proceed with the update
    }
    
    fields[fieldIndex][property] = value;
    
    // Update protoType - now the type IS the proto type
    if (property === 'type') {
      fields[fieldIndex].protoType = value;
    }
    
    // Handle Side-Effect steps - keep input and output fields in sync
    if (step.cardinality === 'SIDE_EFFECT') {
      if (type === 'input') {
        step.outputFields[fieldIndex][property] = value;
        if (property === 'type') {
          step.outputFields[fieldIndex].protoType = value;
        }
      } else {
        step.inputFields[fieldIndex][property] = value;
        if (property === 'type') {
          step.inputFields[fieldIndex].protoType = value;
        }
      }
    }
    
    // Update the step in the array
    config.steps[stepIndex] = { ...step };
    
    // Bidirectional synchronization
    if (type === 'output') {
      // If we updated an output field, also update the input field of the next step
      if (stepIndex < config.steps.length - 1) {
        const nextStep = config.steps[stepIndex + 1];
        if (nextStep && nextStep.inputFields && nextStep.inputFields[fieldIndex]) {
          nextStep.inputFields[fieldIndex][property] = value;
          if (property === 'type') {
            nextStep.inputFields[fieldIndex].protoType = value;
          }
          config.steps[stepIndex + 1] = { ...nextStep };
        }
      }
    } else if (type === 'input' && stepIndex > 0) {
      // If we updated an input field, also update the output field of the previous step
      const prevStep = config.steps[stepIndex - 1];
      if (prevStep && prevStep.outputFields && prevStep.outputFields[fieldIndex]) {
        prevStep.outputFields[fieldIndex][property] = value;
        if (property === 'type') {
          prevStep.outputFields[fieldIndex].protoType = value;
        }
        config.steps[stepIndex - 1] = { ...prevStep };
      }
    }
    
    config = { ...config };
  }

  // Get the corresponding proto type - now we're using proto types directly
  function getProtoType(protoType) {
    // Validate that it's a valid proto type
    const validTypes = ['string', 'int32', 'int64', 'float', 'double', 'bool', 'bytes', 'Enum'];
    return validTypes.includes(protoType) ? protoType : 'string';
  }

  // Generate YAML and download
  function downloadYaml() {
    // For now, create a simple YAML string to avoid complex escaping issues
    let yamlContent = '---\n';
    yamlContent += 'appName: \"' + config.appName + '\"\n';
    yamlContent += 'basePackage: \"' + config.basePackage + '\"\n';
    yamlContent += 'steps:\n';
    
    for (let i = 0; i < config.steps.length; i++) {
      const step = config.steps[i];
      yamlContent += '  - stepType: \"' + step.stepType + '\"\n';
      yamlContent += '    serviceNameCamel: \"' + step.serviceNameCamel + '\"\n';
      yamlContent += '    serviceName: \"' + step.serviceName + '\"\n';
      yamlContent += '    cardinality: \"' + step.cardinality + '\"\n';
      yamlContent += '    inputFields:\n';
      
      for (let j = 0; j < step.inputFields.length; j++) {
        const field = step.inputFields[j];
        yamlContent += '    - protoType: \"' + field.protoType + '\"\n';
        yamlContent += '      name: \"' + field.name + '\"\n';
        yamlContent += '      type: \"' + field.type + '\"\n';
      }
      
      yamlContent += '    outputFields:\n';
      
      for (let k = 0; k < step.outputFields.length; k++) {
        const field = step.outputFields[k];
        yamlContent += '    - protoType: \"' + field.protoType + '\"\n';
        yamlContent += '      name: \"' + field.name + '\"\n';
        yamlContent += '      type: \"' + field.type + '\"\n';
      }
      
      yamlContent += '    outputTypeName: \"' + step.outputTypeName + '\"\n';
      yamlContent += '    inputTypeName: \"' + step.inputTypeName + '\"\n';
      yamlContent += '    outputTypeSimpleName: \"' + step.outputTypeSimpleName + '\"\n';
      yamlContent += '    grpcClientName: \"' + step.grpcClientName + '\"\n';
      yamlContent += '    name: \"' + step.name + '\"\n';
      yamlContent += '    inputTypeSimpleName: \"' + step.inputTypeSimpleName + '\"\n';
      yamlContent += '    order: ' + step.order + '\n';
    }

    const blob = new Blob([yamlContent], { type: 'application/yaml' });
    saveAs(blob, config.appName.replace(/\s+/g, '-') + '-config.yaml');
  }

  // Handle file upload
  async function handleFileUpload(event) {
    const file = event.target.files[0];
    if (file && (file.name.endsWith('.yaml') || file.name.endsWith('.yml'))) {
      const text = await file.text();
      try {
        const data = load(text);
        
        // Validate the data structure
        if (!data.appName || !data.basePackage || !data.steps) {
          throw new Error('Invalid configuration file: missing required fields (appName, basePackage, steps)');
        }
        
        if (!Array.isArray(data.steps)) {
          throw new Error('Invalid configuration file: steps must be an array');
        }
        
        // Validate each step has required fields
        for (let i = 0; i < data.steps.length; i++) {
          const step = data.steps[i];
          if (!step.serviceName || !step.serviceNameCamel || !step.cardinality) {
            throw new Error(`Invalid configuration file: step ${i+1} is missing required fields (serviceName, serviceNameCamel, cardinality)`);
          }
          
          if (!step.inputFields || !Array.isArray(step.inputFields)) {
            step.inputFields = [];
          }
          
          if (!step.outputFields || !Array.isArray(step.outputFields)) {
            step.outputFields = [];
          }
          
          // Validate fields have required properties and valid types
          for (let j = 0; j < step.inputFields.length; j++) {
            const field = step.inputFields[j];
            if (!field.name || !field.type || !field.protoType) {
              throw new Error(`Invalid configuration file: input field ${j+1} in step ${i+1} is missing required properties (name, type, protoType)`);
            }
            // Validate that field.type is a valid protobuf type or custom message type from a previous step
            if (!isValidFieldType(field.type, i)) {
              throw new Error(`Invalid configuration file: input field ${j+1} in step ${i+1} has invalid type '${field.type}'. Valid types are: ${validProtoTypes.join(', ')}, or custom message types defined in previous steps.`);
            }
          }
          
          for (let j = 0; j < step.outputFields.length; j++) {
            const field = step.outputFields[j];
            if (!field.name || !field.type || !field.protoType) {
              throw new Error(`Invalid configuration file: output field ${j+1} in step ${i+1} is missing required properties (name, type, protoType)`);
            }
            // Validate that field.type is a valid protobuf type or custom message type from a previous step
            if (!isValidFieldType(field.type, i)) {
              throw new Error(`Invalid configuration file: output field ${j+1} in step ${i+1} has invalid type '${field.type}'. Valid types are: ${validProtoTypes.join(', ')}, or custom message types defined in previous steps.`);
            }
          }
          
          // Set stepType based on cardinality
          if (!step.stepType) {
            switch (step.cardinality) {
              case 'EXPANSION':
                step.stepType = 'StepOneToMany';
                break;
              case 'REDUCTION':
                step.stepType = 'StepManyToOne';
                break;
              case 'SIDE_EFFECT':
                step.stepType = 'StepSideEffect';
                break;
              default:
                step.stepType = 'StepOneToOne';
                break;
            }
          }
          
          // For Side-Effect steps, ensure input and output types are aligned
          if (step.cardinality === 'SIDE_EFFECT') {
            step.outputTypeName = step.inputTypeName;
            step.outputTypeSimpleName = step.inputTypeSimpleName;
            step.outputFields = [...step.inputFields];
          }
        }
        
        config = data;
        currentStepIndex = -1;
        showInputForm = false;
      } catch (e) {
        alert('Error parsing YAML file: ' + e.message);
      }
    }
  }

  // Update step type when cardinality changes
  function updateStepType(stepIndex) {
    const step = config.steps[stepIndex];
    
    // Check if we're converting to SIDE_EFFECT and there are existing output fields
    if (step.cardinality === 'SIDE_EFFECT' && step.outputFields && step.outputFields.length > 0) {
      // Show confirmation dialog
      pendingStepIndex = stepIndex;
      pendingCardinality = step.cardinality;
      confirmationTitle = 'Convert to Side-Effect Step';
      confirmationMessage = `Converting to a Side-Effect step will synchronize the output type (${step.outputTypeName}) with the input type (${step.inputTypeName}). This will replace all existing output fields with the input fields. Are you sure you want to proceed?`;
      confirmationWarning = true;
      showConfirmationDialog = true;
      return; // Don't proceed with the change yet
    }
    
    // Proceed with the actual change
    applyStepTypeChange(stepIndex, step.cardinality);
  }
  
  // Apply the step type change after confirmation
  function applyStepTypeChange(stepIndex, cardinality) {
    const step = config.steps[stepIndex];
    step.cardinality = cardinality;
    
    switch (cardinality) {
      case 'EXPANSION':
        step.stepType = 'StepOneToMany';
        break;
      case 'REDUCTION':
        step.stepType = 'StepManyToOne';
        break;
      case 'SIDE_EFFECT':
        step.stepType = 'StepSideEffect';
        // For side-effect steps, output type should be the same as input type
        step.outputTypeName = step.inputTypeName;
        step.outputTypeSimpleName = step.inputTypeSimpleName;
        // Copy input fields to output fields for side-effect steps
        step.outputFields = [...step.inputFields];
        break;
      default:
        step.stepType = 'StepOneToOne';
        break;
    }
    config.steps[stepIndex] = { ...step };
    config = { ...config };
  }
  
  // Handle click on step - show combined form with both input and output fields
  function handleStepClick(stepIndex, side) {
    currentStepIndex = stepIndex;
    currentFormType = side; // Will be 'both' for steps
    
    if (side === 'both') {
      showInputForm = true; // Using input form to show both input/output for steps
    }
  }
  
  // Handle click on connector - show shared type form
  function handleConnectorClick(stepIndex, side, sharedType) {
    // The connector represents the connection between step (stepIndex-1) and step (stepIndex)
    // The data flowing through the connector is the output of step (stepIndex-1) which becomes
    // the input of step (stepIndex). We display the form for the step that receives this data,
    // which is step (stepIndex), so we show its input fields (which should match the previous step's output)
    currentStepIndex = stepIndex;  // Show the step that receives the data from the connector
    currentFormType = side; 
    showInputForm = true; // Using input form to represent shared type
  }
  
  // Close forms (we're now using a single combined form)
  function closeInputForm() {
    showInputForm = false;
    currentFormType = '';
  }
  
  // No longer using separate output form
  function closeOutputForm() {
    showInputForm = false; // Using single form now
    currentFormType = '';
  }
  
  // Add field from form (updated for CombinedFieldForm)
  function formAddField(type) {
    addField(currentStepIndex, type);
  }
  
  // Remove field from form (updated for CombinedFieldForm)
  function formRemoveField(type, fieldIndex) {
    removeField(currentStepIndex, type, fieldIndex);
  }
  
  // Update field from form (updated for CombinedFieldForm)
  function formUpdateField(type, fieldIndex, property, value) {
    updateField(currentStepIndex, type, fieldIndex, property, value);
  }
  
  // Update a step type property (for name changes)
  function updateStepTypeProperty(property, value) {
    if (currentStepIndex !== -1) {
      const step = config.steps[currentStepIndex];
      step[property] = value;
      config.steps[currentStepIndex] = { ...step };
      config = { ...config };
    }
  }
  // Handle confirmation dialog confirm
  function handleConfirmationConfirm() {
    showConfirmationDialog = false;
    if (pendingStepIndex !== -1 && pendingCardinality) {
      applyStepTypeChange(pendingStepIndex, pendingCardinality);
      pendingStepIndex = -1;
      pendingCardinality = '';
    }
  }
  
  // Handle confirmation dialog cancel
  function handleConfirmationCancel() {
    showConfirmationDialog = false;
    // Reset the select box to the previous value if needed
    if (pendingStepIndex !== -1) {
      const step = config.steps[pendingStepIndex];
      // We don't actually need to reset the UI since the user canceled
      pendingStepIndex = -1;
      pendingCardinality = '';
    }
  }
  
  // Cleanup function to clear timeouts
  function cleanup() {
    if (animationTimeout) {
      clearTimeout(animationTimeout);
    }
  }
  
  // Run cleanup when component unmounts
  $: if (isDestroyed) {
    cleanup();
  }
  
  let isDestroyed = false;
  
  // Set up cleanup for component destruction
  onMount(() => {
    return () => {
      isDestroyed = true;
      cleanup();
    };
  });
</script>

<main class="flex h-screen bg-gray-100">
  <!-- Metadata sidebar -->
  <div class="w-64 bg-white shadow-lg p-4 overflow-y-auto flex flex-col">
    <h1 class="text-xl font-bold mb-6 text-center">Pipeline Canvas</h1>
    
    <div class="flex-grow">
      <button 
        on:click={downloadYaml}
        class="w-full mb-3 px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
      >
        Download YAML
      </button>
      
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-1">Upload Config:</label>
        <input 
          type="file" 
          accept=".yaml,.yml"
          on:change={handleFileUpload}
          class="w-full text-sm"
        />
      </div>
    </div>
    
    <div class="mt-auto pt-4 border-t border-gray-200">
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 mb-1">App Name</label>
        <input 
          type="text" 
          bind:value={config.appName}
          class="w-full px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
        />
      </div>
      
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 mb-1">Base Package</label>
        <input 
          type="text" 
          bind:value={config.basePackage}
          class="w-full px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
        />
      </div>
    </div>
  </div>

  <!-- Canvas area -->
  <div class="flex-1 p-4 overflow-auto">
    <div class="flex flex-col items-center min-h-full">
      {#if config.steps.length === 0}
        <div class="flex flex-col items-center justify-center h-full text-gray-500">
          <p class="text-lg mb-4">No steps yet. Add your first step to get started.</p>
          <button 
            on:click={addStep}
            class="w-16 h-16 rounded-full bg-blue-600 text-white text-3xl flex items-center justify-center hover:bg-blue-700 shadow-lg"
          >
            +
          </button>
        </div>
      {:else}
        <div class="flex items-center mt-8">
          {#each config.steps as step, index}
            <div class="flex flex-col items-center">
              <StepArrow 
                {step} 
                {index}
                isAnimating={index === animatedStepIndex}
                on:clickStep={(e) => handleStepClick(e.detail.index, e.detail.side)}
              />
              
              <!-- Step controls -->
              <div class="mt-1 flex flex-col items-center">
                <select 
                  bind:value={step.cardinality}
                  on:change={() => updateStepType(index)}
                  class="text-xs px-1 py-1 border border-gray-300 rounded"
                >
                  {#each cardinalityOptions as option}
                    <option value={option.value}>{option.label}</option>
                  {/each}
                </select>
                <div class="flex space-x-1 mt-1">
                  <input 
                    type="text" 
                    bind:value={step.name}
                    class="text-xs w-20 px-1 py-1 border border-gray-300 rounded"
                  />
                  <button 
                    on:click={() => removeStep(index)}
                    class="text-xs px-1.5 py-1 bg-red-600 text-white rounded hover:bg-red-700"
                  >
                    Del
                  </button>
                </div>
              </div>
            </div>
            
            <!-- Connector between steps -->
            {#if index < config.steps.length - 1}
              <Connector 
                stepIndex={index + 1}
                on:clickConnector={(e) => handleConnectorClick(e.detail.stepIndex, e.detail.side, e.detail.sharedType)}
                sharedType={step.outputTypeName}
              />
            {:else}
              <!-- Add Step button at the end -->
              <button 
                on:click={addStep}
                class="w-12 h-12 rounded-full bg-blue-600 text-white text-2xl flex items-center justify-center hover:bg-blue-700 shadow-lg ml-4 self-center"
                title="Add Step"
              >
                +
              </button>
            {/if}
          {/each}
        </div>
      {/if}
    </div>
  </div>

  <!-- Combined Field Form -->
  {#if showInputForm}
    <CombinedFieldForm
      step={config.steps[currentStepIndex]}
      stepIndex={currentStepIndex}
      fieldTypes={getAvailableFieldTypes(currentStepIndex)}
      formType={currentFormType}
      visible={showInputForm}
      on:close={closeInputForm}
      on:addInputField={() => formAddField('input')}
      on:addOutputField={() => formAddField('output')}
      on:addField={(e) => formAddField(e.detail.type)}
      on:removeField={(e) => formRemoveField(e.detail.type, e.detail.index)}
      on:updateField={(e) => formUpdateField(e.detail.type, e.detail.index, e.detail.property, e.detail.value)}
      on:typeChange={(e) => updateStepTypeProperty(e.detail.property, e.detail.value)}
    />
  {/if}
  
  <!-- Confirmation Dialog -->
  <ConfirmationDialog
    visible={showConfirmationDialog}
    title={confirmationTitle}
    message={confirmationMessage}
    warning={confirmationWarning}
    confirmText="Yes, Convert"
    cancelText="Cancel"
    on:confirm={handleConfirmationConfirm}
    on:cancel={handleConfirmationCancel}
  />
</main>