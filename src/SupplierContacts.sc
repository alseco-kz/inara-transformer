require:  Functions/SupplContacts.js



theme: /SupplierContacts
    state: SupplierContacts
        intent!: /КонтактыПоставщика
        script: 
            SupplContactsClear();
            $session.RepeatCnt = $session.RepeatCnt || {};
            $session.RepeatCnt.ServRepeat = 0;
            # log("SupplierContacts = " + toPrettyString($parseTree))
            # log("SupplierContacts entity = " + toPrettyString($entities))
            
            if ($parseTree._ОсновнойПоставщик){
                if ($parseTree._ОсновнойПоставщик[0])
                    SupplContactsSetSuppl($parseTree._ОсновнойПоставщик[0])
                else 
                    SupplContactsSetSuppl($parseTree._ОсновнойПоставщик)
            } else if ($parseTree._Услуга){
            # если есть услуга, то выделяем ее 
                $temp.Service = $parseTree._Услуга;
                # log("1. $temp.Service"+toPrettyString($temp.Service))
                # log("2. $temp.Service"+toPrettyString($temp.Service))
                if (typeof($temp.Service)=="string"){
                    var  Names = $temp.Service;
                    Names = Names.replaceAll( "\"","\'");
                    Names = Names.replaceAll( "\'","\"");
                    $temp.Service = JSON.parse(Names);
                }
                if ($temp.Service[0])
                    $temp.Service = $temp.Service[0];
                # log("3. $temp.Service"+toPrettyString($temp.Service))
                SupplContactsSetServ($temp.Service.SERV_ID)
            } else if($parseTree._УслугаСл){
                $temp.Service = $parseTree._УслугаСл;
                if (typeof($temp.Service)=="string"){
                    var  Names = $temp.Service;
                    Names = Names.replaceAll( "\"","\'");
                    Names = Names.replaceAll( "\'","\"");
                    $temp.Service = JSON.parse(Names);
                }
                if ($temp.Service[0])
                    $temp.Service = $temp.Service[0];
                # log("3. $temp.Service"+toPrettyString($temp.Service))
                SupplContactsSetServ($temp.Service.SERV_ID)
            } else if ($parseTree._алсеко){
                $reactions.transition("/AlsecoCommon/AlsecoPhones");
            }
            

        # a: даем контакты по услуге
        if: SupplContactsIsSuppSet()
            go!: SupplierContactsSayContacts
            # a: Записывайте. {{GetMainSupplNamesContact($MainSuppl,SupplContactsGetSupplCode())}}.
        #  если есть ЛС, то смотрим по нему. если ЛС нет, то надо спрашивать
        # смотрим, был ли лицевой счет выявлен в ходе диалога
        # Есть номер лицевого счета, будем давать информацию по нему по контактам поставщиков
        elseif: FindAccountIsAccountSet()
            go!: SupplierContactsByAccountServ
        else: 
            # здесь идет определение, что ЛС в рамках дилагога еще не запрашивался - передаем управление туда
            a: Чтобы я дала контакты нужных Вам поставщиков, нужен Ваш лицевой счёт
            BlockAccountNumber:
                okState = SupplierContactsByAccountServ
                errorState = SupplierContactsError
                noAccountState = SupplierContactsError
            
        state: SupplierContactsError
            a: без лицевого счета не могу дать вам телефон поставщика
        
        state: SupplierContactsByAccountServ
            # если есть услуга, то ее не запрашиваем - сразу идем на определение кода 
            if: SupplContactsGetServices()
                go!: ../SupplierContactsSayContacts
            a: Назовите услугу
            
            state: SupplierContactsByAccountServGetServ
                q: * @Услуга * 
                q: * @УслугаСл * 
                script:
                    # log("SupplierContactsByAccountServGetServ = " + toPrettyString($parseTree))
                    if ($parseTree._Услуга){
                        $temp.Service = $parseTree._Услуга;
                        if (typeof($temp.Service)=="string"){
                            var  Names = $temp.Service;
                            Names = Names.replaceAll( "\"","\'");
                            Names = Names.replaceAll( "\'","\"");
                            $temp.Service = JSON.parse(Names);
                        }
                        SupplContactsSetServ($temp.Service.SERV_ID)
                    } 
                    else if($parseTree._УслугаСл){
                        $temp.Service = $parseTree._УслугаСл;
                        if (typeof($temp.Service)=="string"){
                            var  Names = $temp.Service;
                            Names = Names.replaceAll( "\"","\'");
                            Names = Names.replaceAll( "\'","\"");
                            $temp.Service = JSON.parse(Names);
                        }
                        if ($temp.Service[0])
                            $temp.Service = $temp.Service[0];
                        SupplContactsSetServ($temp.Service.SERV_ID)
                    }
                if: SupplContactsGetServices()
                    go!:../../SupplierContactsSayContacts
                else:
                    a: Я не нашла услугу. Перевожу Вас на оператора
                    go!: /CallTheOperator

            state: SupplierContactsByAccountPhone
                q: телефон
                script:
                    SupplContactsSetServ([18, 202, 211, 289])
                if: SupplContactsGetServices()
                    go!:../../SupplierContactsSayContacts
                else:
                    a: Я не нашла услугу. Перевожу Вас на оператора
                    go!: /CallTheOperator                    

            state: SupplierContactsByAccountServGetServNoMatch
                event: noMatch
                a: Я не нашла услугу. Перевожу Вас на оператора
                go!: /CallTheOperator
                
        
        state: SupplierContactsSayContacts
            script:
                $session.RepeatCnt.ServRepeat += 1;
                $temp.ss = {};
                if (SupplContactsIsSuppSet())
                    $temp.ss.text = GetMainSupplNamesContact($MainSuppl,SupplContactsGetSupplCode())
                else 
                    SupplContactsGetContactsByAccountServ($MainSuppl, $temp.ss, ($session.RepeatCnt.ServRepeat == 1));
                if ($session.RepeatCnt.ServRepeat > 1){
                    $dialer.setTtsConfig({speed: 0.9});
                    $session.speedChanged = true;
                }
            # a: Сообщаем контакы
            # a: Запрос еще в работе {{$temp.ss.text}}. лицевой счет {{AccountTalkNumber($session.Account.Number)}}, услуга [{{toPrettyString(SupplContactsGetServices())}}]
            if: ($temp.ss.text) && ($temp.ss.text.length)
                a: Записывайте. 
                a: {{$temp.ss.text}}.
                if: $session.RepeatCnt.ServRepeat < 3
                    a: Повторить? 
                else:
                    go!:../CanIHelpYou
                    
            else
                a: у меня нет нужного телефона. перевожу звонок на оператора. 
                go!: /CallTheOperator
            intent: /Согласие || toState = "."
            intent: /Согласие_продиктовать_список_поставщиков || toState = "."
            intent: /Согласие_повторить || toState = "."
            intent: /Повторить || toState = "."
            intent: /Несогласие || toState = "../CanIHelpYou"
            intent: /Несогласие_повторить || toState = "../CanIHelpYou"
            q: * @duckling.number * || toState = "."
            q: * @Услуга * || toState = ".."
            q: * @УслугаСл * || toState = ".."

        
        state: CanIHelpYou ||noContext = false
            # CommonAnswers
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            # a: Нужна ли моя помощь дальше?
            
            state: Repeat
                intent: /Согласие_продиктовать_список_поставщиков
                intent: /Согласие_повторить
                intent: /Повторить
                go!: ../../SupplierContactsSayContacts

            state: CanIHelpYouAgree
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                go!: /WhatDoYouWant
            
                
            state: CanIHelpYouDisagree
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_помочь
                go!: /bye                    
            
            
            # state: SupplierContactsSayContactsYes
            
    
theme: /NoElectricService
    state: CallerNoElectric
        intent!: /Услуга_НетСвета
        script:
            $session.RepeatCnt = $session.RepeatCnt || {};
            $session.RepeatCnt.ServRepeat = 0;
        random:
            a: У Вас нет электричества, правильно? 
            a: Так, у Вас отключили свет?
            a: Нужен телефон по свету? 
            
        state: CallerNoElectricYes
            intent: /Согласие
            intent: /Согласие_адрес_определен_верно
            intent: /Услуга_НетСвета
            q: $yes *
            q: $agree *
            go!: CallerNoElectricSayAES

            state: CallerNoElectricSayAES
                script: $session.RepeatCnt.ServRepeat += 1
                # a: Позвоните в АлматыЭнергоСбыт по телефону 356, 99, 99. Код города - 727.
                if:  $session.RepeatCnt.ServRepeat == 1
                    a: Позвоните в АлматыЭнергоСбыт по телефону 356, 99, 99. Код города - 727.
                else:
                    a: 356, 99, 99. Код города - 727. || tts = "356 <break strength='strong'/> 99 <break strength='strong'/> 99. Код города - 727."
                if: $session.RepeatCnt.ServRepeat < 3
                    a: Повторить? 
                else:
                    go!:../../CanIHelpYou
            
            # state: CallerNoElectricYesRepeat
                intent: /Согласие || toState = "."
                intent: /Согласие_продиктовать_список_поставщиков || toState = "."
                intent: /Согласие_повторить || toState = "."
                intent: /Повторить || toState = "."
                q: $numbersByWords || toState = "."
            # state: CallerNoElectricYesFinish
                intent: /Несогласие || toState = "../../CanIHelpYou"
                intent: /Несогласие_повторить || toState = "../../CanIHelpYou"
                
                
        
        state: CallerNoElectricNo
            intent: /Несогласие
            intent: /AnotherQuestion
            go!: /WhatDoYouWant

        state: CanIHelpYou ||noContext = false
            # CommonAnswers
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}

            state: Repeat
                intent: /Согласие_продиктовать_список_поставщиков
                intent: /Согласие_повторить
                intent: /Повторить
                q: $numbersByWords 
                go!: ../../CallerNoElectricYes
                
            state: CanIHelpYouAgree
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                go!: /WhatDoYouWant
            
                
            state: CanIHelpYouDisagree
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_помочь
                go!: /bye
                
    state: NoService
        intent!: /Услуга_ПодключитьОтключить
        # a: Услуга_ПодключитьОтключить
        # a: {{toPrettyString($parseTree)}}
        # a: {{toPrettyString($parseTree._Услуга)}}
        script:
            // проверяем наличие услуги ЭЭ в запросе
            // если есть услуга ЭЭ, то отправляем ветка - у вас нет света? 
            // если нет услуг или это другие услуги, то уже говорим - что-то другое
            $temp.HasElectricService = false
            if ($parseTree._Услуга){
                $temp.Service = $parseTree._Услуга;
                if (typeof($temp.Service)=="string"){
                    var  Names = $temp.Service;
                    Names = Names.replaceAll( "\"","\'");
                    Names = Names.replaceAll( "\'","\"");
                    $temp.Service = JSON.parse(Names);
                }
                $temp.HasElectricService = $temp.Service.SERV_ID[0] == 23
            }
        # a: {{$temp.Service}}
        if: $temp.HasElectricService
            go!: /NoElectricService/CallerNoElectric
        else:
            go!: /OtherTheme
            
         