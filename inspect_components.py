import re
import urllib.request
base='https://www.scanandprint.in/'
entry=urllib.request.urlopen(base+'assets/index-BVvjeBY5.js',timeout=30).read().decode('utf-8','replace')
chunks=sorted(set(re.findall(r'assets/[A-Za-z0-9_-]+\.js',entry)))
selected=['Home-','Features-','Pricing-','HowToSetup-','RegisterShop-','ShopLogin-','CustomerKiosk-','EditorPage-','OwnerOverview-','OwnerJobs-','OwnerPrinters-','OwnerPricing-','OwnerQrCode-','OwnerAgentDownload-','OwnerSettings-','OwnerPaymentSetup-','OwnerDevices-','OwnerSubscription-','OwnerReview-','OwnerReferralCard-','BlogList-','BlogPost-','Accordion-','Contact-','About-','AdminOverview-']
out=[]
for chunk in chunks:
 if not any(name in chunk for name in selected): continue
 try:
  data=urllib.request.urlopen(base+chunk,timeout=30).read().decode('utf-8','replace')
  # print readable JSX string literals, skipping common asset paths, CSS class names and minified filler
  vals=[]
  for m in re.finditer(r'(["\'`])((?:\\.|(?!\1).){2,500}?)\1',data):
   s=m.group(2).replace('\\n',' ').replace('\\t',' ')
   if re.search(r'[A-Za-z]{3}',s) and not re.fullmatch(r'[A-Za-z0-9_./:-]+',s) and not s.startswith(('http','assets/','/assets/','M','0x')):
    vals.append(s)
  out.append(f'### {chunk} length={len(data)}\n'+'\n'.join(vals))
 except Exception as e: out.append(f'### {chunk} ERROR {e}')
# Capture exact route and UI linkage hints from app entry near route paths
for pattern in ['path','createBrowserRouter','Routes','/owner/jobs','/register','/studio','/kiosk']:
 for m in list(re.finditer(re.escape(pattern),entry))[:20]:
  out.append(f'ENTRY {pattern}: '+entry[max(0,m.start()-240):m.start()+600])
open('public_components_audit.txt','w',encoding='utf-8').write('\n\n'.join(out))
print(f'Inspected {len(out)} component/route excerpts from {len(chunks)} referenced chunks')
