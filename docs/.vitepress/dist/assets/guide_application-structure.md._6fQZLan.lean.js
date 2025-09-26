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

import{_ as n,C as o,c,o as a,a2 as t,b as s,w as r,a as p,G as l,a3 as d}from"./chunks/framework.BoyqnxIq.js";const S=JSON.parse('{"title":"Application Structure","description":"","frontmatter":{},"headers":[],"relativePath":"guide/application-structure.md","filePath":"guide/application-structure.md"}'),u={name:"guide/application-structure.md"};function h(m,e,f,g,b,v){const i=o("Mermaid");return a(),c("div",null,[e[1]||(e[1]=t("",6)),(a(),s(d,null,{default:r(()=>[l(i,{id:"mermaid-37",class:"mermaid",graph:"graph%20TB%0A%20%20%20%20subgraph%20%22Pipeline%20Application%22%0A%20%20%20%20%20%20%20%20A%5BCommon%20Module%5D%0A%20%20%20%20%20%20%20%20B%5BOrchestrator%20Service%5D%0A%20%20%20%20%20%20%20%20C%5BStep%201%20Service%5D%0A%20%20%20%20%20%20%20%20D%5BStep%202%20Service%5D%0A%20%20%20%20end%0A%20%20%20%20%0A%20%20%20%20A%20--%3E%20B%0A%20%20%20%20A%20--%3E%20C%0A%20%20%20%20A%20--%3E%20D%0A%20%20%20%20B%20--%3E%20C%0A%20%20%20%20C%20--%3E%20D%0A"})]),fallback:r(()=>[...e[0]||(e[0]=[p(" Loading... ",-1)])]),_:1})),e[2]||(e[2]=t("",19))])}const k=n(u,[["render",h]]);export{S as __pageData,k as default};
