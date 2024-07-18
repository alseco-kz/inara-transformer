// проверяем, был ли выявлен ИИН в ходе диалога
function FindAccountIsAccountSet(){
    var $session = $jsapi.context().session;
    if (($session.Account) && ($session.Account.Number > 0))
        return true
    else 
        return false
}
//---------------------------------------------------------------------------
// очистка номера ИИН
function FindAccountIINClear(){
    var $session = $jsapi.context().session;
    $session.Account = {};
    $session.Account.IIN = 0;
    $session.Account.iin = 0;
    
    
}
//---------------------------------------------------------------------------
//
function FindAccountIINStart(){
    var $session = $jsapi.context().session;
    $session.Account = {};
    $session.Account.IIN = 0;
    $session.Account._iin = 0;
    $session.oldState = $jsapi.context().session._lastState;
    $session.Account.RetryAccount = 0; // количество раз, сколько спрашивали номер ЛС
    $session.Account.Succeed = false;
    $session.Account.Result = "";
}
//---------------------------------------------------------------------------
//
function FindAccountIINSetResult(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Account.Result = result_comment;
    $session.Account._iin = 0;
    $session.Account.IIN = -1;
    $session.Account.Succeed = false;
}
//---------------------------------------------------------------------------
//
function FindAccountIINSetSuccees(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Account.Result = result_comment;
    $session.Account.IIN = $session.Account._iin;
    $session.Account._iin = 0;
    $session.Account.Succeed = true;

}
//---------------------------------------------------------------------------
//
function TrySetIIN(acc_num)
{
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    $session.Account._iin = acc_num;
    // ищем адрес
    $session.Account.Address = "";
    return $session.Account._iin > 0;
    
}

function FindIINAddress(){
    var $injector = $jsapi.context().injector;
    var $session = $jsapi.context().session;
    var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts';
    
    var url = addr + "?iin=" + $session.Account._iin;
    var token = $secrets.get("InaraSeviceToken", "Токен не найден")

    
    return $http.query(url, {method: "GET",
        timeout: 20000        // таймаут выполнения запроса в мс
        ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token
            
        }
    });
}

//---------------------------------------------------------------------------
//
function FindIINAddress(){
    var $injector = $jsapi.context().injector;
    var $session = $jsapi.context().session;
    var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts';
    
    var url = addr + "?iin=" + $session.Account._iin;
    var token = $secrets.get("InaraSeviceToken", "Токен не найден")

    
    return $http.query(url, {method: "GET",
        timeout: 20000        // таймаут выполнения запроса в мс
        ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token
            
        }
    });
}
//---------------------------------------------------------------------------
//
function GetTempAccountIIN(){
    var $session = $jsapi.context().session;
    return $session.Account._iin;
}
// возвращает сохраненный номер ЛС
function GetAccountIIN(){
    var $session = $jsapi.context().session;
    return $session.Account.IIN;
}
//---------------------------------------------------------------------------
// Как говорить номер ЛС (разбиение по разрядам)
function AccountTalkIIN(acc_num){
    return acc_num.toString().replace(/\B(?=(\d{2})+(?!\d))/g, "- - ")    
}

