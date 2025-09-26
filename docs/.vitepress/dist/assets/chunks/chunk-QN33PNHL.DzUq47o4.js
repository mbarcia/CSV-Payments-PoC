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

import{_ as a,e as w,l as x}from"../app.Cb2ZRlQq.js";var d=a((e,t,i,o)=>{e.attr("class",i);const{width:r,height:h,x:n,y:c}=u(e,t);w(e,h,r,o);const s=l(n,c,r,h,t);e.attr("viewBox",s),x.debug(`viewBox configured: ${s} with padding: ${t}`)},"setupViewPortForSVG"),u=a((e,t)=>{var o;const i=((o=e.node())==null?void 0:o.getBBox())||{width:0,height:0,x:0,y:0};return{width:i.width+t*2,height:i.height+t*2,x:i.x,y:i.y}},"calculateDimensionsWithPadding"),l=a((e,t,i,o,r)=>`${e-r} ${t-r} ${i} ${o}`,"createViewBox");export{d as s};
