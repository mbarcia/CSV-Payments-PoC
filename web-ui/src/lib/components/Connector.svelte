<!--
  - Copyright (c) 2023-2025 Mariano Barcia
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script>
  import {createEventDispatcher} from 'svelte';

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

<div class="flex justify-center items-center mx-4 my-2">
  <!-- Horizontal connector in landscape/desktop mode -->
  <div class="md:block hidden">
    <svg width="40" height="40" class="cursor-pointer" role="button" aria-label="Connect to next step" tabindex="0" on:click={handleConnectorClick} on:keydown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleConnectorClick(e);
      }
    }}>
      <line x1="0" y1="20" x2="40" y2="20" stroke="#9CA3AF" stroke-width="2" stroke-dasharray="4" />
      <polygon points="35,15 40,20 35,25" fill="#9CA3AF" />
    </svg>
  </div>
  
  <!-- Vertical connector in portrait/mobile mode -->
  <div class="block md:hidden">
    <svg width="40" height="40" class="cursor-pointer" role="button" aria-label="Connect to next step" tabindex="0" on:click={handleConnectorClick} on:keydown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleConnectorClick(e);
      }
    }}>
      <line x1="20" y1="0" x2="20" y2="40" stroke="#9CA3AF" stroke-width="2" stroke-dasharray="4" />
      <polygon points="15,35 20,40 25,35" fill="#9CA3AF" />
    </svg>
  </div>
</div>