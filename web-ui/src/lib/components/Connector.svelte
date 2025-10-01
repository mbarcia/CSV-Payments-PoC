<script>
  import { createEventDispatcher } from 'svelte';
  
  export let stepIndex;
  export let sharedType;
  
  const dispatch = createEventDispatcher();
  
  // Function to handle clicks on the connector
  function handleConnectorClick(event) {
    event.preventDefault();
    event.stopPropagation();
    
    // Pass the shared type to the handler
    dispatch('clickConnector', { stepIndex, side: 'shared', sharedType });
  }
</script>

<div class="flex justify-center items-center mx-4">
  <svg width="40" height="40" class="cursor-pointer" role="button" tabindex="0" on:click={handleConnectorClick} on:keydown={(e) => e.key === 'Enter' && handleConnectorClick(e)}>
    <line x1="0" y1="20" x2="40" y2="20" stroke="#9CA3AF" stroke-width="2" stroke-dasharray="4" />
    <polygon points="35,15 40,20 35,25" fill="#9CA3AF" />
    <circle cx="20" cy="20" r="6" fill="#9CA3AF" class="opacity-0" /> <!-- Invisible clickable area -->
  </svg>
</div>