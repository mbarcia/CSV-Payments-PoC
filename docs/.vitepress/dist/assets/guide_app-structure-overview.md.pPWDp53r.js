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

import{_ as p,C as i,c as t,o as e,a2 as r,b as l,w as n,a as o,G as c,a3 as m}from"./chunks/framework.BoyqnxIq.js";const f=JSON.parse('{"title":"Application Structure Overview","description":"","frontmatter":{},"headers":[],"relativePath":"guide/app-structure-overview.md","filePath":"guide/app-structure-overview.md"}'),d={name:"guide/app-structure-overview.md"};function u(v,a,h,w,g,A){const s=i("Mermaid");return e(),t("div",null,[a[1]||(a[1]=r(`<h1 id="application-structure-overview" tabindex="-1">Application Structure Overview <a class="header-anchor" href="#application-structure-overview" aria-label="Permalink to &quot;Application Structure Overview&quot;">​</a></h1><p>This guide explains how to structure applications using the Pipeline Framework, following the patterns demonstrated in the CSV Payments reference implementation.</p><h2 id="overview" tabindex="-1">Overview <a class="header-anchor" href="#overview" aria-label="Permalink to &quot;Overview&quot;">​</a></h2><p>Applications built with the Pipeline Framework follow a modular architecture with clear separation of concerns. The framework promotes a clean division between:</p><ol><li><strong>Orchestrator Service</strong>: Coordinates the overall pipeline execution</li><li><strong>Backend Services</strong>: Implement individual pipeline steps</li><li><strong>Common Module</strong>: Shared domain objects, DTOs, and mappers</li><li><strong>Framework</strong>: Provides the pipeline infrastructure</li></ol><h2 id="project-structure-overview" tabindex="-1">Project Structure Overview <a class="header-anchor" href="#project-structure-overview" aria-label="Permalink to &quot;Project Structure Overview&quot;">​</a></h2><p>A typical pipeline application follows this structure. Note that the deployment module is not typically included as a module in the application&#39;s parent POM since it&#39;s used at build time with provided scope:</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>my-pipeline-application/</span></span>
<span class="line"><span>├── pom.xml                           # Parent POM</span></span>
<span class="line"><span>├── common/                           # Shared components</span></span>
<span class="line"><span>│   ├── pom.xml</span></span>
<span class="line"><span>│   └── src/</span></span>
<span class="line"><span>│       └── main/java/</span></span>
<span class="line"><span>│           └── com/example/app/common/</span></span>
<span class="line"><span>│               ├── domain/           # Domain entities</span></span>
<span class="line"><span>│               ├── dto/              # Data transfer objects</span></span>
<span class="line"><span>│               └── mapper/           # Shared mappers</span></span>
<span class="line"><span>├── orchestrator-svc/                 # Pipeline orchestrator</span></span>
<span class="line"><span>│   ├── pom.xml</span></span>
<span class="line"><span>│   └── src/</span></span>
<span class="line"><span>│       └── main/java/</span></span>
<span class="line"><span>│           └── com/example/app/orchestrator/</span></span>
<span class="line"><span>│               ├── service/         # Pipeline execution service</span></span>
<span class="line"><span>│               └── CsvPaymentsApplication.java</span></span>
<span class="line"><span>├── step-one-svc/                     # First pipeline step</span></span>
<span class="line"><span>│   ├── pom.xml</span></span>
<span class="line"><span>│   └── src/</span></span>
<span class="line"><span>│       └── main/java/</span></span>
<span class="line"><span>│           └── com/example/app/stepone/</span></span>
<span class="line"><span>│               ├── service/         # Step implementation</span></span>
<span class="line"><span>│               └── mapper/          # Step-specific mappers</span></span>
<span class="line"><span>├── step-two-svc/                     # Second pipeline step</span></span>
<span class="line"><span>│   ├── pom.xml</span></span>
<span class="line"><span>│   └── src/</span></span>
<span class="line"><span>│       └── main/java/</span></span>
<span class="line"><span>│           └── com/example/app/steptwo/</span></span>
<span class="line"><span>│               ├── service/        # Step implementation</span></span>
<span class="line"><span>│               └── mapper/          # Step-specific mappers</span></span>
<span class="line"><span>└── pipeline-framework/              # Framework modules</span></span>
<span class="line"><span>    ├── runtime/                     # Runtime components (dependency)</span></span>
<span class="line"><span>    └── deployment/                  # Build-time components (provided scope)</span></span></code></pre></div><h2 id="architecture-diagram" tabindex="-1">Architecture Diagram <a class="header-anchor" href="#architecture-diagram" aria-label="Permalink to &quot;Architecture Diagram&quot;">​</a></h2>`,9)),(e(),l(m,null,{default:n(()=>[c(s,{id:"mermaid-44",class:"mermaid",graph:"graph%20TB%0A%20%20%20%20subgraph%20%22Pipeline%20Application%22%0A%20%20%20%20%20%20%20%20A%5BCommon%20Module%5D%0A%20%20%20%20%20%20%20%20B%5BOrchestrator%20Service%5D%0A%20%20%20%20%20%20%20%20C%5BStep%201%20Service%5D%0A%20%20%20%20%20%20%20%20D%5BStep%202%20Service%5D%0A%20%20%20%20end%0A%20%20%20%20%0A%20%20%20%20A%20--%3E%20B%0A%20%20%20%20A%20--%3E%20C%0A%20%20%20%20A%20--%3E%20D%0A%20%20%20%20B%20--%3E%20C%0A%20%20%20%20C%20--%3E%20D"})]),fallback:n(()=>[...a[0]||(a[0]=[o(" Loading... ",-1)])]),_:1}))])}const x=p(d,[["render",u]]);export{f as __pageData,x as default};
