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

<template>
  <div class="callout" :class="[type]">
    <div class="callout-header">
      <span class="callout-icon" v-if="icon">{{ icon }}</span>
      <strong class="callout-title">{{ title || type.charAt(0).toUpperCase() + type.slice(1) }}</strong>
    </div>
    <div class="callout-content">
      <slot />
    </div>
  </div>
</template>

<script>
export default {
  name: 'Callout',
  props: {
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['info', 'tip', 'warning', 'danger'].includes(value)
    },
    title: {
      type: String,
      default: null
    },
    icon: {
      type: String,
      default: null
    }
  }
}
</script>

<style scoped>
.callout {
  border-radius: 8px;
  margin: 1.5rem 0;
  overflow: hidden;
  border-left: 4px solid;
}

.callout.info {
  border-color: var(--vp-c-brand);
  background-color: rgba(225, 34, 24, 0.05);
}

.callout.tip {
  border-color: #2e7d32;
  background-color: rgba(46, 125, 50, 0.05);
}

.callout.warning {
  border-color: #ff8f00;
  background-color: rgba(255, 143, 0, 0.05);
}

.callout.danger {
  border-color: #d32f2f;
  background-color: rgba(211, 47, 47, 0.05);
}

.callout-header {
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  border-bottom: 1px solid transparent;
}

.callout.info .callout-header {
  background-color: rgba(225, 34, 24, 0.15);
  color: var(--vp-c-brand);
}

.callout.tip .callout-header {
  background-color: rgba(46, 125, 50, 0.15);
  color: #2e7d32;
}

.callout.warning .callout-header {
  background-color: rgba(255, 143, 0, 0.15);
  color: #ff8f00;
}

.callout.danger .callout-header {
  background-color: rgba(211, 47, 47, 0.15);
  color: #d32f2f;
}

.callout-icon {
  margin-right: 0.5rem;
  font-size: 1.2em;
}

.callout-title {
  font-weight: 600;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.callout-content {
  padding: 1rem;
  line-height: 1.6;
}

.callout :deep(.vp-doc p) {
  margin: 0.5rem 0;
}

.callout :deep(.vp-doc pre),
.callout :deep(.vp-doc code) {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>