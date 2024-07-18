var turkishLangs = ["kk", "kg", "tr", "az", "tk", "kaa", "gag", "ky", "tt", "ba", "nog", "crh", "ug", "uz", "sah", "tyv", "alt", "kjh", "krc", "kum"];
var slavicLangs = ["ru", "uk", "bg", "be", "mk", "sl", "cnr", "bs", "hr", "sr", "pl", "cs", "sk", "csb", "hsb", "dsb"];

var langContext;

function messageLang(str) {
  var currentLang = $caila.detectLanguage([str])[0];
  return currentLang;
}

function isKZ(str) {
  return turkishLangs.includes(messageLang(str));
}

function isRU(str) {
  return slavicLangs.includes(messageLang(str));
}

function currentLang(context) {
  return messageLang(context.parseTree.text);
}

function currentKZ(context) {
  return turkishLangs.indexOf(currentLang(context)) !== -1;
}

function currentRU(context) {
  return slavicLangs.indexOf(currentLang(context)) !== -1;
}

function setContext(context) {
    langContext = context;
}

//context is always $context (a system variable)
function extractPhrase(fileName, stateName, context) {

    if (currentKZ(context)) {
        $dialer.setTtsConfig({ "lang": "kk-KK", "voice": "amira", "speed": 1.0, 'useV3': true});
        return CommonAnswers[fileName][stateName]["KZ"];
    } else if (currentRU(context)) {
        $dialer.setTtsConfig({ "lang": "ru-RU", "voice": "alena", "speed": 1.0, 'useV3': true});
        return CommonAnswers[fileName][stateName]["RU"];
    } else {
        $dialer.setTtsConfig({ "lang": "kk-KK", "voice": "amira", "speed": 1.0, 'useV3': true});
        return CommonAnswers[fileName][stateName]["KZ"];
    }
}