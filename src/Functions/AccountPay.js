//-----------------------------------------------------------------
// получает дату последней оплаты по ЛС
function GetAccountPayShortInfo(){
    var $session = $jsapi.context().session;
    var $injector = $jsapi.context().injector;
    var return_str = '';
    // есть сведения по ЛС 
    if ($session.Account && $session.Account.Number>0){
        // смотрим, была ли получена ранее информация по основным поставщикам
        //if (!$session.Account.MainSuppliers)
        {
            // функция для поиска поставщиков
            // var url = $injector.MacrosUrl + "sheetURL=" + $injector.AccountTableURL + "&sheetName="+$injector.AccountSheetPayName
            // url = url + "&filterHead=account_number&filterValue="+$session.Account.Number;
            var addr = $env.get("InaraSeviceAddress", "Адрес сервиса не найден") + 'accounts/';
            var url = addr + $session.Account.Number + '/lastPay';
            var token = $secrets.get("InaraSeviceToken", "Токен не найден")

            try{
                var response =  $http.query(url, {method: "GET",
                    timeout: 20000        // таймаут выполнения запроса в мс
                    ,headers: {"Content-Type": "application/json", "Authorization": "Basic " + token}//dXNlcl9zZXJ2aWNlOk5TV0tvZ0RZX1BIcVZvNWM="
                });
            }
            catch(e){
                //$reactions.answer("Что-то сервер барахлит. ");
                log('--------------- произошла ошибка GetAccountPayShortInfo' );
                SendErrorMessage("onHttpRequest", 'Функция: GetAccountPayShortInfo')

                
                return false;
            };

            if(response.isOk){
                if (response.data /*&& response.data.data[0]*/){
                    $session.Account.PaymentInfo = $session.Account.PaymentInfo || {};

                    $session.Account.PaymentInfo.date_last_pay =  response.data.payDate;
                    $session.Account.PaymentInfo.registration_date =  response.data.registrationDate;

                    // $session.Account.PaymentInfo.date_last_pay =  response.data.data[0].date_last_pay;
                    // $session.Account.PaymentInfo.sum_last_pay =  response.data.data[0].sum_last_pay;
                    // $session.Account.PaymentInfo.registration_date =  response.data.data[0].registration_date;

                }
            }
            else{
                // произошла ошибка сервиса - надо залогировать
                SendErrorMessage("onHttpResponseError", toPrettyString(response.error))
            }
        }
    }

    return (($session.Account) && ($session.Account.PaymentInfo));
}


function GetPaymentAnswer(){
    var MonthNames=['январь','февраль','март','апрель','май','июнь','июль','август','сентябрь','октябрь','ноябрь','декабрь'];
    var $session = $jsapi.context().session;
    var message = "";
    if ($session.Account.PaymentInfo){
        if ($session.Account.PaymentInfo.date_last_pay && $session.Account.PaymentInfo.date_last_pay != ""){
            var dat = $session.Account.PaymentInfo.date_last_pay;
            // log(dat.substring(8, 10));
            // log(dat.substring(5, 7));
            var day_num = (dat.substring(8, 10));
            var month_num = Number(dat.substring(5, 7))-1;
            message = "Последняя оплата - ";
            // message += dat.getDate() + ' ' + $nlp.inflect(MonthNames[dat.getMonth()], "gent");
            message += day_num + ' ' + $nlp.inflect(MonthNames[month_num], "gent");
            // message += " dat = " + dat;
            // message += typeof dat;
            return message; //"Последняя оплата  " + $session.Account.PaymentInfo.date_last_pay;
        }
            
        return "Оплата не поступала более двух месяцев. Просим Вас погасить задолженность";
    }
    return "не удалось получить сведения о платежах"
}