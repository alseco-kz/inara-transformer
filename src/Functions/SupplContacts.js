// require: ErrorBind/ErrorLogger.js

function SupplContactsClear(){
    var $session = $jsapi.context().session;
    $session.SupplContracts = {};
}
function  SupplContactsSetServ(_servId){
    // log('Услуга' + toPrettyString(_servId));
    var $session = $jsapi.context().session;
    $session.SupplContracts = $session.SupplContracts || {};
    $session.SupplContracts.servId = _servId;
}
    
function  SupplContactsSetSuppl(_mainSupplCode){
    var $session = $jsapi.context().session;
    $session.SupplContracts = $session.SupplContracts || {};
    $session.SupplContracts.mainSupplCode = _mainSupplCode;
}

function SupplContactsIsSuppSet(){
    var $session = $jsapi.context().session;
    return $session.SupplContracts.mainSupplCode;
}
function SupplContactsIsServSet(){
    var $session = $jsapi.context().session;
    return $session.SupplContracts.servId;
}

function SupplContactsGetSupplCode(){
    var $session = $jsapi.context().session;
    if (SupplContactsIsSuppSet())
        return $session.SupplContracts.mainSupplCode.code
    else return '';
}
function SupplContactsGetServices(){
    var $session = $jsapi.context().session;
    if (SupplContactsIsServSet())
        return $session.SupplContracts.servId
    else return '';
}

function SupplContactsGetServicesArray(){
    var return_str = "";
    var $session = $jsapi.context().session;
    $session.SupplContracts.servId.forEach(function(elem){
        return_str = return_str + (return_str.length>0? "&": "")+"serviceCodes="  + elem;
    });
    return return_str;
    
}
//////// Надо сохранить данные по коду услуги, коду поставщика и его телефону
function FixTalkSupplContacts(elem){
    $.session.SupplContracts = $.session.SupplContracts || {};
    $.session.SupplContracts.TalkContacts = {
        "supplierCodeName": elem.supplierCodeName,
        "serviceCode": elem.serviceCode,
        "talkContacts": elem.supplierContacts
        }
}

function SupplContactsGetContactsByAccountServ(MainSuppList, ret_contacts, need_suppl_name ){
    // есть ЛС, есть коды услуг
    // надо обратиться к сервису и получить значения
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    // есть ЛС и есть услуга
    if ((FindAccountIsAccountSet()) && (SupplContactsIsServSet())){
        // обращаемся к сервису для получения данных по услуге
        var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts/';
        var token = $secrets.get("InaraSeviceToken", "Токен не найден");

        var url = addr + $session.Account.Number + '/services/contacts?';
        // из услуг надо сформировать строку вида serviceCodes=11&serviceCodes=13 
        var serv = SupplContactsGetServicesArray();
        url = url + serv;
        try{
            var response =  $http.query(url, {method: "GET",
                timeout: 20000        // таймаут выполнения запроса в мс
                ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token}
            });
        }
        catch(e){
            //$reactions.answer("Что-то сервер барахлит. ");
            log('--------------- произошла ошибка' );
            SendErrorMessage("onHttpRequest", 'Функция: SupplContactsGetContactsByAccountServ')
            return false;
        };

        // анализируем ответ от сервера
        if(response.isOk){
            if (response.data && response.data.services){
                // $session.Account.MainSuppliers =  response.data.data[0].suppl_list;
                response.data.services.forEach(function(elem){
                    // log(toPrettyString(elem))
                    if (elem.supplierContacts.length > 0){
                        var suppl_name = elem.supplierName;
                        
                    
                        if (MainSuppList[elem.supplierCodeName])
                            suppl_name  = MainSuppList[elem.supplierCodeName].value.suppl_talk_name
                        //если в строке есть ОСИ, надо заменить ее
                        suppl_name = suppl_name.replace('ОСИ','ОС+И');
                        suppl_name = suppl_name.replace('ПТ ','П Т ');
                        suppl_name = suppl_name.replace('ПКСК','пэ ка эс ка');
                        suppl_name = suppl_name.replace('КСК','ка эс ка');}
                        // var first_index = suppl_name.indexOf('"');
                        // var last_index = suppl_name.lastIndexOf('"'); 
                        // if ((first_index>0)&& (last_index>first_index))
                        //     suppl_name = suppl_name.substring(first_index,last_index)

                    // собираем строку из контактов
                    // проверяем, если это основной поставщик, то даем его наименование 
                    // если нет, то данные из сервиса 
                    
                    else{
                         return false;}
                    
                    if (need_suppl_name)
                        ret_contacts.text = suppl_name + ' - ' + elem.supplierContacts
                    else 
                        ret_contacts.text = elem.supplierContacts
                        
                    //////// Надо сохранить данные по коду услуги, коду поставщика и его телефону
                    FixTalkSupplContacts(elem)

                });
            }
        }
        else 
        {
            // произошла ошибка сервиса - надо залогировать
            SendErrorMessage("onHttpResponseError", toPrettyString(response.error))
            return false;
        }
    
}
    
}
    

    
