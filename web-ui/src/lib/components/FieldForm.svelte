<script>
  import { onMount } from 'svelte';
  import GenericTypeConfigPopup from './GenericTypeConfigPopup.svelte';

  export let fields = [];
  // Default to Java-centered types if no fieldTypes provided
  export let fieldTypes = [
    'String', 'Integer', 'Long', 'Double', 'Boolean', 
    'UUID', 'BigDecimal', 'Currency', 'Path',
    'List', 'Map', 'LocalDateTime', 'LocalDate', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period',
    'URI', 'URL', 'File', 'BigInteger', 'AtomicInteger', 'AtomicLong', 'Enum'
  ];
  export let type = 'input';
  export let title = '';
  export let visible = false;
  export let onClose = () => {};
  export let onAddField = () => {};
  export let onRemoveField = () => {};
  export let onUpdateField = () => {};

  // State for generic type configuration popup
  let showGenericConfig = false;
  let currentFieldIndex = -1;
  let currentGenericType = 'List';

  // Filter out List and Map from the types shown in the generic type popup
  $: genericFieldTypes = fieldTypes.filter(t => t !== 'List' && t !== 'Map');

  let container;

  // Close modal when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      if (container && !container.contains(event.target) && visible && !showGenericConfig) {
        onClose?.();
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape' && visible && !showGenericConfig) {
        onClose?.();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  });

  // Function to handle type change, including generic types
  function handleTypeChange(fieldIndex, newType) {
    if (newType === 'List' || newType === 'Map') {
      // Store context for the generic type popup
      currentFieldIndex = fieldIndex;
      currentGenericType = newType;
      
      // Show the generic type configuration popup
      showGenericConfig = true;
      
      // Prevent the field type from being updated to just 'List' or 'Map'
      // We'll update it with the full generic type (e.g., 'List<String>') after confirmation
      return false;
    } else {
      // For non-generic types, update directly
      onUpdateField?.(fieldIndex, 'type', newType);
    }
  }
</script>

{#if visible}
  <div 
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    on:click={() => !showGenericConfig && onClose?.()}
  >
    <div 
      class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md" 
      bind:this={container}
      role="dialog"
      aria-modal="true"
      aria-labelledby="fieldFormDialogTitle"
      on:click|stopPropagation={() => {}}
    >
      <div class="flex justify-between items-center mb-4">
        <h3 id="fieldFormDialogTitle" class="text-lg font-semibold">{title}</h3>
        <button 
          on:click={() => onClose?.()}
          class="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      <div class="mb-4">
        <div class="flex justify-between items-center mb-2">
          <h4 class="font-medium">Fields</h4>
          <button 
            on:click={() => onAddField?.()}
            class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
          >
            Add Field
          </button>
        </div>
        
        {#if fields?.length === 0}
          <p class="text-sm text-gray-500 italic mb-4">No fields defined</p>
        {:else}
          <div class="space-y-3 max-h-60 overflow-y-auto">
            {#each fields as field, fieldIndex}
              <div class="flex items-center gap-2">
                <input 
                  type="text" 
                  bind:value={field?.name}
                  on:input={() => onUpdateField?.(fieldIndex, 'name', field?.name)}
                  class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Field name"
                />
                <select 
                  bind:value={field?.type}
                  on:change={(e) => handleTypeChange(fieldIndex, e.target.value)}
                  class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {#each fieldTypes as fieldType}
                    <option value={fieldType}>{fieldType}</option>
                  {/each}
                  <!-- Add dynamic generic types as options, if they exist -->
                  {#if field?.type && !fieldTypes.includes(field?.type) && (field?.type.startsWith('List<') || field?.type.startsWith('Map<'))}
                    <option value={field?.type} selected>{field?.type}</option>
                  {/if}
                </select>
                <button 
                  on:click={() => onRemoveField?.(fieldIndex)}
                  class="px-2 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                >
                  Remove
                </button>
              </div>
            {/each}
          </div>
        {/if}
      </div>
      
      <div class="flex justify-end">
        <button 
          on:click={() => onClose?.()}
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Done
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Generic Type Configuration Popup -->
{#if showGenericConfig}
  <GenericTypeConfigPopup
    visible={showGenericConfig}
    genericType={currentGenericType}
    fieldTypes={genericFieldTypes}
    selectedField={fields?.[currentFieldIndex]}
    on:close={() => showGenericConfig = false}
    on:confirm={(e) => {
      const selectedType = e.detail.type;
      onUpdateField?.(currentFieldIndex, 'type', selectedType);
      showGenericConfig = false;
    }}
  />
{/if}