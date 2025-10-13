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
            {text: 'Versions', link: '/versions'}
        ],

        sidebar: [
            {
                text: 'Introduction',
                items: [
                    {text: 'What is The Pipeline Framework?', link: '/'},
                    {text: 'Getting Started', link: '/guide/getting-started'},
                    {text: 'Creating Pipeline Steps', link: '/guide/creating-steps'},
                ]
            },
            {
                text: 'Guides',
                items: [
                    {text: 'Overview', link: '/guide/'},
                    {text: 'Application Structure', link: '/guide/application-structure'},
                    {text: 'Backend Services', link: '/guide/backend-services'},
                    {text: 'Orchestrator Services', link: '/guide/orchestrator-services'},
                    {text: 'Pipeline Compilation', link: '/guide/pipeline-compilation'},
                    {text: 'Error Handling & DLQ', link: '/guide/error-handling'},
                    {text: 'Observability', link: '/guide/observability'},
                ]
            },
            {
                text: 'Reference',
                items: [
                    {text: 'Architecture', link: '/reference/architecture'},
                    {text: 'Framework Overview', link: '/FRAMEWORK_OVERVIEW'},
                    {text: 'Reference Implementation', link: '/REFERENCE_IMPLEMENTATION'},
                    {text: 'YAML Schema', link: '/YAML_SCHEMA'},
                    {text: 'Canvas Guide', link: '/CANVAS_GUIDE'},
                    {text: 'Java-Centered Types', link: '/JAVA_CENTERED_TYPES'},
                ]
            },
            {
                text: 'Advanced Topics',
                items: [
                    {text: 'Application Structure Overview', link: '/guide/app-structure-overview'},
                    {text: 'Common Module Structure', link: '/guide/common-module-structure'},
                    {text: 'Backend Service Structure', link: '/guide/backend-service-structure'},
                    {text: 'Orchestrator Service Structure', link: '/guide/orchestrator-service-structure'},
                    {text: 'Dependency Management', link: '/guide/dependency-management'},
                    {text: 'Configuration', link: '/guide/configuration'},
                    {text: 'Best Practices', link: '/guide/best-practices'},
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