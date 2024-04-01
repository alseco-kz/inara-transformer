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
            $temp.SayAccount = "Назовите номер вашего лицевого счета"
            if ($session.Account.RetryAccount>1)
                $temp.SayAccount += " по цифрам"
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
            a: да, жду Вас
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
                
            if: $session.speechNotRecognized.repetition >= 3
                a: Кажется, проблемы со связью.
                script:
                    $dialer.hangUp();
            else:
                random: 
                    a: Извините, я не расслышала. Повторите, пожалуйста.
                    a: Не совсем поняла. Можете повторить, пожалуйста?
                    a: Повторите, пожалуйста. Вас не слышно.
                    a: Алло? Вы здесь?

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
                    a: Не ругайтесь пожалуйста. Соединяю вас с оператором.
                    a: Спасибо.Мне важно ваше мнение. Перевожу вас на оператора.
                    a: Давайте не будем переживать. Перевожу вас на оператора.
                go!: /CallTheOperator
                
        state: AccountInputNotNumbersWay
            intent: /ЛС_ИнойТипВвода
            a: Сейчас я умею понимать только цифры. Вы можете назвать номер счета сейчас?
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
            a: Номер отображается в счёте Алсеко сразу **над таблицей**.  Вы можете посмотреть счёт и назвать номер **сейчас**?
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
        
        state: AccountInputNumber 
            
            # проверяем наличие цифр в запросе. если есть, значит говорит номер лицевого счета
            q: * $numbers *
            q: * @duckling.number *
            script: 
                $temp.AccNum = "";
                # log("блок ЛС цифры")
                # log($session.AccountNumberContinue);
                if ($session.AccountNumberContinue)
                    $temp.AccNum = GetTempAccountNumber();
                # log("ЛС временный = "+ toPrettyString($temp.AccNum))
                TrySetNumber($temp.AccNum + words_to_number($entities));
                # TrySetNumber(words_to_number($entities));
                # log(new Intl.NumberFormat('ru-RU', { style: 'decimal' }).format(GetTempAccountNumber()));
            if: (GetTempAccountNumber().length) <= 5
                a: {{AccountTalkNumber(GetTempAccountNumber())}}. **д+альше** || bargeInIf = AccountNumDecline
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
                q: * @duckling.number *
                script:                
                    $temp.AccNum = "";
                    # log("блок ЛС цифры")
                    # log($session.AccountNumberContinue);
                    # if ($session.AccountNumberContinue)
                    $temp.AccNum = GetTempAccountNumber();
                    $temp.CurrentNum = words_to_number($entities);
                    # log("ЛС временный = "+ toPrettyString($temp.AccNum))
                    TrySetNumber($temp.AccNum + $temp.CurrentNum);
                    $temp.AccNumLen = GetTempAccountNumber().length;
                if: ($temp.AccNumLen) < 9
                    a:{{AccountTalkNumber($temp.CurrentNum)}}
                    random:
                        a: **д+альше**
                        # a: Так
                        a: **продолж+айте**
                elseif: (($temp.AccNumLen ==  9)||($temp.AccNumLen ==  10))
                    go!: ./AccountInputNumberComplete
                else:
                    random:
                        a: Это слишком длинный номер. В базе такого нет. 
                        a: Вы назвали слишком длинный номер, у нас такого нет.
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
                    q: * ($no/$disagree) * @duckling.number *
                    intent: /Несогласие
                    if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                        a: Дав+айте начнём снач+ала
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
                    if: $session.speechNotRecognized.repetitionNumCont > 3
                        a: К+ажется, пробл+емы со св+язью. Перезвон+ите поздн+ей
                        script:
                            $dialer.hangUp();
                    else:
                        random:
                            a: алл+о? говор+ите д+альше
                            a: алл+о? продолж+айте
                            
                state: AccountInputNumberComplete
                    q: все 
                    intent: /ЛС_цифры_закончились
                    random:
                        a: Подскажите, это Ваш лицевой счет? {{AccountTalkNumber(GetTempAccountNumber())}} || bargeInIf = AccountNumDecline 
                        a: Верно ли я записала Ваш лицевой счет?  {{AccountTalkNumber(GetTempAccountNumber())}} || bargeInIf = AccountNumDecline 
                    
                    state: AccountInputNumberComplete
                        q: $yes
                        q: $agree
                        intent: /Согласие
                        intent: /Согласие_подожду
                        event: noMatch
                        a: Поиск займет пару секунд, Подождите пожалуйста.
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
                        a: Давайте попробуем снова
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
                    a: Давайте еще раз проверим
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
                a: Ваш адрес {{$session.Account.Address}}. Верно? 

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
                        a: Давайте еще раз проверим
                    go!: /BlockAccountNumInput/AccountInput
                
                state: AccountAddressNoMatch
                    event: noMatch || noContext = true
                    event: speechNotRecognized || noContext = true
                    if: $session.Account.AddressRepeatCount < 2
                        a: Я Вас не расслышала. Повторите еще раз.
                        go!: ..
                    else:
                        go!:../AccountAddressDecline

            state: AccountNotFound
                a: Извините, я не нашла Ваш лицевой счёт. 
                script: 
                    $analytics.setSessionData("Блок ЛС", "ЛС не найден")
                if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                    a: Давайте еще раз проверим
                go!: /BlockAccountNumInput/AccountInput

        state: AccountInputNoNumber
            event: noMatch || noContext = true
            a: Это не похоже на номер лицевого счета.
            go!: ..

        state: AccountInputToOperator
            q: $switchToOperator
            intent: /CallTheOperator
            a: Переключаю на оператора
            script:
                $analytics.setSessionData("Блок ЛС", "Оператор")
            go!: /CallTheOperator
            
            
    
    state: DontKnow
        intent: /DontKnow || fromState = "/BlockAccountNumInput/AccountInput"
        script:
            FindAccountNumberSetResult("DontKnow"); 
            $analytics.setSessionData("Блок ЛС", "Не знаю ЛС")
        
        # a: Возвращаю управление в стейт {{$session.oldState}}
        go!: {{$session.AccountNoAccounState}}