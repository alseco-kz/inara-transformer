function getUserPhone(){
    var tel = ''
    if ($.session.MakeRequest && $.session.MakeRequest.userPhoneNumber)
        tel = $.session.MakeRequest.userPhoneNumber
    else if (testMode()){
        tel = '7051848013'
        // return '1111'
    }else if ($.request.channelType == 'chatwidget'){
        tel = '7051848013'
        // return '1111'
    }else if ($dialer.getCaller()){
        tel = $dialer.getCaller()
    } 
    else if (tel.length === 7 && (tel[0]!= '7' || tel[0]!= '8')  ){
        tel = '8727'+ tel
        return tel
        
    }
    else if ((tel.length === 10  && tel [0]=== '7' ) ||   (tel.length === 11 && (tel[0] == '8' || tel[0] == '+7' ) ) ){
        return tel
            }
    
       
    return tel
}

function formatPhoneNumber(phoneNumber) {
  return phoneNumber.replace(/([78]?)(\d{3})(\d{3})(\d{2})(\d{2})/, '8-$2-$3-$4-$5');
}

function setUserPhone(value){
    $.session.MakeRequest = $.session.MakeRequest || {};
    $.session.MakeRequest.userPhoneNumber = value
}

function AddRequestComplaint(){
    {
        // функция для поиска поставщиков
        // var url = $injector.MacrosUrl + "sheetURL=" + $injector.AccountTableURL + "&sheetName="+$injector.AccountSheetPayName
        // url = url + "&filterHead=account_number&filterValue="+$session.Account.Number;
        log("function AddRequestComplaint")
        var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'requests/complaint';
        var url = addr;
        var token = $secrets.get("InaraSeviceToken", "Токен не найден")
        var audio_url = $dialer.getCallRecordingFullUrl()
        if (!audio_url && testMode()){
           audio_url = 'https:\\myurl_test' 
        }
        var request_params = {
            "accountId": GetAccountNumber(),
            "supplierCodeName": $.session.SupplContracts.TalkContacts.supplierCodeName,
            "serviceCode": $.session.SupplContracts.TalkContacts.serviceCode,
            "taklContacts": $.session.SupplContracts.TalkContacts.talkContacts,
            "userText": $.session.MakeRequest.text,
            "audioLink": audio_url,
            // "audioLink": $dialer.getCallRecordingPath(),
            "userPhoneNumber": $.session.MakeRequest.userPhoneNumber,
            "complaintType": "WRONG_PHONE_NUMBER"
        }

        try{
            var response =  $http.query(url, {method: "POST",
                timeout: 20000        // таймаут выполнения запроса в мс
                ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token}//dXNlcl9zZXJ2aWNlOk5TV0tvZ0RZX1BIcVZvNWM="
                , body: request_params
            });
        }
        catch(e){
            //$reactions.answer("Что-то сервер барахлит. ");
            log('--------------- произошла ошибка AddRequestComplaint' );
            SendErrorMessage("onHttpRequest", 'Функция: AddRequestComplaint')
            return false;
        };

        if(response.isOk){
            // заявка зафиксирована
            return true;
        }
        else{
            // произошла ошибка сервиса - надо залогировать
            SendErrorMessage("onHttpResponseError AddRequestComplaint", toPrettyString(response.error))
            return false;
        }
    }
}