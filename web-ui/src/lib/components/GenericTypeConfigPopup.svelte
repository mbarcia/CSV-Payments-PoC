<script>
  import { onMount, createEventDispatcher } from 'svelte';
  
  const dispatch = createEventDispatcher();

  export let visible = false;
  export let genericType = 'List'; // 'List' or 'Map'
  export let fieldTypes = [];
  export let currentStepIndex = -1;
  export let selectedField = null;

  let keyType = 'String';
  let valueType = 'String';
  let listType = 'String';

  // Keep defaults in sync with available options when modal opens or props change
  $: if (visible && fieldTypes?.length) {
    const first = fieldTypes[0];
    if (!fieldTypes.includes(listType)) listType = first;
    if (!fieldTypes.includes(keyType)) keyType = first;
    if (!fieldTypes.includes(valueType)) valueType = first;
  }

  // If editing an existing generic field, prefill from its type (e.g., "List<String>", "Map<String, Integer>")
  $: if (visible && selectedField?.type) {
    if (genericType === 'List') {
      const m = selectedField.type.match(/^List<([^>]+)>$/);
      if (m?.[1] && fieldTypes.includes(m[1])) listType = m[1];
    } else if (genericType === 'Map') {
      const m = selectedField.type.match(/^Map<([^,>]+),\s*([^>]+)>$/);
      if (m?.[1] && fieldTypes.includes(m[1])) keyType = m[1];
      if (m?.[2] && fieldTypes.includes(m[2])) valueType = m[2];
    }
  }

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

  // Function to handle confirm
  function handleConfirm() {
    let resultType;
    if (genericType === 'List') {
      resultType = `List<${listType}>`;
    } else if (genericType === 'Map') {
      resultType = `Map<${keyType}, ${valueType}>`;
    }
    
    dispatch('confirm', { type: resultType });
  }
</script>

{#if visible}
  <div 
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    on:click={() => dispatch('close')}
  >
    <div 
      class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md" 
      bind:this={container}
      role="dialog"
      aria-modal="true"
      aria-labelledby="genericTypeDialogTitle"
      on:click|stopPropagation={() => {}}
    >
      <div class="flex justify-between items-center mb-4">
        <h3 id="genericTypeDialogTitle" class="text-lg font-semibold">Configure {genericType} Type</h3>
        <button 
          on:click={() => dispatch('close')}
          class="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      {#if genericType === 'List'}
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">List Inner Type</label>
          <select 
            bind:value={listType}
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {#each fieldTypes as fieldType}
              <option value={fieldType}>{fieldType}</option>
            {/each}
          </select>
        </div>
      {:else if genericType === 'Map'}
        <div class="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Key Type</label>
            <select 
              bind:value={keyType}
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {#each fieldTypes as fieldType}
                <option value={fieldType}>{fieldType}</option>
              {/each}
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Value Type</label>
            <select 
              bind:value={valueType}
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {#each fieldTypes as fieldType}
                <option value={fieldType}>{fieldType}</option>
              {/each}
            </select>
          </div>
        </div>
      {/if}
      
      <div class="flex justify-end space-x-2">
        <button 
          on:click={() => dispatch('close')}
          class="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-100"
        >
          Cancel
        </button>
        <button 
          on:click={handleConfirm}
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Confirm
        </button>
      </div>
    </div>
  </div>
{/if}