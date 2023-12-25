// проверяем, был ли выявлен ЛС в ходе диалога
function FindAccountIsAccountSet(){
    var $session = $jsapi.context().session;
    if (($session.Account) && ($session.Account.Number > 0))
        return true
    else 
        return false
}
//---------------------------------------------------------------------------
// очистка номера ЛС
function FindAccountNumberClear(){
    var $session = $jsapi.context().session;
    $session.Account = {};
    $session.Account.Number = 0;
    $session.Account._number = 0;
    
    
}
//---------------------------------------------------------------------------
//
function FindAccountNumberStart(){
    var $session = $jsapi.context().session;
    $session.Account = {};
    $session.Account.Number = 0;
    $session.Account._number = 0;
    $session.oldState = $jsapi.context().session._lastState;
    $session.Account.RetryAccount = 0; // количество раз, сколько спрашивали номер ЛС
    $session.Account.Succeed = false;
    $session.Account.Result = "";
}
//---------------------------------------------------------------------------
//
function FindAccountNumberSetResult(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Account.Result = result_comment;
    $session.Account._number = 0;
    $session.Account.Number = -1;
    $session.Account.Succeed = false;
}
//---------------------------------------------------------------------------
//
function FindAccountNumberSetSuccees(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Account.Result = result_comment;
    $session.Account.Number = $session.Account._number;
    $session.Account._number = 0;
    $session.Account.Succeed = true;

}
//---------------------------------------------------------------------------
//
function TrySetNumber(acc_num)
{
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    $session.Account._number = acc_num;
    // ищем адрес
    $session.Account.Address = "";
    return $session.Account._number > 0;
    
}
//---------------------------------------------------------------------------
//
function FindAccountAddress(){
    var $injector = $jsapi.context().injector;
    var $session = $jsapi.context().session;
    var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts/';
    
    // log("addr value = "+ addr);
    // var url = $injector.InaraServiceUrl + $session.Account._number + '/address';
    var url = addr + $session.Account._number + '/address';
    var token = $secrets.get("InaraSeviceToken", "Токен не найден")

    
    return $http.query(url, {method: "GET",
        timeout: 20000        // таймаут выполнения запроса в мс
        ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token
            
        }
    });
}
//---------------------------------------------------------------------------
//
function GetTempAccountNumber(){
    var $session = $jsapi.context().session;
    return $session.Account._number;
}
// возвращает сохраненный номер ЛС
function GetAccountNumber(){
    var $session = $jsapi.context().session;
    return $session.Account.Number;
}
//---------------------------------------------------------------------------
// Как говорить номер ЛС (разбиение по разрядам)
function AccountTalkNumber(acc_num){
    return acc_num.toString().replace(/\B(?=(\d{2})+(?!\d))/g, "- - ")    
}

