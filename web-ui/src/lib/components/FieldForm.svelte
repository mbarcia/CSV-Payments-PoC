<script>
  import { onMount } from 'svelte';

  export let fields;
  // Default to Java-centered types if no fieldTypes provided
  export let fieldTypes = [
    'String', 'Integer', 'Long', 'Double', 'Boolean', 
    'UUID', 'BigDecimal', 'Currency', 'Path',
    'List<String>', 'LocalDateTime', 'LocalDate', 'OffsetDateTime', 'ZonedDateTime', 'Instant', 'Duration', 'Period',
    'URI', 'URL', 'File', 'BigInteger', 'AtomicInteger', 'AtomicLong', 'Enum'
  ];
  export let type = 'input';
  export let title = '';
  export let visible = false;
  export let onClose;
  export let onAddField;
  export let onRemoveField;
  export let onUpdateField;

  let container;

  // Close modal when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      if (container && !container.contains(event.target) && visible) {
        onClose();
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape' && visible) {
        onClose();
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
    on:click={onClose}
  >
    <div 
      class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md" 
      bind:this={container}
      on:click|stopPropagation
    >
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold">{title}</h3>
        <button 
          on:click={onClose}
          class="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      <div class="mb-4">
        <div class="flex justify-between items-center mb-2">
          <h4 class="font-medium">Fields</h4>
          <button 
            on:click={onAddField}
            class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
          >
            Add Field
          </button>
        </div>
        
        {#if fields.length === 0}
          <p class="text-sm text-gray-500 italic mb-4">No fields defined</p>
        {:else}
          <div class="space-y-3 max-h-60 overflow-y-auto">
            {#each fields as field, fieldIndex}
              <div class="flex items-center gap-2">
                <input 
                  type="text" 
                  bind:value={field.name}
                  on:input={() => onUpdateField(fieldIndex, 'name', field.name)}
                  class="flex-1 px-3 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Field name"
                />
                <select 
                  bind:value={field.type}
                  on:change={() => onUpdateField(fieldIndex, 'type', field.type)}
                  class="px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {#each fieldTypes as fieldType}
                    <option value={fieldType}>{fieldType}</option>
                  {/each}
                </select>
                <button 
                  on:click={() => onRemoveField(fieldIndex)}
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
          on:click={onClose}
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Done
        </button>
      </div>
    </div>
  </div>
{/if}