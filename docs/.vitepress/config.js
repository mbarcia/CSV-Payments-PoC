/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {defineConfig} from 'vitepress'
import {withMermaid} from "vitepress-plugin-mermaid"

// Use withMermaid to wrap the entire configuration - this enables GitHub-style mermaid code blocks
// Note: This adds significant size to the bundle due to Mermaid's dependencies
export default withMermaid(
  defineConfig({
    title: 'The Pipeline Framework',
    description: 'A framework for building reactive pipeline processing systems',
    
    // Disable dead links check since we're only documenting the pipeline framework
    ignoreDeadLinks: true,
    
    // Base URL for the site (can be changed for different deployments)
    base: '/',
    
    // Register custom theme
    themeConfig: {
        nav: [
            {text: 'Home', link: '/'},
            {text: 'Guide', link: '/guide/'},
            {text: 'Annotations', link: '/annotations/pipeline-step'},
            {text: 'Versions', link: '/versions'},
            {text: 'Dev Guidelines', link: '/ci-guidelines'}
        ],

        sidebar: [
            {
                text: 'Getting Started',
                items: [
                    {text: 'Guide Overview', link: '/guide/'},
                    {text: 'Quick Start', link: '/guide/quick-start'},
                    {text: 'Getting Started', link: '/guide/getting-started'},
                ]
            },
            {
                text: 'Essential Guides',
                items: [
                    {text: 'Creating Pipeline Steps', link: '/guide/creating-steps'},
                    {text: 'Application Structure', link: '/guide/application-structure'},
                    {text: 'Backend Services', link: '/guide/backend-services'},
                    {text: 'Mappers and DTOs', link: '/guide/mappers-and-dtos'},
                    {text: 'Orchestrator Services', link: '/guide/orchestrator-services'},
                ]
            },
            {
                text: 'Advanced Guides',
                items: [
                    {text: 'Pipeline Compilation', link: '/guide/pipeline-compilation'},
                    {text: 'Error Handling & DLQ', link: '/guide/error-handling'},
                    {text: 'Observability', link: '/guide/observability'},
                    {text: 'Configuration', link: '/guide/configuration'},
                    {text: 'Best Practices', link: '/guide/best-practices'},
                ]
            },
            {
                text: 'Reference',
                items: [
                    {text: 'Architecture', link: '/reference/architecture'},
                    {text: 'Reference Implementation', link: '/REFERENCE_IMPLEMENTATION'},
                    {text: 'YAML Schema', link: '/YAML_SCHEMA'},
                    {text: 'Canvas Guide', link: '/CANVAS_GUIDE'},
                    {text: 'Java-Centered Types', link: '/JAVA_CENTERED_TYPES'},
                ]
            },
            {
                text: 'API Reference',
                items: [
                    {text: '@PipelineStep Annotation', link: '/annotations/pipeline-step'},
                ]
            },
            {
                text: 'Additional Resources',
                items: [
                    {text: 'Common Module Structure', link: '/guide/common-module-structure'},
                    {text: 'Local Steps', link: '/guide/local-steps'},
                    {text: 'Handling File Operations', link: '/guide/handling-file-operations'},
                    {text: 'Using Template Generator', link: '/guide/using-template-generator'},
                    {text: 'Versions', link: '/versions'},
                ]
            },
            {
                text: 'Development Guidelines',
                items: [
                    {text: 'CI Guidelines', link: '/ci-guidelines'},
                    {text: 'Testing Guidelines', link: '/testing-guidelines'},
                    {text: 'Gotchas & Pitfalls', link: '/gotchas-pitfalls'},
                ]
            }
        ],

      // Add search functionality
      search: {
        provider: 'local'
      },
      
      socialLinks: [
        { icon: 'github', link: 'https://github.com/mbarcia/pipelineframework' }
      ]
    },
    
    vite: {
      optimizeDeps: { 
        include: ['@braintree/sanitize-url'] 
      },
      resolve: {
        alias: {
          dayjs: 'dayjs/',
        },
      },
      server: {
        fs: {
          allow: ['../..']
        }
      }
    },
    
    head: [
      // Add Google Fonts for Quarkus-like typography
      ['link', { rel: 'preconnect', href: 'https://fonts.googleapis.com' }],
      ['link', { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' }],
      ['link', { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Red+Hat+Display:wght@400;500;700;900&display=swap' }],
      ['link', { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Red+Hat+Text:wght@400;500&display=swap' }]
    ]
  })
)