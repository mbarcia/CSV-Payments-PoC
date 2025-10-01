<script>
  import { onMount, createEventDispatcher } from 'svelte';
  
  const dispatch = createEventDispatcher();

  export let visible = false;
  export let title = 'Confirm Action';
  export let message = '';
  export let confirmText = 'Confirm';
  export let cancelText = 'Cancel';
  export let warning = false;

  let container;

  // Close modal when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      if (container && !container.contains(event.target) && visible) {
        dispatch('cancel');
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape' && visible) {
        dispatch('cancel');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  });
  
  // Handle confirm action
  function handleConfirm() {
    dispatch('confirm');
  }
  
  // Handle cancel action
  function handleCancel() {
    dispatch('cancel');
  }
</script>

{#if visible}
  <div 
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    on:click={handleCancel}
  >
    <div 
      class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md" 
      bind:this={container}
      on:click|stopPropagation
    >
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold">{title}</h3>
        <button 
          on:click={handleCancel}
          class="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      <div class={warning ? 'mb-6 p-4 bg-yellow-50 border-l-4 border-yellow-400' : 'mb-6'}>
        <p class={warning ? 'text-yellow-700' : 'text-gray-700'}>{message}</p>
      </div>
      
      <div class="flex justify-end space-x-2">
        <button 
          on:click={handleCancel}
          class="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-100"
        >
          {cancelText}
        </button>
        <button 
          on:click={handleConfirm}
          class={warning ? 'px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700' : 'px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700'}
        >
          {confirmText}
        </button>
      </div>
    </div>
  </div>
{/if}