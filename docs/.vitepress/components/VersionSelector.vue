<!--
  - Copyright © 2023-2025 Mariano Barcia
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
  <div class="version-selector">
    <label for="version-select">Documentation Version:</label>
    <select id="version-select" @change="changeVersion" :value="currentVersion">
      <option value="/">v0.9.0 (current)</option>
      <option value="/v0.8.0/">v0.8.0</option>
      <option value="/v0.7.0/">v0.7.0</option>
    </select>
  </div>
</template>

<script>
export default {
  name: 'VersionSelector',
  data() {
    return {
      currentVersion: typeof window !== 'undefined' ? window.location.pathname : '/'
    }
  },
  methods: {
    changeVersion(event) {
      window.location.href = event.target.value
    }
  }
}
</script>

<style scoped>
.version-selector {
  margin: 1rem 0;
  padding: 1rem;
  background-color: var(--vp-c-bg-soft);
  border-radius: 8px;
}

.version-selector label {
  font-weight: bold;
  margin-right: 1rem;
}

.version-selector select {
  padding: 0.5rem;
  border-radius: 4px;
  border: 1px solid var(--vp-c-divider);
  background-color: var(--vp-c-bg);
}
</style>