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

import{_ as e,l as o,H as i,e as n,I as p}from"../app.BQKyEa-c.js";import{p as g}from"./treemap-75Q7IDZK.D4EepEgS.js";import"./framework.BoyqnxIq.js";import"./theme.vqtJgV6x.js";import"./baseUniq.BMJ5-t5J.js";import"./basePickBy.r7vk2RdQ.js";import"./clone.DQAOX4T6.js";var v={parse:e(async r=>{const a=await g("info",r);o.debug(a)},"parse")},d={version:p.version+""},m=e(()=>d.version,"getVersion"),c={getVersion:m},l=e((r,a,s)=>{o.debug(`rendering info diagram
`+r);const t=i(a);n(t,100,400,!0),t.append("g").append("text").attr("x",100).attr("y",40).attr("class","version").attr("font-size",32).style("text-anchor","middle").text(`v${s}`)},"draw"),f={draw:l},D={parser:v,db:c,renderer:f};export{D as diagram};
