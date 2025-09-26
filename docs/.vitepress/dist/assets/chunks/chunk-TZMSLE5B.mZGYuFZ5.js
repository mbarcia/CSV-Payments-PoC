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

import{_ as n,n as c,j as l}from"../app.D1bwSZmY.js";var o=n((a,t)=>{const e=a.append("rect");if(e.attr("x",t.x),e.attr("y",t.y),e.attr("fill",t.fill),e.attr("stroke",t.stroke),e.attr("width",t.width),e.attr("height",t.height),t.name&&e.attr("name",t.name),t.rx&&e.attr("rx",t.rx),t.ry&&e.attr("ry",t.ry),t.attrs!==void 0)for(const r in t.attrs)e.attr(r,t.attrs[r]);return t.class&&e.attr("class",t.class),e},"drawRect"),d=n((a,t)=>{const e={x:t.startx,y:t.starty,width:t.stopx-t.startx,height:t.stopy-t.starty,fill:t.fill,stroke:t.stroke,class:"rect"};o(a,e).lower()},"drawBackgroundRect"),g=n((a,t)=>{const e=t.text.replace(c," "),r=a.append("text");r.attr("x",t.x),r.attr("y",t.y),r.attr("class","legend"),r.style("text-anchor",t.anchor),t.class&&r.attr("class",t.class);const s=r.append("tspan");return s.attr("x",t.x+t.textMargin*2),s.text(e),r},"drawText"),h=n((a,t,e,r)=>{const s=a.append("image");s.attr("x",t),s.attr("y",e);const i=l(r);s.attr("xlink:href",i)},"drawImage"),m=n((a,t,e,r)=>{const s=a.append("use");s.attr("x",t),s.attr("y",e);const i=l(r);s.attr("xlink:href",`#${i}`)},"drawEmbeddedImage"),y=n(()=>({x:0,y:0,width:100,height:100,fill:"#EDF2AE",stroke:"#666",anchor:"start",rx:0,ry:0}),"getNoteRect"),p=n(()=>({x:0,y:0,width:100,height:100,"text-anchor":"start",style:"#666",textMargin:0,rx:0,ry:0,tspan:!0}),"getTextObj");export{d as a,p as b,m as c,o as d,h as e,g as f,y as g};
