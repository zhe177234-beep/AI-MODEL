import json,os,time,subprocess,urllib.request,urllib.error,uuid,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
base='http://127.0.0.1:8080';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
env=dict(os.environ,MERCHANT_PASSWORD='MerchantPassword123!',DB_URL='jdbc:h2:file:/tmp/agent-http-db-'+uuid.uuid4().hex+';MODE=MySQL;DATABASE_TO_LOWER=TRUE')
log=open('/tmp/agent-http-server.log','w');p=None

def call(path,body=None,token=None):
 data=None if body is None else json.dumps(body).encode();h={'Content-Type':'application/json'}
 if token:h['Authorization']='Bearer '+token
 r=opener.open(urllib.request.Request(base+path,data=data,headers=h),timeout=15)
 raw=r.read();return json.loads(raw) if r.headers.get_content_type()=='application/json' else raw

def start():
 p=subprocess.Popen([os.environ.get('JAVA_BIN','java'),'-jar',str(root/'backend/target/customer-agent-0.1.0.jar')],env=env,stdout=log,stderr=log,cwd='/tmp')
 for _ in range(50):
  if p.poll() is not None:raise RuntimeError('Server failed to start')
  try:
   if call('/api/health')['status']=='ok':return p
  except Exception:pass
  time.sleep(.3)
 raise RuntimeError('Startup timeout')

def stop(p):
 p.terminate()
 try:p.wait(timeout=8)
 except subprocess.TimeoutExpired:p.kill();p.wait()

try:
 p=start()
 assert b'vue.global.prod.js' in call('/')
 assert len(call('/vendor/vue.global.prod.js'))>100000
 credentials={'username':'smoke_buyer','password':'BuyerPassword123!'};call('/api/auth/register',credentials);buyer=call('/api/auth/login',credentials)['token']
 merchant=call('/api/auth/login',{'username':'merchant','password':'MerchantPassword123!'})['token']
 call('/api/cart/items',{'productId':'p-lamp','quantity':2},buyer);orders=call('/api/cart/checkout',{'address':'Smoke test address','requestKey':uuid.uuid4().hex},buyer);order=orders[0]['id']
 call('/api/orders/'+order+'/pay',{},buyer);call('/api/orders/'+order+'/ship',{'tracking':'DEMO-SMOKE'},merchant)
 refund=call('/api/orders/'+order+'/refund',{'reason':'Smoke test refund'},buyer);call('/api/refunds/'+refund['id']+'/approve',{},merchant)
 assert call('/api/orders/'+order,token=buyer)['status']=='REFUNDED'
 c=call('/api/conversations/mine',{},buyer)['id'];call('/api/conversations/'+c+'/messages',{'content':'请查询订单'},buyer)
 draft=call('/api/conversations/'+c+'/draft',{},merchant);assert draft['mode']=='UNCONFIGURED'
 assert len(call('/api/conversations/'+c+'/messages',token=buyer))==1
 call('/api/conversations/'+c+'/drafts/'+draft['message']['id']+'/approve',{},merchant)
 assert len(call('/api/conversations/'+c+'/messages',token=buyer))==2
 stop(p);p=start();assert call('/api/orders/'+order,token=buyer)['status']=='REFUNDED';assert len(call('/api/conversations/'+c+'/messages',token=buyer))==2
 print(json.dumps({'status':'PASS','checks':['jar-startup','static-assets','register-login','cart-checkout','payment','shipment','refund','draft-hidden','approve-send','restart-persistence']},ensure_ascii=False))
finally:
 if p is not None and p.poll() is None:stop(p)
 log.close()
