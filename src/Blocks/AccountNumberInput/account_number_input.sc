# **********************************************************
# сценарий по вводу номера лицевого счета
# **********************************************************
# зависимости от других модулей:
# 1.  общие паттерны - да, нет, согласие, отказ
# require: patterns.sc
#     module = sys.zb-common
# 2. Настройки в модуле chatbot.yaml -  Сколько раз уточняем по номеру ЛС
# injector:
#   AccountInputSettings:
#     MaxRetryCount: 2 
# 3. Настроен обработчик сохранения состояния - в главном модуле 
    # bind("preProcess", function($context) {
    #     $context.session._lastState = $context.currentState;
    #     //$context.session._lastState = $context.contextPath ;
    # });
# 4. Подключен файл AccountNumberInput.js  - запросы к Сервису, информация по ЛС
# 5. Подключен файл GetNumbers.js - вычленяет номер ЛС из найденных сущностей
# require: Functions/GetNumbers.js    
# 6. Добавить интент "подождите", "ГдеНомерЛС", "DontKnow","ЛС_ИнойТипВвода" в CAILA
# 7. Добавить в паттерны цифры
# patterns:
#     $numbers = $regexp<(\d+(-|\/)*)+>

# подключение модуля: 
#    require: AccountInput.sc
#    require: Functions/GetNumbers.js

# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# Важно - в телефонном сценарии добавить очистку номера ЛС после окончания сесии (завершили разговор)
# Почему важно - стейт модальный, просто так из него не выйдешь
    # state: HangUp
    #     event!:hangup
    #     event: botHangup
    #     script: FindAccountNumberClear()

# **********************************************************
# Пример использования в стейте:
# ----------------------------------------------------------
#     state: NewAccountMainInfo
#         q: лицев* *
#         if: ($session.Account && $session.Account.Number > 0)
#             a: сейчас дам вам еще информацию по счёту {{$session.Account.Number}}
#             script: 
#                 $reactions.answer(GetAccountNumAnswer($session.Account.Number));
#         elseif: ($session.Account && $session.Account.Number < 0)
#             a: что ж с тобой делать? нет у тебя лицевого счёта ... 
#         else: 
#             a: давайте уточним ваш номер счёта
#             go!:/AccountNumInput/AccountInput
# **********************************************************



require: /Functions/AccountNumberInput.js
require: /Blocks/AccountIIN/account_iin.sc
require: /Functions/Language.js

theme: /BlockAccountNumInput

    state: AccountInput || modal = true
        script: 
            # log(toPrettyString($request.data.args));
            # log('$session.Account = ' + toPrettyString($session.Account));

            //log( toPrettyString($context.session._lastState) );
            if (($context.session._lastState.substr(1,20)) != "BlockAccountNumInput")
            {
                $session.oldState = $context.session._lastState;  
                FindAccountNumberStart();
                $session.AccountOkState = $request.data.args.okState;
                $session.AccountErrorState = $request.data.args.errorState;
                $session.AccountNoAccounState = $request.data.args.noAccountState;
                $analytics.setSessionData("Блок ЛС", "Зашли в блок")
            }
            $session.Account.MaxRetryCount = $injector.AccountInputSettings.MaxRetryCount || 3;
            $session.Account.RetryAccount = $session.Account.RetryAccount || 0;
            $session.Account.RetryAccount++;
            $temp.SayAccount = extractPhrase("account_number_input.sc", "SayAccount", $context)
            if ($session.Account.RetryAccount>1)
                $temp.SayAccount += " " + extractPhrase("account_number_input.sc", "ThroughNumbers", $context)
            $session.AccountNumberContinue = false;
        if: $session.Account.RetryAccount <= $session.Account.MaxRetryCount
            a: {{$temp.SayAccount}}
        else: 
            #  уже запрашивали номер ЛС больше 2-х раз. Зафиксировать результат - не смогла Вас понять и вернуть управление в исходный стейт со всеми данными
            script: FindAccountNumberSetResult("DontUnderstand");
                # $analytics.setSessionData("Блок ЛС", "ЛС не определен")

            #a: Возвращаюсь назад в {{toPrettyString($session.oldState)}}
            go!: {{$session.AccountErrorState}}
        
        state: AccountInputWait    
            intent: /подождите
            a: {{extractPhrase("account_number_input.sc", "AccountInputWait", $context)}}
            script:
               $dialer.setNoInputTimeout(20000); // 20 сек
               
            state: AccountInputWaitConfirm
                intent: /Согласие
            state: AccountInputWaitWait
                intent: /подождите
                go!: ..

        state: speechNotRecognized1
            event: speechNotRecognized
            script:
                $session.speechNotRecognized = $session.speechNotRecognized || {};
                log($session.lastState);
                //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
                if ($session.lastState && !$session.lastState.startsWith("/BlockAccountNumInput/AccountInput/speechNotRecognized")) {
                    $session.speechNotRecognized.repetition = 0;
                } else{
                    $session.speechNotRecognized.repetition = $session.speechNotRecognized.repetition || 0;
                }
                $session.speechNotRecognized.repetition += 1;
                
                if ($session.lastLang == "RU") {
                    $session.accountOne = "Кажется, проблемы со связью.";
                    $session.accountTwo = "Извините, я не расслышала. Повторите, пожалуйста.";
                    $session.accountThree = "Не совсем поняла. Можете повторить, пожалуйста?";
                    $session.accountFour = "Повторите, пожалуйста. Вас не слышно.";
                    $session.accountFive = "Алло? Вы здесь?"; 
                } else {
                    $session.accountOne = "Байланыс мәселесі бар сияқты.";
                    $session.accountTwo = "Кешіріңіз, мен естімедім. Кешіріңіз қайталаңызшы.";
                    $session.accountThree = "Толық түсінбедім. Өтінемін, қайталай аласыз ба?";
                    $session.accountFour = "Кешіріңіз қайталаңызшы. Сізді есту мүмкін емес.";
                    $session.accountFive = "Сәлеметсіз бе? Сіз осындасыз ба?"; 
                }
                
                
            if: $session.speechNotRecognized.repetition >= 3
                a: {{$session.accountOne}}
                script:
                    $dialer.hangUp();
            else:
                random: 
                    a: {{$session.accountTwo}}
                    a: {{$session.accountThree}}
                    a: {{$session.accountFour}}
                    a: {{$session.accountFive}}

        state: looser
            q!: * $looser *
            q!: * $obsceneWord *
            q!: * $stupid  * 
            script:
                $analytics.setMessageLabel("Отрицательная")
                # здесь хочется Чем я могу Вам помочь? Иначе провисание диалога
            if: $session.looser_count ==0
                script: $session.looser_count=+1
                go!: /WhatDoYouWant
            else:
                random:
                    a: {{extractPhrase("account_number_input.sc", "looser1", $context)}}
                    a: {{extractPhrase("account_number_input.sc", "looser2", $context)}}
                    a: {{extractPhrase("account_number_input.sc", "looser3", $context)}}
                go!: /CallTheOperator
                
        state: AccountInputNotNumbersWay
            intent: /ЛС_ИнойТипВвода
            a: {{extractPhrase("account_number_input.sc", "AccountInputNotNumbersWay", $context)}}
            state: AccountInputNotNumbersWayYes
                q: $yes
                q: $agree
                intent: /Согласие
                script:  
                    $session.Account.RetryAccount--;
                go!: ../..
            
            state: AccountInputNotNumbersWayDecline 
                q: $no
                q: $disagree
                intent: /Несогласие
                script: 
                    FindAccountNumberSetResult("DontKnow"); 
                    $analytics.setSessionData("Блок ЛС", "Не знаю ЛС")
                go!: {{$session.AccountNoAccounState}}
            
            
        state: AccountInputWhereIsAccount
            intent: /ГдеНомерЛС
            a: {{extractPhrase("account_number_input.sc", "AccountInputWhereIsAccount", $context)}}
            state: AccountInputWhereIsAccountYes
                q: $yes
                q: $agree
                intent: /Согласие_назвать_номер
                intent: /Согласие
                
                script:  
                    $session.Account.RetryAccount--;
                # a: Ваш лицевой счет {{$session.AccountNumber}}. {{ $session.oldState }}
                go!: ../..
            
            state: AccountInputWhereIsAccountDecline 
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_назвать_номер
                event: noMatch
                script: 
                    FindAccountNumberSetResult("DontKnow"); 
                    $analytics.setSessionData("Блок ЛС", "Не знаю ЛС")
                go!: {{$session.AccountNoAccounState}}
        
        state: AccountInputNumber || modal = true
            
            # проверяем наличие цифр в запросе. если есть, значит говорит номер лицевого счета
            q: * $numbers *
            q: * @zb.number [@zb.number] [@zb.number] *
            q: * @zb.number [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] *
            script: 
                $temp.AccNum = "";
                # log("блок ЛС цифры")
                # log($session.AccountNumberContinue);
                if ($session.AccountNumberContinue)
                    $temp.AccNum = GetTempAccountNumber();
                # log("ЛС временный = "+ toPrettyString($temp.AccNum))
                #TrySetNumber($temp.AccNum + words_to_number($entities));
                var length = $parseTree["zb.number"].length;
                var result = "";
                for (var i = 0; i < length; i++) {
                    result += Math.floor($parseTree["zb.number"][i].value);
                }
                TrySetNumber($temp.AccNum + result);
                # TrySetNumber(words_to_number($entities));
                # log(new Intl.NumberFormat('ru-RU', { style: 'decimal' }).format(GetTempAccountNumber()));
            if: (GetTempAccountNumber().length) <= 5
                a: {{AccountTalkNumber(GetTempAccountNumber())}}. {{extractPhrase("account_number_input.sc", "AccountInputNumberContinue3", $context)}} || bargeInIf = AccountNumDecline
                # go!: AccountInputNumberContinue
            else
                go!: ./AccountInputNumberNumComplete
                # script:
                #     $reactions.timeout({interval: '1s', targetState: 'FindAccount'});
                #     $dialer.setNoInputTimeout(3000); // Бот ждёт ответ 1 секунду и начинает искать.
            # script:
            #     $dialer.bargeInResponse({
            #         //bargeIn: "phrase", // при перебивании бот договаривает текущую фразу до конца, а затем прерывается.
            #         bargeIn: "forced", // forced — при перебивании бот прерывается сразу, не договаривая текущую фразу до конца.
            #         bargeInTrigger: "interim",
            #         //bargeInTrigger: "final",
            #         // noInterruptTime: 1500
            #         noInterruptTime: 0
            #         });
            state: BargeInIntent || noContext = true
                event: bargeInIntent
                script:
                    var bargeInIntentStatus = $dialer.getBargeInIntentStatus();
                    # log(bargeInIntentStatus.bargeInIf); // => "beforeHangup"
                    var text = bargeInIntentStatus.text;
                    var res = $nlp.matchPatterns(text,["$no", "$disagree"])
        
                    if (res) {
                        $dialer.bargeInInterrupt(true);
                    }
                    var res = $nlp.matchPatterns(text,["$Number"])
        
                    if (res) {
                        $session.AccountNumberContinue = true;
                        $dialer.bargeInInterrupt(true);
                    }
                    
                    
            state: AccountInputNumberContinue
                q: * $numbers *
                q: @zb.number [@zb.number] [@zb.number] *
                q: * @zb.number [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] [@zb.number] *
                script:                
                    $temp.AccNum = "";
                    # log("блок ЛС цифры")
                    # log($session.AccountNumberContinue);
                    # if ($session.AccountNumberContinue)
                    $temp.AccNum = GetTempAccountNumber();
                    var length = $parseTree["zb.number"].length;
                    var result = "";
                    for (var i = 0; i < length; i++) {
                        result += Math.floor($parseTree["zb.number"][i].value);
                    }
                    $temp.CurrentNum = result;
                    # log("ЛС временный = "+ toPrettyString($temp.AccNum))
                    TrySetNumber($temp.AccNum + $temp.CurrentNum);
                    $temp.AccNumLen = GetTempAccountNumber().length;
                if: ($temp.AccNumLen) < 9
                    a:{{AccountTalkNumber($temp.CurrentNum)}}
                    random:
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberContinue3", $context)}}
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberContinue4", $context)}}
                elseif: (($temp.AccNumLen ==  9)||($temp.AccNumLen ==  10))
                    go!: ./AccountInputNumberComplete
                else:
                    random:
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberContinue1", $context)}}
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberContinue2", $context)}}
                    go!: /BlockAccountNumInput/AccountInput
                        
                script:
                    # $reactions.timeout({interval: '1s', targetState: 'FindAccount'});
                    # $dialer.setNoInputTimeout(1000); // Бот ждёт ответ 1 секунду и начинает искать.
                    $dialer.bargeInResponse({
                        //bargeIn: "phrase", // при перебивании бот договаривает текущую фразу до конца, а затем прерывается.
                        bargeIn: "forced", // forced — при перебивании бот прерывается сразу, не договаривая текущую фразу до конца.
                        bargeInTrigger: "interim",
                        //bargeInTrigger: "final",
                        // noInterruptTime: 1500
                        noInterruptTime: 0
                        });

                state: AccountInputNumberContinueNo
                    q: $no
                    q: $disagree
                    q: * ($no/$disagree) * @zb.number *
                    intent: /Несогласие
                    if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberContinueNo", $context)}}
                    go!: /BlockAccountNumInput/AccountInput

                state: AccountInputNumberContinueNoSpeech
                    event: speechNotRecognized
                    script:
                        $session.speechNotRecognized = $session.speechNotRecognized || {};
                        # log($session.lastState);
                        //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
                        if ($session.lastState && !$session.lastState.startsWith("/BlockAccountNumInput/AccountInput/AccountInputNumber/AccountInputNumberContinue/AccountInputNumberContinueNoSpeech")) {
                            $session.speechNotRecognized.repetitionNumCont = 0;
                        } else{
                            $session.speechNotRecognized.repetitionNumCont = $session.speechNotRecognized.repetitionNumCont || 0;
                        }
                        $session.speechNotRecognized.repetitionNumCont += 1;
                        
                        if ($session.lastLang == "RU") {
                            $session.accountOne = "Кажется, проблемы со связью.";
                            $session.accountTwo = "Извините, я не расслышала. Повторите, пожалуйста.";
                            $session.accountThree = "Не совсем поняла. Можете повторить, пожалуйста?";
                        } else {
                            $session.accountOne = "Байланыс мәселесі бар сияқты.";
                            $session.accountTwo = "Кешіріңіз, мен естімедім. Кешіріңіз қайталаңызшы.";
                            $session.accountThree = "Толық түсінбедім. Өтінемін, қайталай аласыз ба?";
                        }
                        
                    if: $session.speechNotRecognized.repetitionNumCont > 3
                        a: {{$session.accountOne}}
                        script:
                            $dialer.hangUp();
                    else:
                        random:
                            a: {{$session.accountTwo}}
                            a: {{$session.accountThree}}
                            
                state: AccountInputNumberComplete
                    q: все 
                    intent: /ЛС_цифры_закончились
                    random:
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberComplete1", $context)}} {{AccountTalkNumber(GetTempAccountNumber())}} || bargeInIf = AccountNumDecline 
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberComplete2", $context)}}  {{AccountTalkNumber(GetTempAccountNumber())}} || bargeInIf = AccountNumDecline 
                    
                    state: AccountInputNumberComplete
                        q: $yes
                        q: $agree
                        intent: /Согласие
                        intent: /Согласие_подожду
                        event: noMatch
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberComplete3", $context)}}
                        # script:
                        #     $reactions.timeout({interval: '1s', targetState: '../../../FindAccount'});
                        #     $dialer.setNoInputTimeout(1000); // Бот ждёт ответ 1 секунду и начинает искать.
                        script:
                                $dialer.bargeInResponse({
                                //bargeIn: "phrase", // при перебивании бот договаривает текущую фразу до конца, а затем прерывается.
                                bargeIn: "forced", // forced — при перебивании бот прерывается сразу, не договаривая текущую фразу до конца.
                                bargeInTrigger: "interim",
                                //bargeInTrigger: "final",
                                // noInterruptTime: 1500
                                noInterruptTime: 0
                                });
                        
                        go!: ../../../FindAccount
                    state: AccountInputNumberCompleteNoSpeech
                        event: speechNotRecognized
                        go!: ../../../FindAccount
                    state: AccountInputNumberDisagree
                        q: $no
                        q: $disagree
                        intent: /Несогласие
                        intent: /Несогласие_подожду
                        a: {{extractPhrase("account_number_input.sc", "AccountInputNumberComplete4", $context)}}
                        if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                            go!:  ../../../../../../BlockAccountNumInput/AccountInput
                        else
                            go!:../../../AccountNotFound

            state: AccountInputNumberNumComplete
                intent: /ЛС_цифры_закончились
                go!: ../AccountInputNumberContinue/AccountInputNumberComplete
                
                
            state: AccountInputNumberYes
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_подожду
                event: noMatch
                go!: ../FindAccount

            state: AccountInputNumberNoRecognize
                event: speechNotRecognized
                if: (GetTempAccountNumber().length) <= 4
                    go!: ../AccountInputNumberContinue/AccountInputNumberContinueNoSpeech
                else:
                    go!: ../FindAccount


            state: AccountInputNumberNo
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_подожду
                script: 
                    FindAccountNumberSetResult("AddressCancel"); 
                    $analytics.setSessionData("Блок ЛС", "Неверный номер")
                if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                    a: {{extractPhrase("account_number_input.sc", "AccountInputNumberNo", $context)}}
                go!: /BlockAccountNumInput/AccountInput

            state: FindAccount
                script: 
                    TrySetNumber(GetTempAccountNumber());
                    
                    try{
                        FindAccountAddress().then(function(res) {
                            log(toPrettyString(res));
                            if (res && res.accountId) {
                                //log(res.data[0].address_full_name);
                                $session.Account.Address = res.fullAddressName;
                                $session.Account.NumberInputStreet = res.streetName;
                                $session.Account.NumberInputHouse = keepOnlyDigits(res.houseName);
                                $session.Account.NumberInputFlat = keepOnlyDigits(res.flatName);
                                
                                if ($session.Account.NumberInputHouse.length > 0) {
                                    $session.Account.NumberInputHouse = extractPhrase("account_number_input.sc", "AccountAddressConfirm5", $context)
                                    + " " + $session.Account.NumberInputHouse;
                                }
                                
                                if ($session.Account.NumberInputFlat.length > 0) {
                                    $session.Account.NumberInputFlat = extractPhrase("account_number_input.sc", "AccountAddressConfirm6", $context)
                                    + " " + $session.Account.NumberInputFlat;
                                }
                                // $session.Account.Address = res.data[0].address_full_name;
                                $reactions.transition('../AccountAddressConfirm')
                                $session.Account.AddressRepeatCount = 0;
                                
                            } else {
                                $session.Account.Address = "";
                                $reactions.transition('../AccountNotFound');
                            }
                        }).catch(function(e) {
                            $reactions.answer("Что-то сервер барахлит. ");
                            $reactions.transition('../AccountNotFound');
                            $analytics.setSessionData("Блок ЛС", "ЛС не найден")
                            SendErrorMessage("onHttpRequest", 'Функция: FindAccountAddress ')// + toPrettyString(e)
    
                        });
                    }            
                    catch(e){
                        //$reactions.answer("Что-то сервер барахлит. ");
                        $reactions.answer("Произошла ошибка");
                        $analytics.setSessionData("Блок ЛС", "ЛС не найден")
                        $reactions.transition('../AccountNotFound');
                        SendErrorMessage("onHttpRequest", 'Функция: FindAccountAddress 2')// + toPrettyString(e)
                        return false;
                    };
                    
                        

            state: AccountAddressConfirm
                script:
                    $session.Account.AddressRepeatCount += 1;
                    # log('$request = ' + toPrettyString($request));
                a: {{extractPhrase("account_number_input.sc", "AccountAddressConfirm1", $context)}} {{$session.Account.NumberInputStreet}} {{$session.Account.NumberInputHouse}} {{$session.Account.NumberInputFlat}}. {{extractPhrase("account_number_input.sc", "AccountAddressConfirm2", $context)}}

                state: AccountAddressConfirmYes
                    q: $yes
                    q: $agree
                    intent: /Согласие
                    intent: /Согласие_адрес_определен_верно
                    script:  
                        FindAccountNumberSetSuccees("Address");
                        $analytics.setSessionData("Блок ЛС", "ЛС найден")
                        
                    # a: Ваш лицевой счет {{$session.AccountNumber}}. {{ $session.oldState }}
                    go!: {{$session.AccountOkState}}
                
                state: AccountAddressDecline 
                    q: $no
                    q: $disagree
                    intent: /Несогласие
                    intent: /Несогласие_адрес_определен_верно
                    script: 
                        FindAccountNumberSetResult("AddressCancel"); 
                        $analytics.setSessionData("Блок ЛС", "Другой адрес")
                    if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                        a: {{extractPhrase("account_number_input.sc", "AccountAddressConfirm3", $context)}}
                    go!: /BlockAccountNumInput/AccountInput
                
                state: AccountAddressNoMatch
                    event: noMatch || noContext = true
                    event: speechNotRecognized || noContext = true
                    if: $session.Account.AddressRepeatCount < 2
                        if: $session.lastLang == "RU"
                            a: Я Вас не расслышала. Повторите еще раз.
                        else: 
                            a: Мен сені естімедім. Тағы бір рет қайталаңыз.
                        go!: ..
                    else:
                        go!:../AccountAddressDecline

            state: AccountNotFound
                a: {{extractPhrase("account_number_input.sc", "AccountNotFound1", $context)}}
                script: 
                    $analytics.setSessionData("Блок ЛС", "ЛС не найден")
                if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                    a: {{extractPhrase("account_number_input.sc", "AccountNotFound2", $context)}}
                    go!: /BlockAccountNumInput/AccountInput
                else:
                    go!: /BlockAccountNumInput/AccountInput/AskAboutIIN
                    
        state: AskAboutIIN
            a: {{extractPhrase("account_number_input.sc", "AskAboutIIN", $context)}}
            
            state: AgreeIIN
                intent: /Согласие
                go!: /AccountIIN/AccountIIN
            
            state: DisagreeIIN
                intent: /Несогласие
                go!: /BlockAccountNumInput/AccountInput
        
            

        state: AccountInputNoNumber
            event: noMatch || noContext = true
            a: {{extractPhrase("account_number_input.sc", "AccountInputNoNumber", $context)}}
            go!: ..

        state: AccountInputToOperator
            q: $switchToOperator
            intent: /CallTheOperator
            a: {{extractPhrase("account_number_input.sc", "AccountInputToOperator", $context)}}
            script:
                $analytics.setSessionData("Блок ЛС", "Оператор")
            go!: /CallTheOperator
            
        state: AccountInputRepeat
            intent: /Повторить
            go!: /repeat
            
            
    
    state: DontKnow
        intent: /DontKnow || fromState = "/BlockAccountNumInput/AccountInput"
        script:
            FindAccountNumberSetResult("DontKnow"); 
            $analytics.setSessionData("Блок ЛС", "Не знаю ЛС")
        
        go!: /AccountIIN/AccountIIN