import re
import urllib.request
from html import unescape
base='https://www.scanandprint.in'
paths=['/','/features','/pricing','/how-to-setup','/about','/contact','/register','/login','/shop-login','/kiosk','/print-now','/studio','/editor','/dashboard','/owner','/owner/jobs','/owner/printers','/owner/qr-code','/owner/payment-setup','/owner/settings','/owner/subscription','/blog']
chunks=set()
out=[]
for path in paths:
 try:
  req=urllib.request.Request(base+path,headers={'User-Agent':'Mozilla/5.0'})
  data=urllib.request.urlopen(req,timeout=20).read().decode('utf-8','replace')
  scripts=re.findall(r'<script[^>]+src="([^"]+)',data)
  chunks.update(x for x in scripts if x.endswith('.js'))
  title=re.search(r'<title>(.*?)</title>',data,re.S)
  text=re.sub(r'<(script|style)[^>]*>.*?</\1>',' ',data,flags=re.S|re.I)
  text=re.sub(r'<[^>]+>',' ',text)
  text=' '.join(unescape(text).split())
  out.append(f'### {path} [{len(data)} bytes] {unescape(title.group(1)) if title else ""}\n{text[:8000]}')
 except Exception as e: out.append(f'### {path}\nFETCH ERROR: {e}')
for chunk in sorted(chunks):
 try:
  req=urllib.request.Request(base+chunk,headers={'User-Agent':'Mozilla/5.0'})
  data=urllib.request.urlopen(req,timeout=25).read().decode('utf-8','replace')
  strings=[]
  for m in re.finditer(r'(["\'`])((?:\\.|(?!\1).){3,300}?)\1',data):
   s=m.group(2)
   if re.search(r'[A-Za-z]{3}',s) and not s.startswith(('http','assets/','image/','application/','text/','M','0x')):
    strings.append(s)
  out.append(f'### CHUNK {chunk} [{len(data)} bytes]\n'+' | '.join(strings[:350]))
 except Exception as e: out.append(f'### CHUNK {chunk}\nFETCH ERROR: {e}')
open('public_site_audit.txt','w',encoding='utf-8').write('\n\n'.join(out))
print(f'Wrote report for {len(paths)} routes and {len(chunks)} entry chunks')
