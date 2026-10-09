const {createApp}=Vue;
createApp({
 data(){return {user:null,token:sessionStorage.getItem('token')||'',credentials:{username:'',password:''},register:false,busy:false,error:'',tab:'products',cart:[],cartAddress:'',cartKey:'',products:[],orders:[],refunds:[],knowledge:[],audit:[],conversations:[],messages:[],activeConversation:'',mode:'ASSISTED',text:'',trace:[],agentConfigured:false,selected:null,purchase:{quantity:1,address:'',requestKey:''},newKnowledge:{title:'',content:''},timer:null}},
 computed:{merchant(){return this.user?.role==='MERCHANT'},tabs(){return this.merchant?[{id:'chat',label:'客服会话'},{id:'orders',label:'订单与售后'},{id:'knowledge',label:'知识库'},{id:'audit',label:'操作审计'}]:[{id:'products',label:'选购商品'},{id:'cart',label:'购物车'},{id:'orders',label:'我的订单'},{id:'chat',label:'咨询客服'},{id:'knowledge',label:'店铺政策'}]}},
 methods:{
 async api(path,body){const headers={};if(this.token)headers.Authorization='Bearer '+this.token;if(body!==undefined)headers['Content-Type']='application/json';const r=await fetch('/api'+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:JSON.stringify(body)});const result=await r.json();if(!r.ok)throw new Error(result.error||'请求失败');return result},
 async safe(task){if(this.busy)return;this.busy=true;this.error='';try{await task()}catch(e){this.error=e.message}finally{this.busy=false}},
 async signIn(){await this.safe(async()=>{if(this.register)await this.api('/auth/register',this.credentials);const r=await this.api('/auth/login',this.credentials);this.token=r.token;sessionStorage.setItem('token',this.token);this.user=r.user;this.credentials.password='';this.tab=this.merchant?'chat':'products';await this.refresh()})},
 async logout(){try{await this.api('/auth/logout',{})}catch(e){}this.token='';sessionStorage.removeItem('token');this.user=null;this.activeConversation='';this.messages=[]},
 async refresh(){try{this.products=await this.api('/products');if(!this.user)return;this.orders=await this.api('/orders');this.refunds=await this.api('/refunds');this.knowledge=await this.api('/knowledge');if(this.merchant){this.conversations=await this.api('/conversations');this.agentConfigured=(await this.api('/agent/status')).configured;if(this.tab==='audit')this.audit=await this.api('/audit');if(this.activeConversation)this.mode=this.conversations.find(c=>c.id===this.activeConversation)?.mode||'ASSISTED'}else{this.cart=await this.api('/cart');const c=await this.api('/conversations/mine',{});this.activeConversation=c.id}if(this.activeConversation)this.messages=await this.api('/conversations/'+this.activeConversation+'/messages')}catch(e){this.error=e.message}},
 async addCart(p){const quantity=Math.min(20,(this.cart.find(i=>i.product_id===p.id)?.quantity||0)+1);await this.act('/cart/items',{productId:p.id,quantity})},
 async setCart(item,quantity){await this.act('/cart/items',{productId:item.product_id,quantity});this.cartKey=''},
 async checkoutCart(){if(!this.cartKey)this.cartKey=crypto.randomUUID();await this.safe(async()=>{await this.api('/cart/checkout',{address:this.cartAddress,requestKey:this.cartKey});this.cartKey='';this.tab='orders';await this.refresh()})},
 checkout(p){this.selected=p;this.purchase={quantity:1,address:'',requestKey:crypto.randomUUID()};this.$refs.checkout.showModal()},
 async placeOrder(){await this.safe(async()=>{await this.api('/orders',{productId:this.selected.id,...this.purchase});this.$refs.checkout.close();this.tab='orders';await this.refresh()})},
 async act(path,body={}){await this.safe(async()=>{await this.api(path,body);await this.refresh()})},
 async ship(o){const t=prompt('输入模拟物流编号','DEMO-'+o.id.slice(0,8));if(t)await this.act('/orders/'+o.id+'/ship',{tracking:t})},
 async refund(o){const reason=prompt('请填写售后原因');if(reason)await this.act('/orders/'+o.id+'/refund',{reason})},
 async approveRefund(r){if(confirm('确认批准该订单的模拟退款？不会产生真实资金转移。'))await this.act('/refunds/'+r.id+'/approve')},
 async selectConversation(c){this.activeConversation=c.id;this.mode=c.mode;this.trace=[];await this.refresh()},
 async sendMessage(){const content=this.text;await this.safe(async()=>{await this.api('/conversations/'+this.activeConversation+'/messages',{content});this.text='';await this.refresh()})},
 async generate(){await this.safe(async()=>{const r=await this.api('/conversations/'+this.activeConversation+'/draft',{});this.trace=r.trace;await this.refresh()})},
 async toggleMode(){await this.act('/conversations/'+this.activeConversation+'/mode',{mode:this.mode==='HUMAN'?'ASSISTED':'HUMAN'})},
 async addKnowledge(){await this.safe(async()=>{await this.api('/knowledge',this.newKnowledge);this.newKnowledge={title:'',content:''};await this.refresh()})},
 money(v){return (v/100).toFixed(2)},productName(id){return this.products.find(p=>p.id===id)?.name||id},status(s){return {PENDING:'待处理',PAID:'已模拟支付',SHIPPED:'已模拟发货',REFUNDED:'已模拟退款',APPROVED:'已批准'}[s]||s}
 },
 async mounted(){if(this.token){try{this.user=await this.api('/auth/me');this.tab=this.merchant?'chat':'products';await this.refresh()}catch(e){sessionStorage.removeItem('token');this.token=''}}this.timer=setInterval(()=>{if(this.user&&this.tab==='chat'&&!this.busy)this.refresh()},5000)},beforeUnmount(){clearInterval(this.timer)}
}).mount('#app');
