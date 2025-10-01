<script>
  import { onMount, createEventDispatcher } from 'svelte';
  
  const dispatch = createEventDispatcher();

  export let step;
  export let stepIndex;
    // Field types for dropdowns (using Java types with protobuf mappings)
  export let fieldTypes = [
    'String', 'Integer', 'Long', 'Double', 'Boolean', 
    'UUID', 'BigDecimal', 'Currency', 'Path',
    'List<String>', 'LocalDateTime', 'LocalDate', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period',
    'URI', 'URL', 'File', 'BigInteger', 'AtomicInteger', 'AtomicLong', 'Enum'
  ];
  
  // Function to get all available types including custom message types from previous steps
  function getAvailableFieldTypes(currentStepIndex, config) {
    // Start with basic scalar types and Enum
    let allTypes = [...fieldTypes];
    
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
  export let formType = 'both'; // 'both', 'input', 'output', 'shared'
  export let visible = false;

  let container;

  // Close modal when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      if (container && !container.contains(event.target) && visible) {
        dispatch('close');
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape' && visible) {
        dispatch('close');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  });
</script>

{#if visible}
  <div 
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    on:click={() => dispatch('close')}
  >
    <div 
      class="bg-white rounded-lg shadow-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-auto" 
      bind:this={container}
      on:click|stopPropagation
    >
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold">
          {formType === 'shared' ? 'Shared Type Configuration' : 
           formType === 'both' ? `Step ${stepIndex + 1}: ${step?.name} Fields` : 
           `${formType === 'input' ? 'Input' : 'Output'} Fields - ${step?.name}`}
        </h3>
        <button 
          on:click={() => dispatch('close')}
          class="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      {#if formType === 'both'}
        <!-- Two-column layout for Input and Output Types -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <!-- Input Type Column -->
          <div>
            <h4 class="font-medium mb-2">Input Type</h4>
            <div class="flex items-center mb-3">
              <label class="block text-sm font-medium text-gray-700 mr-2 w-32">Type Name:</label>
              <input 
                type="text" 
                bind:value={step.inputTypeName}
                on:input={() => dispatch('typeChange', { property: 'inputTypeName', value: step.inputTypeName })}
                class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            
            <div class="flex justify-between items-center mb-2">
              <h5 class="font-medium">Input Fields</h5>
              <button 
                on:click={() => dispatch('addInputField')}
                class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
              >
                Add Field
              </button>
            </div>
            
            {#if step.inputFields.length === 0}
              <p class="text-sm text-gray-500 italic mb-4">No input fields defined</p>
            {:else}
              <div class="space-y-2 max-h-60 overflow-y-auto">
                {#each step.inputFields as field, fieldIndex}
                  <div class="flex items-center gap-2">
                    <input 
                      type="text" 
                      bind:value={field.name}
                      on:input={() => dispatch('updateField', { type: 'input', index: fieldIndex, property: 'name', value: field.name })}
                      class="flex-1 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                      placeholder="Field name"
                    />
                    <select 
                      bind:value={field.type}
                      on:change={() => dispatch('updateField', { type: 'input', index: fieldIndex, property: 'type', value: field.type })}
                      class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                    >
                      {#each fieldTypes as fieldType}
                        <option value={fieldType}>{fieldType}</option>
                      {/each}
                    </select>
                    <button 
                      on:click={() => dispatch('removeField', { type: 'input', index: fieldIndex })}
                      class="px-2 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                    >
                      Remove
                    </button>
                  </div>
                {/each}
              </div>
            {/if}
          </div>
          
          <!-- Output Type Column -->
          <div>
            <h4 class="font-medium mb-2">Output Type</h4>
            <div class="flex items-center mb-3">
              <label class="block text-sm font-medium text-gray-700 mr-2 w-32">Type Name:</label>
              <input 
                type="text" 
                bind:value={step.outputTypeName}
                on:input={() => dispatch('typeChange', { property: 'outputTypeName', value: step.outputTypeName })}
                class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            
            <div class="flex justify-between items-center mb-2">
              <h5 class="font-medium">Output Fields</h5>
              <button 
                on:click={() => dispatch('addOutputField')}
                class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
              >
                Add Field
              </button>
            </div>
            
            {#if step.outputFields.length === 0}
              <p class="text-sm text-gray-500 italic mb-4">No output fields defined</p>
            {:else}
              <div class="space-y-2 max-h-60 overflow-y-auto">
                {#each step.outputFields as field, fieldIndex}
                  <div class="flex items-center gap-2">
                    <input 
                      type="text" 
                      bind:value={field.name}
                      on:input={() => dispatch('updateField', { type: 'output', index: fieldIndex, property: 'name', value: field.name })}
                      class="flex-1 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                      placeholder="Field name"
                    />
                    <select 
                      bind:value={field.type}
                      on:change={() => dispatch('updateField', { type: 'output', index: fieldIndex, property: 'type', value: field.type })}
                      class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                    >
                      {#each fieldTypes as fieldType}
                        <option value={fieldType}>{fieldType}</option>
                      {/each}
                    </select>
                    <button 
                      on:click={() => dispatch('removeField', { type: 'output', index: fieldIndex })}
                      class="px-2 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                    >
                      Remove
                    </button>
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        </div>
      {:else if formType === 'shared'}
        <!-- Shared Type Configuration -->
        <div class="mb-6">
          <h4 class="font-medium mb-2">Shared Type (Connects Previous Output to Current Input)</h4>
          <div class="flex items-center mb-3">
            <label class="block text-sm font-medium text-gray-700 mr-2 w-32">Type Name:</label>
            <input 
              type="text" 
              bind:value={step.inputTypeName}
              on:input={() => dispatch('typeChange', { property: 'inputTypeName', value: step.inputTypeName })}
              class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Shared type name"
            />
          </div>
          
          <div class="flex justify-between items-center mb-2">
            <h5 class="font-medium">Fields</h5>
            <button 
              on:click={() => dispatch('addInputField')}
              class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
            >
              Add Field
            </button>
          </div>
          
          {#if step.inputFields && step.inputFields.length > 0}
            <div class="space-y-2 max-h-60 overflow-y-auto">
              {#each step.inputFields as field, fieldIndex}
                <div class="flex items-center gap-2">
                  <input 
                    type="text" 
                    bind:value={field.name}
                    on:input={() => dispatch('updateField', { type: 'input', index: fieldIndex, property: 'name', value: field.name })}
                    class="flex-1 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                    placeholder="Field name"
                  />
                  <select 
                    bind:value={field.type}
                    on:change={() => dispatch('updateField', { type: 'input', index: fieldIndex, property: 'type', value: field.type })}
                    class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  >
                    {#each fieldTypes as fieldType}
                      <option value={fieldType}>{fieldType}</option>
                    {/each}
                  </select>
                  <button 
                    on:click={() => dispatch('removeField', { type: 'input', index: fieldIndex })}
                    class="px-2 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                  >
                    Remove
                  </button>
                </div>
              {/each}
            </div>
          {:else}
            <p class="text-sm text-gray-500 italic">No fields defined for this shared type</p>
          {/if}
        </div>
      {:else}
        <!-- Single type (input or output) -->
        <div class="mb-6">
          <h4 class="font-medium mb-2">{formType === 'input' ? 'Input' : 'Output'} Type</h4>
          <div class="flex items-center mb-3">
            {#if formType === 'input'}
            <label class="block text-sm font-medium text-gray-700 mr-2 w-32">Type Name:</label>
            <input 
              type="text" 
              bind:value={step.inputTypeName}
              on:input={() => dispatch('typeChange', { property: 'inputTypeName', value: step.inputTypeName })}
              class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {:else}
            <label class="block text-sm font-medium text-gray-700 mr-2 w-32">Type Name:</label>
            <input 
              type="text" 
              bind:value={step.outputTypeName}
              on:input={() => dispatch('typeChange', { property: 'outputTypeName', value: step.outputTypeName })}
              class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {/if}
          </div>
          
          <div class="flex justify-between items-center mb-2">
            <h5 class="font-medium">{formType === 'input' ? 'Input' : 'Output'} Fields</h5>
            <button 
              on:click={() => dispatch('addField', { type: formType })}
              class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
            >
              Add Field
            </button>
          </div>
          
          {#if (formType === 'input' ? step.inputFields : step.outputFields) && (formType === 'input' ? step.inputFields : step.outputFields).length === 0}
            <p class="text-sm text-gray-500 italic mb-4">No {formType} fields defined</p>
          {:else if (formType === 'input' ? step.inputFields : step.outputFields)}
            <div class="space-y-2 max-h-60 overflow-y-auto">
              {#each (formType === 'input' ? step.inputFields : step.outputFields) as field, fieldIndex}
                <div class="flex items-center gap-2">
                  <input 
                    type="text" 
                    bind:value={field.name}
                    on:input={() => dispatch('updateField', { type: formType, index: fieldIndex, property: 'name', value: field.name })}
                    class="flex-1 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                    placeholder="Field name"
                  />
                  <select 
                    bind:value={field.type}
                    on:change={() => dispatch('updateField', { type: formType, index: fieldIndex, property: 'type', value: field.type })}
                    class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  >
                    {#each fieldTypes as fieldType}
                      <option value={fieldType}>{fieldType}</option>
                    {/each}
                  </select>
                  <button 
                    on:click={() => dispatch('removeField', { type: formType, index: fieldIndex })}
                    class="px-2 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                  >
                    Remove
                  </button>
                </div>
              {/each}
            </div>
          {:else}
            <p class="text-sm text-gray-500 italic mb-4">No {formType} fields defined</p>
          {/if}
        </div>
      {/if}
      
      <div class="flex justify-end">
        <button 
          on:click={() => dispatch('close')}
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Done
        </button>
      </div>
    </div>
  </div>
{/if}