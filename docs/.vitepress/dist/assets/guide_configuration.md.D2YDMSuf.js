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

import{_ as n,C as t,c as p,o as s,a2 as r,b as l,w as a,a as o,G as h,a3 as c}from"./chunks/framework.BoyqnxIq.js";const y=JSON.parse('{"title":"Configuration","description":"","frontmatter":{},"headers":[],"relativePath":"guide/configuration.md","filePath":"guide/configuration.md"}'),k={name:"guide/configuration.md"};function d(u,i,g,f,E,m){const e=t("Mermaid");return s(),p("div",null,[i[1]||(i[1]=r(`<h1 id="configuration" tabindex="-1">Configuration <a class="header-anchor" href="#configuration" aria-label="Permalink to &quot;Configuration&quot;">​</a></h1><p>Configure pipeline behavior through application properties and environment-specific profiles.</p><h2 id="application-properties" tabindex="-1">Application Properties <a class="header-anchor" href="#application-properties" aria-label="Permalink to &quot;Application Properties&quot;">​</a></h2><p>Configure pipeline behavior through application properties:</p><div class="language-properties vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">properties</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;"># application.properties</span></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;"># Pipeline configuration</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.retry-limit</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=3</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.debug</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=false</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.auto-persist</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=true</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;"># gRPC clients</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">quarkus.grpc.clients.process-payment.host</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=localhost</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">quarkus.grpc.clients.process-payment.port</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=8080</span></span></code></pre></div><h2 id="environment-specific-profiles" tabindex="-1">Environment-Specific Profiles <a class="header-anchor" href="#environment-specific-profiles" aria-label="Permalink to &quot;Environment-Specific Profiles&quot;">​</a></h2><p>Use Quarkus profiles for different environments:</p><div class="language-properties vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">properties</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;"># application-dev.properties</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.debug</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=true</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.retry-limit</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=1</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;"># application-prod.properties</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.retry-limit</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=5</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">pipeline.retry-wait</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">=1S</span></span></code></pre></div><h2 id="configuration-architecture" tabindex="-1">Configuration Architecture <a class="header-anchor" href="#configuration-architecture" aria-label="Permalink to &quot;Configuration Architecture&quot;">​</a></h2>`,9)),(s(),l(c,null,{default:a(()=>[h(e,{id:"mermaid-23",class:"mermaid",graph:"graph%20TD%0A%20%20%20%20A%5BApplication%20Properties%5D%20--%3E%20B%5BQuarkus%20Runtime%5D%0A%20%20%20%20A%20--%3E%20C%5BEnvironment%20Profiles%5D%0A%20%20%20%20C%20--%3E%20D%5BDev%20Configuration%5D%0A%20%20%20%20C%20--%3E%20E%5BTest%20Configuration%5D%0A%20%20%20%20C%20--%3E%20F%5BProd%20Configuration%5D%0A%20%20%20%20B%20--%3E%20G%5BPipeline%20Execution%5D%0A"})]),fallback:a(()=>[...i[0]||(i[0]=[o(" Loading... ",-1)])]),_:1}))])}const D=n(k,[["render",d]]);export{y as __pageData,D as default};
