/*
 * Copyright © 2023-2025 Mariano Barcia
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

export default defineConfig({
  title: 'Pipeline Framework',
  description: 'A framework for building reactive pipeline processing systems',
  
  // Disable dead links check since we're only documenting the pipeline framework
  ignoreDeadLinks: true,
  
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
      { text: 'Annotations', link: '/annotations/pipeline-step' }
    ],
    
    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Introduction', link: '/guide/' },
          { text: 'Getting Started', link: '/guide/getting-started' },
          { text: 'Creating Pipeline Steps', link: '/guide/creating-steps' }
        ]
      },
      {
        text: 'Annotations',
        items: [
          { text: 'PipelineStep and MapperForStep', link: '/annotations/pipeline-step' }
        ]
      }
    ],
    
    socialLinks: [
      { icon: 'github', link: 'https://github.com/mbarcia/CSV-Payments-PoC' }
    ]
  }
})