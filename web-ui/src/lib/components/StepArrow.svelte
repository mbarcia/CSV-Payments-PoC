<script>
  import { createEventDispatcher } from 'svelte';
  
  export let step;
  export let index;
  export let isSelected = false;
  export let isAnimating = false;  // New prop to control animation
  
  const dispatch = createEventDispatcher();
  
  // Calculate the width based on the step name
  let width = Math.max(200, (step.name?.length || 10) * 10);
  
  // Calculate height based on content complexity
  let height = 120;
  if (step.inputFields?.length > 0 || step.outputFields?.length > 0) {
    height += Math.max(step.inputFields?.length || 0, step.outputFields?.length || 0) * 20;
  }
  
  // Handle click on the step - show combined form
  function handleStepClick(event) {
    event.preventDefault();
    event.stopPropagation(); // Prevent event bubbling
    
    // Always open combined form when clicking on step
    dispatch('clickStep', { index, side: 'both' });
  }
</script>

<div class="relative" style="width: {width}px; height: {height}px;">
  <svg 
    width={width} 
    height={height} 
    class="cursor-pointer" 
    role="button" 
    tabindex="0" 
    class:animate-pulse={isAnimating}
    on:click={handleStepClick} 
    on:keydown={(e) => e.key === 'Enter' && handleStepClick(e)}
  >
    {#if step.cardinality === 'ONE_TO_ONE' || step.cardinality === 'SIDE_EFFECT'}
      <!-- 1-1 arrow (normal arrow) -->
      <defs>
        <linearGradient id={`arrow-gradient-${index}`} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:{isSelected ? '#4F46E5' : '#4F46E5'};stop-opacity:1" />
          <stop offset="100%" style="stop-color:{isSelected ? '#6366F1' : '#818CF8'};stop-opacity:1" />
        </linearGradient>
      </defs>
      <polygon 
        points={`20,${height/2-30} ${width-40},${height/2-30} ${width-20},${height/2} ${width-40},${height/2+30} 20,${height/2+30}`} 
        fill={`url(#arrow-gradient-${index})`} 
        stroke={isSelected ? "#312E81" : "#1E3A8A"} 
        stroke-width="2"
        class="step-arrow"
        class:animate-pulse={isAnimating}
      />
    {:else if step.cardinality === 'EXPANSION'}
      <!-- Expansion arrow (1-many, wider at output) -->
      <defs>
        <linearGradient id={`arrow-gradient-${index}`} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:{isSelected ? '#10B981' : '#10B981'};stop-opacity:1" />
          <stop offset="100%" style="stop-color:{isSelected ? '#34D399' : '#6EE7B7'};stop-opacity:1" />
        </linearGradient>
      </defs>
      <polygon 
        points={`20,${height/2-30} ${width-60},${height/2-40} ${width-20},${height/2} ${width-60},${height/2+40} 20,${height/2+30}`} 
        fill={`url(#arrow-gradient-${index})`} 
        stroke={isSelected ? "#065F46" : "#047857"} 
        stroke-width="2"
        class="step-arrow"
        class:animate-pulse={isAnimating}
      />
    {:else if step.cardinality === 'REDUCTION'}
      <!-- Reduction arrow (many-1, wider at input) -->
      <defs>
        <linearGradient id={`arrow-gradient-${index}`} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:{isSelected ? '#EF4444' : '#EF4444'};stop-opacity:1" />
          <stop offset="100%" style="stop-color:{isSelected ? '#F87171' : '#FCA5A5'};stop-opacity:1" />
        </linearGradient>
      </defs>
      <polygon 
        points={`60,${height/2-40} ${width-20},${height/2-30} ${width-20},${height/2+30} 60,${height/2+40}`} 
        fill={`url(#arrow-gradient-${index})`} 
        stroke={isSelected ? "#7F1D1D" : "#DC2626"} 
        stroke-width="2"
        class="step-arrow"
        class:animate-pulse={isAnimating}
      />
      <polygon 
        points={`20,${height/2-15} 60,${height/2-40} 60,${height/2+40} 20,${height/2+15}`} 
        fill={`url(#arrow-gradient-${index})`} 
        stroke={isSelected ? "#7F1D1D" : "#DC2626"} 
        stroke-width="2"
        class:animate-pulse={isAnimating}
      />
    {/if}
    
    <!-- Step name text -->
    <text 
      x={width/2} 
      y={height/2+5} 
      text-anchor="middle" 
      fill="white" 
      font-size="14" 
      font-weight="bold"
      class="step-name"
      class:animate-pulse={isAnimating}
    >
      {step.name}
    </text>
  </svg>
</div>

<style>
  .animate-pulse {
    animation: pulse 0.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  }
  
  @keyframes pulse {
    0%, 100% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.05);
    }
  }
</style>