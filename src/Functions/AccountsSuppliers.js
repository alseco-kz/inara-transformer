//*****************************************************************
// Модуль предназначен для вывода информации по основным поставщикам 
// работает как с лицеввым счетом, так и предоставляет общую информацию по всем поставщикам

// Функции:
// получает список  и контакты  поставщиков по ЛС и в целом
// function GetAccountMainSuppls(){

// возвращает список основных поставщиков по ЛС
// function GetAccountMainSupplNames(MainSuppList){

// возвращает список всех основных поставщиков 
// function GetMainSupplNames(MainSuppList){

// возвращает список всех основных поставщиков  с их телефонами
// function GetMainSupplNamesContracts(MainSuppList){

// возвращает список всех основных поставщиков  с их телефонами по ЛС
// function GetAccountMainSupplNamesContracts(MainSuppList){

// возвращает название поставщика и телефон
// function GetMainSupplNamesContacts(MainSuppList, suppl_name){


//*****************************************************************

//-----------------------------------------------------------------
// получает список  и контакты  поставщиков по ЛС и в целом
function GetAccountMainSuppls(){
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    var return_str = '';
    // есть сведения по ЛС 
    if ($session.Account && $session.Account.Number>0){
        // смотрим, была ли получена ранее информация по основным поставщикам
        if (!$session.Account.MainSuppliers){
            // функция для поиска поставщиков
            // var url = $injector.MacrosUrl + "sheetURL=" + $injector.AccountTableURL + "&sheetName="+$injector.AccountSheetSupplName
            // url = url + "&filterHead=account_number&filterValue="+$session.Account.Number;

            var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts/';
            var url = addr + $session.Account.Number + '/mainSuppliers';
            var token = $secrets.get("InaraSeviceToken", "Токен не найден")

            try{
                var response =  $http.query(url, {method: "GET",
                    timeout: 20000        // таймаут выполнения запроса в мс
                    ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token}
                });
            }
            catch(e){
                //$reactions.answer("Что-то сервер барахлит. ");
                log('--------------- произошла ошибка' );
                SendErrorMessage("onHttpRequest", 'Функция: GetAccountMainSuppls')
                return false;
            };

            if(response.isOk){
                if (response.data /*&& response.data.data*/){
                    // $session.Account.MainSuppliers =  response.data.data[0].suppl_list;
                    $session.Account.MainSuppliers =  response.data.mainSuppliers;

                    if (typeof($session.Account.MainSuppliers)=="string"){
                        var  Names = $session.Account.MainSuppliers;
                        Names = Names.replaceAll( "\"","\'");
                        Names = Names.replaceAll( "\'","\"");
                        $session.Account.MainSuppliers = JSON.parse(Names);
                    }
                    
                }
            }
            else{
                // произошла ошибка сервиса - надо залогировать
                SendErrorMessage("onHttpResponseError", toPrettyString(response.error))
            }
        }
    }

    return (($session.Account) && ($session.Account.MainSuppliers));
}
//-----------------------------------------------------------------
// возвращает список основных поставщиков по ЛС
function GetAccountMainSupplNames(MainSuppList){
    var $session = $jsapi.context().session;
    var return_str = "";
    if (GetAccountMainSuppls()){
        $session.Account.MainSuppliers.forEach(function(elem){
            return_str = return_str + (return_str.length>0? ", ": " ")  + MainSuppList[elem.toLowerCase()].value.suppl_talk_name;
        });
    }
    return return_str;
}

//-----------------------------------------------------------------
// возвращает список всех основных поставщиков 
function GetMainSupplNames(MainSuppList){
    var $session = $jsapi.context().session;
    var return_str = "";

    Object.keys(MainSuppList).forEach(function(elem){
        if (MainSuppList[elem].value.is_main){
            return_str = return_str + (return_str.length > 0 ? ", ": " ")  + MainSuppList[elem].value.suppl_talk_name;
        }
    });
        
    return return_str;
}

//-----------------------------------------------------------------
// возвращает список всех основных поставщиков  с их телефонами
function GetMainSupplNamesContracts(MainSuppList){
    var $session = $jsapi.context().session;
    var return_str = "";

    //$reactions.answer(toPrettyString(MainSuppList));
    Object.keys(MainSuppList).forEach(function(elem,i){
        // last не работает. надо как-то понять, что это элемент последний
        var last = (i == MainSuppList.length-1) && (i>1);
        if (MainSuppList[elem].value.is_main){
             return_str = return_str + (return_str.length > 0 ? ". ": " ") +(last? " или ": "")  + MainSuppList[elem].value.suppl_talk_name + " - " + MainSuppList[elem].value.talk_phone;
        }
//        $reactions.answer(i);
       
    });
        
    return return_str;
}
//-----------------------------------------------------------------
// возвращает список всех основных поставщиков  с их телефонами по ЛС

function GetAccountMainSupplNamesContracts(MainSuppList){
    var $session = $jsapi.context().session;
    var return_str = "";
    if (GetAccountMainSuppls()){
        $session.Account.MainSuppliers.forEach(function(elem, i){
            elem = elem.toLowerCase();
            var last = (i == ($session.Account.MainSuppliers.length-1)) && (i>1);
            return_str = return_str + (return_str.length > 0? ". ": " ") +(last? " или ": "")  + MainSuppList[elem].value.suppl_talk_name + " - " + MainSuppList[elem].value.talk_phone;
        });
        
    }
    return return_str;
}

//-----------------------------------------------------------------
// возвращает, есть ли на ЛС указанный поставщик
function isAccountHasSuppl(name) {
    var $session = $jsapi.context().session;
    var res = false;
    $session.Account.MainSuppliers.forEach(function(elem){
        if (elem === name){
            return res = true;
        }
    });
    return res;
    
}

//-----------------------------------------------------------------
// возвращает название поставщика и телефон
function GetMainSupplNamesContact(MainSuppList, suppl_name){
    var $session = $jsapi.context().session;
    
    var return_str = MainSuppList[suppl_name].value.suppl_talk_name + " - " + MainSuppList[suppl_name].value.talk_phone ;
    
    return return_str

     
 }
