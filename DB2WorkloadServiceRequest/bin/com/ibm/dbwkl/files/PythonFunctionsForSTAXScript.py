from com.ibm.staf import STAFUtil
import base64
	
def getRequest(dict):
	requestTmp = ' '
	for k, v in dict.items():
		if v != '' and v != 'false' and k != 'machine' and k != 'START':
			if v == 'true':
				requestTmp += ' ' + str(k) + ' ';
			else:
				requestTmp += ' ' + str(k) + ' ' + str(v);
	return requestTmp;

def convertDuration(duration):
	factor = 0

	if duration.endswith('m'):
		factor = 60
	elif duration.endswith('h'):
		factor = 3600
	elif duration.endswith('d'):
		factor = 86400

	if factor != 0:
		duration = duration[0:(len(duration)-1)]
		duration = int(duration) * factor

	return duration
	
def encryptPassword(password):
	password = STAFUtil.removePrivacyDelimiters(password)
	encpwd = base64.encodestring(password)
	encpwd = encpwd.rstrip()
	return encpwd

DURTIME = convertDuration(DURTIME)

PASSWORDBASE64 = encryptPassword(PASSWORD)

dict = {'USER': USER, 'HOST': HOST, 'SSIDS': SSIDS, 'THREADS': THREADS, 'DURTIME': DURTIME, 'TYPE': TYPE, 'PASSWORDBASE64': PASSWORDBASE64, 'NOCLEAN': NOCLEAN, 'PARALLEL': PARALLEL}

delay = STAXThreadID * 1000