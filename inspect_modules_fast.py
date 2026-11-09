import re, urllib.request
base='https://www.scanandprint.in/assets/'
chunks=['Home-DcOnd0dZ.js','Features-C4dZbs8g.js','Pricing-DTj11y8D.js','HowToSetup-Cmi6xJ4_.js','RegisterShop-BAOy_VZ4.js','ShopLogin-B6FcBuga.js','CustomerKiosk-CNqx5TaZ.js','EditorPage-Cr_oy5Mt.js','OwnerOverview-6fiilCpn.js','OwnerJobs-BeiwOrpb.js','OwnerPrinters-JtwDqVIT.js','OwnerQrCode-BGFKmSyp.js','OwnerAgentDownload-IzQEsFpr.js','OwnerSettings-DXYu1GPQ.js','OwnerPaymentSetup-DegK2zgC.js','OwnerDevices-C5KLVoVS.js','OwnerSubscription-CNeFxLKY.js','OwnerReview-CHZ9Ncmc.js','OwnerReferralCard-BfSUdyBY.js','BlogList-D6oeWdke.js','BlogPost-CUXb2Lqo.js','Accordion-t8eOY4QW.js','Contact-DKsO_rOD.js','About-Bp5TDILA.js']
out=[]
for name in chunks:
 try:
  req=urllib.request.Request(base+name,headers={'User-Agent':'Mozilla/5.0'})
  data=urllib.request.urlopen(req,timeout=7).read().decode('utf-8','replace')
  vals=re.findall(r'["\']([^"\'\\]{3,180})["\']',data)
  vals=[v for v in vals if re.search('[A-Za-z]{3}',v) and 'className' not in v and not v.startswith(('http','assets/','/assets/'))]
  out.append('### '+name+' '+str(len(data))+'\n'+' | '.join(vals[:250]))
 except Exception as e: out.append('### '+name+' ERROR '+str(e))
open('public_modules_fast.txt','w',encoding='utf-8').write('\n'.join(out))
print('saved',len(out),'module summaries')
