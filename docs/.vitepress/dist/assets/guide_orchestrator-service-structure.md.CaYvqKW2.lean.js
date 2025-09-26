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

import{_ as t,C as e,c as p,o as i,a2 as l,b as h,w as a,a as k,G as r,a3 as E}from"./chunks/framework.BoyqnxIq.js";const v=JSON.parse('{"title":"Orchestrator Service Structure","description":"","frontmatter":{},"headers":[],"relativePath":"guide/orchestrator-service-structure.md","filePath":"guide/orchestrator-service-structure.md"}'),d={name:"guide/orchestrator-service-structure.md"};function c(o,s,g,y,F,u){const n=e("Mermaid");return i(),p("div",null,[s[1]||(s[1]=l("",9)),(i(),h(E,null,{default:a(()=>[r(n,{id:"mermaid-23",class:"mermaid",graph:"graph%20TD%0A%20%20%20%20A%5BInput%20Data%20Source%5D%20--%3E%20B%5BOrchestrator%20Service%5D%0A%20%20%20%20B%20--%3E%20C%5BPipeline%20Steps%5D%0A%20%20%20%20C%20--%3E%20D%5BOutput%5D%0A%20%20%20%20B%20--%3E%20E%5BError%20Handling%5D%0A%20%20%20%20E%20--%3E%20F%5BDLQ%5D%0A"})]),fallback:a(()=>[...s[0]||(s[0]=[k(" Loading... ",-1)])]),_:1}))])}const C=t(d,[["render",c]]);export{v as __pageData,C as default};
