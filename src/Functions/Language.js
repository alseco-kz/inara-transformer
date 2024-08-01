var turkishLangs = ["kk", "kg", "tr", "az", "tk", "kaa", "gag", "ky", "tt", "ba", "nog", "crh", "ug", "uz", "sah", "tyv", "alt", "kjh", "krc", "kum"];
var slavicLangs = ["ru", "uk", "bg", "be", "mk", "sl", "cnr", "bs", "hr", "sr", "pl", "cs", "sk", "csb", "hsb", "dsb"];

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

function currentLang() {
  return messageLang($jsapi.context().parseTree.text);
}

function currentKZ() {
  return turkishLangs.indexOf(currentLang($jsapi.context())) !== -1;
}

function currentRU() {
  return slavicLangs.indexOf(currentLang($jsapi.context())) !== -1;
}

function setLastKZ() {
    $dialer.setTtsConfig({ "lang": "kk-KK", "voice": "amira", "speed": 1.0, 'useV3': true});
    $jsapi.context().session.lastLang = "KZ";
}

function setLastRU() {
    $dialer.setTtsConfig({ "lang": "ru-RU", "voice": "alena", "speed": 1.0, 'useV3': true});
    $jsapi.context().session.lastLang = "RU";
}

function extractPhrase(fileName, stateName) {
    
    if (currentKZ()) {
        setLastKZ();
        return CommonAnswers[fileName][stateName]["KZ"];
    } else if (currentRU()) {
        setLastRU();
        return CommonAnswers[fileName][stateName]["RU"];
    } else {
        setLastKZ();
        return CommonAnswers[fileName][stateName]["KZ"];
    }
}