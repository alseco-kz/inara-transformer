// проверяем, был ли выявлен телефон в ходе диалога
function FindPhoneIsPhoneSet(){
    var $session = $jsapi.context().session;
    if (($session.Phone) && ($session.Phone.Number > 0))
        return true
    else 
        return false
}
//---------------------------------------------------------------------------
// очистка номера телефона
function FindPhoneNumberClear(){
    var $session = $jsapi.context().session;
    $session.Phone= {};
    $session.Phone.Number = 0;
    $session.Phone._number = 0;
    
    
}
//---------------------------------------------------------------------------
//
function FindPhoneNumberStart(){
    var $session = $jsapi.context().session;
    $session.Phone = {};
    $session.Phone.Number = 0;
    $session.Phone._number = 0;
    $session.oldState = $jsapi.context().session._lastState;
    $session.Phone.RetryPhone = 0; // количество раз, сколько спрашивали номер ЛС
    $session.Phone.Succeed = false;
    $session.Phone.Result = "";
}
//---------------------------------------------------------------------------
//
function FindPhoneNumberSetResult(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Phone.Result = result_comment;
    $session.Phone._number = 0;
    $session.Phone.Number = -1;
    $session.Phone.Succeed = false;
}
//---------------------------------------------------------------------------
//
function FindPhoneNumberSetSuccees(result_comment)
{
    var $session = $jsapi.context().session;
    $session.Phone.Result = result_comment;
    $session.Phone.Number = $session.Phone._number;
    $session.Phone._number = 0;
    $session.Phone.Succeed = true;

}
//---------------------------------------------------------------------------
//
function TrySetNumberforPhone(phone_num)
{
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    $session.Phone._number = phone_num;

    return $session.Phone._number > 0;
    
}
//---------------------------------------------------------------------------

//---------------------------------------------------------------------------
//
function GetTempPhoneNumber(){
    var $session = $jsapi.context().session;
    return $session.Phone._number;
}
// возвращает сохраненный номер Телефона
function GetPhoneNumber(){
    var $session = $jsapi.context().session;
    return $session.Phone.Number;
}
//---------------------------------------------------------------------------
// Как говорить номер телефона (разбиение по разрядам)
function PhoneTalkNumber(phone_num){
    return phone_num.toString().replace(/\B(?=(\d{2})+(?!\d))/g, "- - ")    
}

