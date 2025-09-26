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
  <div class="hero-section">
    <div class="hero-content">
      <h1 class="hero-title rounded">{{ title }}</h1>
      <p class="hero-subtitle">{{ subtitle }}</p>
      <div class="hero-cta">
        <a :href="primaryCta.href" class="button primary">{{ primaryCta.text }}</a>
        <a v-if="secondaryCta" :href="secondaryCta.href" class="button secondary">{{ secondaryCta.text }}</a>
      </div>
    </div>
    <div class="hero-features">
      <div v-for="(feature, index) in features" :key="index" class="feature-card">
        <h3>{{ feature.title }}</h3>
        <p>{{ feature.description }}</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HeroSection',
  props: {
    title: {
      type: String,
      default: 'The Pipeline Framework'
    },
    subtitle: {
      type: String,
      default: 'Build reactive, scalable pipeline applications'
    },
    primaryCta: {
      type: Object,
      default: () => ({ text: 'Get Started', href: '/guide/getting-started' })
    },
    secondaryCta: {
      type: Object,
      default: null
    },
    features: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.hero-section {
  padding: 4rem 1rem 2rem;
  text-align: center;
  background: linear-gradient(135deg, var(--vp-c-bg-soft) 0%, var(--vp-c-bg) 100%);
  border-bottom: 1px solid var(--vp-c-border);
}

.hero-content {
  max-width: 800px;
  margin: 0 auto 3rem;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 1rem;
  color: var(--vp-c-brand);
  font-family: var(--vp-font-family-display);
}

.hero-title.rounded {
  display: inline-block;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  background: linear-gradient(120deg, var(--vp-c-brand) 30%, var(--vp-c-brand-light));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.5rem;
  color: var(--vp-c-text-2);
  margin-bottom: 2rem;
  line-height: 1.5;
}

.hero-cta {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.button {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.25s ease;
  border: 1px solid;
  text-align: center;
  margin: 0.5rem 0;
  font-size: 1rem;
}

.button.primary {
  background-color: var(--vp-c-brand);
  border-color: var(--vp-c-brand);
  color: white;
}

.button.primary:hover {
  background-color: var(--vp-c-brand-dark);
  border-color: var(--vp-c-brand-dark);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.button.secondary {
  background-color: transparent;
  border-color: var(--vp-c-brand);
  color: var(--vp-c-brand);
}

.button.secondary:hover {
  background-color: rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}

.hero-features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.feature-card {
  background: var(--vp-c-bg);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: 1px solid var(--vp-c-border);
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.dark .feature-card {
  background: var(--vp-c-bg-soft);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.feature-card h3 {
  color: var(--vp-c-brand);
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
}

.feature-card p {
  margin: 0;
  color: var(--vp-c-text-2);
  line-height: 1.5;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
  
  .hero-subtitle {
    font-size: 1.25rem;
  }
  
  .hero-cta {
    flex-direction: column;
    align-items: center;
  }
  
  .button {
    width: 100%;
    max-width: 300px;
  }
}
</style>