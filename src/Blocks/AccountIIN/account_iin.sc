require: /Functions/AccountIIN.js
require: /Blocks/AccountIIN/account_iin.sc

theme: /AccountIIN

    state: AccountIIN || modal = true
        intent!: /Dont_Know_LS
        intent!: /Insert_IIN
        
        script:
            if (($context.session._lastState.substr(1,10)) != "AccountIIN")
            {
                $session.oldState = $context.session._lastState;  
                FindAccountIINStart();
#                $session.AccountOkState = $request.data.args.okState;
#                $session.AccountErrorState = $request.data.args.errorState;
#                $session.AccountNoAccountState = $request.data.args.noAccountState;
                $analytics.setSessionData("Блок ИИН", "Зашли в блок")
            }
            $session.Account.MaxRetryCount = $injector.AccountInputSettings.MaxRetryCount || 3;
            $session.Account.RetryAccount = $session.Account.RetryAccount || 0;
            $session.Account.RetryAccount++;
            $temp.SayAccount = "Пожалуйста, назовите ваш ИИН"
            if ($session.Account.RetryAccount > 1)
                $temp.SayAccount += " по цифрам"
            $session.AccountNumberContinue = false;
        if: $session.Account.RetryAccount <= $session.Account.MaxRetryCount
            a: {{$temp.SayAccount}}
        else: 
            script: FindAccountIINSetResult("DontUnderstand");
            go!: {{$session.AccountErrorState}}
        
        state: AccountIINWait    
            intent: /подождите
            a: да, жду Вас
            script:
               $dialer.setNoInputTimeout(20000);
               
            state: AccountIINWaitConfirm
                intent: /Согласие
            state: AccountIINWaitWait
                intent: /подождите
                go!: ..

        state: speechNotRecognized1
            event: speechNotRecognized
            script:
                $session.speechNotRecognized = $session.speechNotRecognized || {};
                log($session.lastState);
                
                if ($session.lastState && !$session.lastState.startsWith("/AccountIIN/AccountIIN/speechNotRecognized")) {
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
            if: $session.looser_count ==0
                script: $session.looser_count=+1
                go!: /WhatDoYouWant
            else:
                random:
                    a: Не ругайтесь пожалуйста. Соединяю вас с оператором.
                    a: Спасибо.Мне важно ваше мнение. Перевожу вас на оператора.
                    a: Давайте не будем переживать. Перевожу вас на оператора.
                go!: /CallTheOperator
                
        state: AccountIINNotNumbersWay
            intent: /ЛС_ИнойТипВвода
            a: Сейчас я умею понимать только цифры. Вы можете назвать номер счета сейчас?
            state: AccountIINNotNumbersWayYes
                q: $yes
                q: $agree
                intent: /Согласие
                script:  
                    $session.Account.RetryAccount--;
                go!: ../..
            
            state: AccountIINNotNumbersWayDecline 
                q: $no
                q: $disagree
                intent: /Несогласие
                script: 
                    FindAccountIINSetResult("DontKnow"); 
                    $analytics.setSessionData("Блок ИИН", "Не знаю ИИН")
                go!: {{$session.AccountNoAccounState}}
            
            
        state: AccountIINWhereIsIIN
            intent: /ГдеНомерЛС
            a: Номер отображается в счёте Алсеко сразу **над таблицей**.  Вы можете посмотреть счёт и назвать номер **сейчас**?
            state: AccountIINWhereIsAccountYes
                q: $yes
                q: $agree
                intent: /Согласие_назвать_номер
                intent: /Согласие
                
                script:  
                    $session.Account.RetryAccount--;
                go!: ../..
            
            state: AccountIINWhereIsAccountDecline 
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_назвать_номер
                event: noMatch
                script: 
                    FindAccountIINSetResult("DontKnow"); 
                    $analytics.setSessionData("Блок ИИН", "Не знаю ИИН")
                go!: {{$session.AccountNoAccounState}}
        
        state: AccountIINNumber 
            
            q: * $numbers *
            q: * @duckling.number *
            script: 
                $temp.AccNum = "";

                if ($session.AccountNumberContinue)
                    $temp.AccNum = GetTempAccountIIN();
                TrySetIIN($temp.AccNum + words_to_number($entities));

            if: (GetTempAccountIIN().length) <= 5
                a: {{AccountTalkIIN(GetTempAccountIIN())}}. **д+альше** || bargeInIf = AccountNumDecline
            else
                go!: ./AccountIINNumberNumComplete

            state: BargeInIntent || noContext = true
                event: bargeInIntent
                script:
                    var bargeInIntentStatus = $dialer.getBargeInIntentStatus();
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
                    
                    
            state: AccountIINNumberContinue
                q: * $numbers *
                q: * @duckling.number *
                script:                
                    $temp.AccNum = "";
                    $temp.AccNum = GetTempAccountIIN();
                    $temp.CurrentNum = words_to_number($entities);
                    TrySetIIN($temp.AccNum + $temp.CurrentNum);
                    $temp.AccNumLen = GetTempAccountIIN().length;
                if: ($temp.AccNumLen) < 12
                    a:{{AccountTalkIIN($temp.CurrentNum)}}
                    random:
                        a: **д+альше**
                        # a: Так
                        a: **продолж+айте**
                elseif: ($temp.AccNumLen ==  12)
                    go!: ./AccountIINNumberComplete
                else:
                    random:
                        a: Это слишком длинный номер. В базе такого нет. 
                        a: Вы назвали слишком длинный номер, у нас такого нет.
                    go!: /AccountIIN/AccountIIN
                        
                script:
                    $dialer.bargeInResponse({
                        bargeIn: "forced", // forced — при перебивании бот прерывается сразу, не договаривая текущую фразу до конца.
                        bargeInTrigger: "interim",
                        noInterruptTime: 0
                        });

                state: AccountIINNumberContinueNo
                    q: $no
                    q: $disagree
                    q: * ($no/$disagree) * @duckling.number *
                    intent: /Несогласие
                    if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                        a: Дав+айте начнём снач+ала
                    go!: /AccountIIN/AccountIIN

                state: AccountIINNumberContinueNoSpeech
                    event: speechNotRecognized
                    script:
                        $session.speechNotRecognized = $session.speechNotRecognized || {};
                        //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
                        if ($session.lastState && !$session.lastState.startsWith("/AccountIIN/AccountIINNumber/AccountIINNumberContinue/AccountIINNumberContinueNoSpeech")) {
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
                            
                state: AccountIINNumberComplete
                    q: все 
                    intent: /ЛС_цифры_закончились
                    random:
                        a: Подскажите, это Ваш ИИН? {{AccountTalkIIN(GetTempAccountIIN())}} || bargeInIf = AccountNumDecline 
                        a: Верно ли я записала Ваш ИИН?  {{AccountTalkIIN(GetTempAccountIIN())}} || bargeInIf = AccountNumDecline 
                    
                    state: AccountIINNumberComplete
                        q: $yes
                        q: $agree
                        intent: /Согласие
                        intent: /Согласие_подожду
                        event: noMatch
                        a: Поиск займет пару секунд, Подождите пожалуйста.
                        script:
                                $dialer.bargeInResponse({
                                bargeIn: "forced", // forced — при перебивании бот прерывается сразу, не договаривая текущую фразу до конца.
                                bargeInTrigger: "interim",
                                noInterruptTime: 0
                                });
                        
                        go!: ../../../FindAccount
                    state: AccountIINNumberCompleteNoSpeech
                        event: speechNotRecognized
                        go!: ../../../FindAccount
                    state: AccountIINNumberDisagree
                        q: $no
                        q: $disagree
                        intent: /Несогласие
                        intent: /Несогласие_подожду
                        a: Давайте попробуем снова
                        if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                            go!:  ../../../../../../AccountIIN/AccountIIN
                        else
                            go!:../../../AccountNotFound

            state: AccountIINNumberNumComplete
                intent: /ЛС_цифры_закончились
                go!: ../AccountIINNumberContinue/AccountIINNumberComplete
                
                
            state: AccountIINNumberYes
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_подожду
                event: noMatch
                go!: ../FindAccount

            state: AccountIINNumberNoRecognize
                event: speechNotRecognized
                if: (GetTempAccountIIN().length) <= 4
                    go!: ../AccountIINNumberContinue/AccountIINNumberContinueNoSpeech
                else:
                    go!: ../FindAccount


            state: AccountIINNumberNo
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_подожду
                script: 
                    FindAccountIINSetResult("AddressCancel"); 
                    $analytics.setSessionData("Блок ИИН", "Неверный номер")
                if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                    a: Давайте еще раз проверим
                go!: /BlockAccountNumInput/AccountIIN

            state: FindAccount
                script: 
                    TrySetIIN(GetTempAccountIIN());
                    
                    try{
                        FindIINAddress().then(function(res) {
                            log(toPrettyString(res));
                            if (res && res.accountId) {
                                $session.Account.Address = res.fullAddressName;
                                $reactions.transition('../AccountAddressConfirm')
                                $session.Account.AddressRepeatCount = 0;
                                
                            } else {
                                $session.Account.Address = "";
                                $reactions.transition('../AccountNotFound');
                            }
                        }).catch(function(e) {
                            $reactions.answer("Что-то сервер барахлит. ");
                            $reactions.transition('../AccountNotFound');
                            $analytics.setSessionData("Блок ИИН", "ИИН не найден")
                            SendErrorMessage("onHttpRequest", 'Функция: FindIINAddress ')
    
                        });
                    }            
                    catch(e){
                        $reactions.answer("Произошла ошибка");
                        $analytics.setSessionData("Блок ИИН", "ИИН не найден")
                        $reactions.transition('../AccountNotFound');
                        SendErrorMessage("onHttpRequest", 'Функция: FindIINAddress 2')
                        return false;
                    };
                    
                        

            state: AccountAddressConfirm
                script:
                    $session.Account.AddressRepeatCount += 1;
                a: Ваш адрес {{$session.Account.Address}}. Верно? 

                state: AccountAddressConfirmYes
                    q: $yes
                    q: $agree
                    intent: /Согласие
                    intent: /Согласие_адрес_определен_верно
                    script:  
                        FindAccountIINSetSuccees("Address");
                        $analytics.setSessionData("Блок ИИН", "ИИН найден")
                        
                    go!: {{$session.AccountOkState}}
                
                state: AccountAddressDecline 
                    q: $no
                    q: $disagree
                    intent: /Несогласие
                    intent: /Несогласие_адрес_определен_верно
                    script: 
                        FindAccountIINSetResult("AddressCancel"); 
                        $analytics.setSessionData("Блок ИИН", "Другой адрес")
                    if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                        a: Давайте еще раз проверим
                    go!: /AccountIIN/AccountIIN
                
                state: AccountAddressNoMatch
                    event: noMatch || noContext = true
                    event: speechNotRecognized || noContext = true
                    if: $session.Account.AddressRepeatCount < 2
                        a: Я Вас не расслышала. Повторите еще раз.
                        go!: ..
                    else:
                        go!:../AccountAddressDecline

            state: AccountNotFound
                a: Извините, я не нашла Ваш ИИН. 
                script: 
                    $analytics.setSessionData("Блок ИИН", "ИИН не найден")
                if: $session.Account.RetryAccount < $session.Account.MaxRetryCount
                    a: Давайте еще раз проверим
                go!: /AccountIIN/AccountIIN
        
            

        state: AccountIINNoNumber
            event: noMatch || noContext = true
            a: Это не похоже на номер ИИН.
            go!: ..

        state: AccountIINToOperator
            q: $switchToOperator
            intent: /CallTheOperator
            a: Переключаю на оператора
            script:
                $analytics.setSessionData("Блок ИИН", "Оператор")
            go!: /CallTheOperator
            
            
    
    state: DontKnow
        intent: /DontKnow || fromState = "/AccountIIN/AccountIIN"
        script:
            FindAccountIINSetResult("DontKnow"); 
            $analytics.setSessionData("Блок ИИН", "Не знаю ИИН")
        
        a: Без ИИН ничего не могу поделать, извините
        go!: /ИнициацияЗавершения/CanIHelpYou