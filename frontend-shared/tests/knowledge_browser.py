from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from functools import partial
import threading
import json,re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

root=Path(__file__).resolve().parents[2]
(root/'outputs').mkdir(exist_ok=True)
with sync_playwright() as playwright:
 browser=playwright.chromium.launch(headless=True)
 for app,port,upload in [('vue_student',9533,False),('vue_admin',9531,True),('vue_superadmin',9532,True)]:
  class QuietHandler(SimpleHTTPRequestHandler):
   def log_message(self,*args): pass
  server=ThreadingHTTPServer(('127.0.0.1',0),partial(QuietHandler,directory=str(root/app/'dist')))
  threading.Thread(target=server.serve_forever,daemon=True).start()
  port=server.server_port
  context=browser.new_context(viewport={'width':1440,'height':1000})
  key=re.search(r"TokenKey = '([^']+)'",(root/app/'src/utils/auth.js').read_text(encoding='utf-8')).group(1)
  context.add_cookies([{'name':key,'value':'browser-test-token','url':f'http://127.0.0.1:{port}'}])
  mode={'error':False}
  def api(route):
   path=route.request.url
   if '/knowledge/' in path and path.endswith('/chat'):
    body='event: error\ndata: 知识库暂不可用\n\n' if mode['error'] else 'data: 根据社团章程，请准备申请材料。\n\n'
    route.fulfill(status=200,content_type='text/event-stream',body=body,headers={'Access-Control-Allow-Origin':'*'})
   else:
    data={'groups':[{'id':1,'name':'摄影社'}]} if path.endswith('/knowledge/groups') else {'documents':[{'documentId':'doc-1','filename':'社团章程.pdf','status':'ACTIVE'}]} if '/knowledge/' in path else {'name':'测试用户','avatar':''}
    route.fulfill(status=200,content_type='application/json',body=json.dumps({'code':20000,'success':True,'data':data}),headers={'Access-Control-Allow-Origin':'*'})
  context.route('http://localhost:8081/**',api)
  context.route('**/api/**',api)
  page=context.new_page();errors=[];page.on('pageerror',lambda error:errors.append(str(error)))
  page.goto(f'http://127.0.0.1:{port}/#/knowledge');page.wait_for_load_state('networkidle')
  (root/'outputs/browser-state.json').write_text(json.dumps({'url':page.url,'errors':errors,'text':page.locator('body').inner_text()[:1500]},ensure_ascii=False),encoding='utf-8')
  page.screenshot(path=str(root/'outputs/browser-inspect.png'),full_page=True)
  expect(page.get_by_role('heading',name='把问题交给社团助手')).to_be_visible()
  page.get_by_label('选择社团',exact=True).select_option('1')
  page.get_by_label('选择社团',exact=True).press('Tab')
  page.get_by_role('button',name='查看资料').click();expect(page.get_by_text('社团章程.pdf',exact=False)).to_be_visible()
  assert page.locator('input[type=file]').count()==int(upload)
  page.get_by_label('你的问题',exact=True).fill('如何申请活动？')
  page.get_by_role('button',name='发送问题').click()
  expect(page.get_by_text('根据社团章程，请准备申请材料。',exact=True)).to_be_visible()
  mode['error']=True
  page.get_by_label('你的问题',exact=True).fill('继续说明')
  page.get_by_role('button',name='发送问题').click()
  expect(page.get_by_role('alert')).to_contain_text('知识库暂不可用')
  page.get_by_role('button',name='新建会话').click();expect(page.locator('.knowledge-message')).to_have_count(0)
  page.screenshot(path=str(root/'outputs'/f'{app}-knowledge-desktop.png'),full_page=True)
  for width in [375,768,1024]:
   page.set_viewport_size({'width':width,'height':900})
   page.wait_for_function('document.documentElement.scrollWidth <= window.innerWidth',timeout=3000)
   page.screenshot(path=str(root/'outputs'/f'{app}-knowledge-{width}.png'),full_page=True)
   assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'),f'{app}: horizontal overflow at {width}'
  page.screenshot(path=str(root/'outputs'/f'{app}-knowledge-mobile.png'),full_page=True)
  assert not errors,errors
  context.close();server.shutdown();server.server_close()
 server=ThreadingHTTPServer(('127.0.0.1',0),partial(QuietHandler,directory=str(root/'web_portal/dist')))
 threading.Thread(target=server.serve_forever,daemon=True).start()
 context=browser.new_context();page=context.new_page();portal_errors=[]
 page.on('pageerror',lambda error:portal_errors.append(str(error)))
 for role,destination in [('student',9533),('admin',9531),('superadmin',9532)]:
  context.route(f'http://localhost:{destination}/**',lambda route:route.fulfill(status=200,content_type='text/html',body='<h1>Login</h1>'))
  page.goto(f'http://127.0.0.1:{server.server_port}');page.wait_for_load_state('networkidle')
  expect(page.get_by_role('heading',name='请选择你的登录身份')).to_be_visible()
  page.locator(f'[data-role-target="{role}"]').click()
  page.wait_for_url(f'http://localhost:{destination}/#/login')
 assert not portal_errors,portal_errors
 context.close();server.shutdown();server.server_close()
 browser.close()
print('PASS: three roles, chat SSE, errors, document list, upload visibility, reset, responsive widths')
