require:  Functions/SupplContacts.js
require:  Functions/RequestsComplaint.js
require:  Functions/PhoneNumberInput.js

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
            } else if ($parseTree._КСК){
                SupplContactsSetServ([1])
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
                        $session.serviceName = $parseTree._УслугаСл;
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
                    
                    $session.serviceName = $parseTree.Услуга[0].words[0];
                    
                if: SupplContactsGetServices()
                    
                    go!:../../SupplierContactsSayContacts
                else:
                    a: Я не нашла услугу. Перевожу Вас на оператора
                    go!: /CallTheOperator
                    
                

            state: SupplierContactsByAccountPhone
                q: телефон
                q: * (телефония/телефонная связь) * 
                script:
                    $session.serviceName = "телефония";
                    SupplContactsSetServ([18, 202, 211, 289])
                if: SupplContactsGetServices()
                    go!:../../SupplierContactsSayContacts
                else:
                    a: Я не нашла услугу. Перевожу Вас на оператора
                    go!: /CallTheOperator    
            
            state: SupplierContactsByAccountWater
                q: вода
                a: уточните, какая вода интересует - горячая или холодная? 
                go: SupplierContactsByAccountServ
                
                state: SupplierContactsByAccountHotWater
                    q: * горяч* *
                    script:
                        $session.serviceName = "горячая вода";
                        SupplContactsSetServ([206, 178, 14, 7, 209])
                    if: SupplContactsGetServices()
                        go!:../../../SupplierContactsSayContacts

                state: SupplierContactsByAccountColdWater
                    q: * холод* *
                    script:
                        $session.serviceName = "холодная вода";
                        SupplContactsSetServ([454, 452, 376, 375, 357, 335, 327, 185, 12, 5])
                    if: SupplContactsGetServices()
                        go!:../../../SupplierContactsSayContacts
            
            state: SupplierContactsByAccountKSK
                q: * (@КСК/как) *
                script:
                    $session.serviceName = "КСК";
                    SupplContactsSetServ([1])
                if: SupplContactsGetServices()
                    go!:../../SupplierContactsSayContacts
                else:
                    a: Я не нашла услугу. Перевожу Вас на оператора
                    go!: /CallTheOperator 
            
            state: SupplContactsNeedElectricSant
                q: * ~электрик *
                q: * сантехник* *                    
                a: Это Вам надо обратиться к Вашему органу управления:  к+а +эс к+а   или ос+и. Сейчас посмотрю, есть ли у меня телефон
                script:
                    $session.serviceName = "электрик и сантехник";
                    $reactions.timeout({interval: '1s', targetState: '../SupplierContactsByAccountKSK'});
                    $dialer.setNoInputTimeout(1000); // Бот ждёт ответ 1 секунду и начинает искать.

                state: SupplContactsNeedElectricSantAnyWord
                    q: *
                    event: speechNotRecognized
                    go!: ../../SupplierContactsByAccountKSK
                    
                    
            state: VDGOContacts
                q: * газовщик* *
                script:
                    $session.serviceName = "газовщик";
                    SupplContactsSetServ([450, 38, 22])
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
            if: !($temp.ss.text)
                if: !(typeof $session.serviceName === 'undefined')
                    script:
                        $session.noSuchService = "По данному эл эс " + AccountTalkNumber($session.Account.Number) + " нет услуги " + toPrettyString($session.serviceName) + ". Хотите, соединю с оператором?";
                else:
                    script:
                        $session.noSuchService = "По данному эл эс " + AccountTalkNumber($session.Account.Number) + " нет такой услуги. Хотите, соединю с оператором?";
                    
                go!: /SupplierContacts/SupplierContacts/ChooseOperator
                
            elseif: ($temp.ss.text.length)
                a: Записывайте. 
                a: {{$temp.ss.text}}. 

                if: $session.RepeatCnt.ServRepeat < 3
                    a: Повторить? 
                else:
                    go!:../CanIHelpYou
            else
                go!: /SupplierContacts/SupplierContacts/ChooseRequest
                
            intent: /Согласие || toState = "."
            intent: /Согласие_продиктовать_список_поставщиков || toState = "."
            intent: /Согласие_повторить || toState = "."
            intent: /Повторить || toState = "."
            intent: /Несогласие || toState = "../CanIHelpYou"
            intent: /Несогласие_повторить || toState = "../CanIHelpYou"
            q: * @duckling.number * || toState = "."
            q: * @Услуга * || toState = ".."
            q: * @УслугаСл * || toState = ".."
            q: * @duckling.number * ($no/$disagree) (отвеча*/дозвон*/доступ*) * || toState = "../MakeRequest"
            intent: /PhoneBadNumber || toState = "../MakeRequest"
            
        state: ChooseOperator
            a: {{$session.noSuchService}}
            
            state: OKOperator
                intent: /Согласие
                q: $yes
                a: Перевожу на оператора
                go!: /CallTheOperator
                
            state: NoOperator
                intent: /Несогласие
                q: $no
                go!: /ИнициацияЗавершения/CanIHelpYou
                
        
        state: ChooseRequest
            a: По данной услуге отсутствуют контакты поставщика. Хотите, создам заявку на определение контактов?
            
            state: CreateRequest
                intent: /Согласие
                q: $yes
                script:
                    $.session.SupplContracts.TalkContacts = {};
                    $.session.SupplContracts.TalkContacts.supplierCodeName = '';
                    $.session.SupplContracts.TalkContacts.serviceCode = '';
                    $.session.SupplContracts.TalkContacts.talkContacts = '';
                go!: /SupplierContacts/SupplierContacts/MakeRequest
                    
            state: DontCreateRequest
                intent: /Несогласие
                q: $no
                go: /ИнициацияЗавершения/CanIHelpYou
            
        state: MakeRequest
            # Делаем заявку на то, что номер недоступен 
            if: !FindAccountIsAccountSet() 
                a: Для решения Вашего вопроса перевожу Вас на оператора
                go!: /CallTheOperator 
            script:
                $session.MakeRequest = {};
                $session.MakeRequest.text = $request.query;
                $session.MakeRequest.userPhoneNumber = getUserPhone();
                $temp.phone =  formatPhoneNumber($session.MakeRequest.userPhoneNumber)

            a: Я зафиксирую вашу заявку. Мы ее обработаем и сообщим Вам правильный номер телефона. Давайте проверим ваш контактный номер телефона. {{$temp.phone}}, это ваш номер? 
        #   a:  
            
            
            state: MakeRequestAnotherSuplierPhone
                q: * сейчас можно *
                a: да
                go!: /MakeRequestAnotherPhone
            state: MakeRequestAnotherPhone
                intent: /AnotherPhone
                q: $no
                q: $disagree
                intent: /Несогласие
                
                intent: /Несогласие_перечислить
                script:
                    $session.Phone = {};
                    $session.Phone.NotMyPhoneCounter = 0
                a: Можете назвать номер телефона целиком?
                state: NoPhone
                    q: $no
                    q: $disagree
                    intent: /Несогласие
                    intent: /Несогласие_помочь
                    intent: /Несогласие_перечислить
                    event: noMatch
                    go:../CallTheOperator
                state: PhoneInputNumber
                    q: * $numbers *
                    q: * @duckling.number *
                    script:
                        $temp.PhoneNum = "";
                        if ($session.PhoneNumberContinue)
                            $temp.PhoneNum  = GetTempPhoneNumber();
                        TrySetNumberforPhone($temp.PhoneNum + words_to_number($entities));    
                    # if: ((($temp.PhoneNum.length) <= 6 && ($temp.PhoneNum[0] != '7' || $temp.PhoneNum[0] != '8') )) ||  ((($temp.PhoneNum.length) <= 9 && ($temp.PhoneNum[0] === '7' ) ))  || ((($temp.PhoneNum.length) <= 10 && ($temp.PhoneNum[0] === '8' ||  tel[0] == '+7') ))   
                    #     a: {{PhoneTalkNumber(GetTempPhoneNumber())}}. **д+альше** || bargeInIf = PhoneNumDecline
                    # else
                    a:  Ваш номер телефона {{PhoneTalkNumber(GetTempPhoneNumber())}}.?
                    state: NotMyPhone
                        q: $no
                        q: $disagree
                        intent: /Несогласие
                        intent: /Несогласие_помочь
                        intent: /Несогласие_перечислить
                        event: noMatch
                        script:
                            if ($session.Phone.NotMyPhoneCounter < 2 )
                               $session.Phone.NotMyPhoneCounter = $session.Phone.NotMyPhoneCounter +1
                        if: $session.Phone.NotMyPhoneCounter == 2
                            a: Для решения Вашего вопроса перевожу Вас на оператора
                            go!: /CallTheOperator
                        else:
                            a: Давайте попробуем снова.Назовите номер телефона целиком.
                            
                    state: YesItismyPhone
                        intent: /YesItsMyPhone
                        q: $yes
                        q: $agree
                        intent: /Согласие
                        intent: /Согласие_помочь
                        script:
                            setUserPhone(GetTempPhoneNumber())
                       
                        go!: ../../../MakeRequestSave
            state: MakeRequestPhoneCorrect
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                
                go!: ../MakeRequestSave
                    
            state: MakeRequestSave
                script:
                    $temp.IsRequestAdded = AddRequestComplaint()
                
                if: $temp.IsRequestAdded
                    a: Я зафиксировала Вашу заявку. Мы с Вами свяжемся в течение трех рабочих дней.
                    go!: ../../CanIHelpYou
                else:
                    a: Мне не удалось сохранить заявку. Для решения Вашего вопроса перевожу Вас на оператора
                    go!: /CallTheOperator 
            
            state: MakeRequestDecline
                intent: /DontNeedRequest
                intent: /Несогласие_помочь
                q: ($no/$disagree) заявк*
                if: countRepeats() == 1 
                    a: Без оформления заявки мы не сможем предоставить корректный номер телефона. Готовы начать?  
                else:
                    a: Для решения Вашего вопроса перевожу Вас на оператора
                    go!: /CallTheOperator 
                    
                state: MakeRequestAccept
                    q: $yes
                    q: $agree
                    intent: /Согласие
                    intent: /Согласие_помочь
                    script:
                        $temp.phone =  formatPhoneNumber($session.MakeRequest.userPhoneNumber)
                    a: {{$temp.phone}}, это ваш номер? 
                    go: ../../.
                
                state:  MakeRequestDecline
                    q: $no
                    q: $disagree
                    intent: /Несогласие
                    intent: /Несогласие_помочь
                    intent: /Несогласие_перечислить
                    event: noMatch
                    a: В таком случае для решения Вашего вопроса перевожу Вас на оператора
                    go!: /CallTheOperator 
                    
                
            state: MakeRequestDeclinePhone
                # q: $no
                # q: $disagree
                # intent: /Несогласие
                # intent: /Несогласие_помочь
                # intent: /Несогласие_перечислить
                # a: Можете назвать свой номер телефона? Говорите весь номер сразу
                a: В таком случае для решения Вашего вопроса перевожу Вас на оператора
                go!: /CallTheOperator 
                

            

        
        state: CanIHelpYou ||noContext = false
            # CommonAnswers
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            # a: Нужна ли моя помощь дальше?
            
            state: PhoneBadNumber
                intent: /PhoneBadNumber 
                q: * @duckling.number * ($no/$disagree) (отвеча*/дозвон*/доступ*) *
                go!: ../../MakeRequest
            state: MyRequsetsHasBeenCreated
                q:* заяв* созда* *
                q: * *переда* заявк* *
                 
                intent: /MyRequsetsHasBeenCreated
                random:
                    a: Да.
                    a: Ваша Заявка со+здана!
                    a: Ваша Заявка передана в обработку
                go!: ..
            state: Repeat
                intent: /Согласие_продиктовать_список_поставщиков
                intent: /Согласие_повторить
                intent: /Повторить
                q: * @duckling.number *
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
                intent: /Несогласие_перечислить
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
        state: NotElectric
            intent: /Услуга_НетСвета
            
            script:
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
                go!:../CallerNoElectricYes
            else:
                go!: /WhatDoYouWant

        state: CallerNoElectricHaveEl
            intent: /Наличие
            a: Похоже, я неправильно Вас поняла
            go!: /WhatDoYouWant
            
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
            
         
theme: /VDGODebt
    
    state: VDGOBill
        intent!: /Долг_по_ВДГО
        a: Правильно я понимаю, что у вас по услуге ВДГО задолженность?
        
        state: YesVDGO
            q: да *
            q: правильно *
            intent: /Согласие
            
            if: FindAccountIsAccountSet()
                go!: VdgoOK 
            else:
                BlockAccountNumber:
                    okState = VdgoOK
                    errorState = VdgoError
                    noAccountState = VdgoError

            state: VdgoOK
                script:
                    var mainList = [38];
                    var additionalList = [22, 450];
                    
                    var $session = $jsapi.context().session;
                    $session.SupplContracts = $session.SupplContracts || {};

                    var supplierContacts = SupplContactsGetServices();
                    supplierContacts = SupplContactsGetServices();
                    
                    $temp.ss = {};
                    
                    mainList.forEach(function(mainNumber) {
                        $session.SupplContracts.servId = [mainNumber];
                        supplierContacts = SupplContactsGetServices();
                    
                        if (SupplContactsIsSuppSet())
                            $temp.ss.text = GetMainSupplNamesContact($MainSuppl,SupplContactsGetSupplCode())
                        else 
                            SupplContactsGetContactsByAccountServ($MainSuppl, $temp.ss, true);
                    
                        if (typeof $temp.ss.text !== 'undefined' && $temp.ss.text !== null) {
                            $temp.mainPresent = true;
                        }
                    });
                    
                    additionalList.forEach(function(additionalNumber) {
                        $session.SupplContracts.servId = [additionalNumber];
                        supplierContacts = SupplContactsGetServices();
                    
                        if (SupplContactsIsSuppSet())
                            $temp.ss.text = GetMainSupplNamesContact($MainSuppl,SupplContactsGetSupplCode())
                        else 
                            SupplContactsGetContactsByAccountServ($MainSuppl, $temp.ss, true);
                    
                        if (typeof $temp.ss.text !== 'undefined' && $temp.ss.text !== null) {
                            $temp.additionalPresent = true;
                        }
                    });
                    
                    $session.textContacts = $temp.ss.text;
                    $session.VDGORepeat = 0;
                    
            
                if: $temp.mainPresent
                    go!: AskSupplier
                elseif: $temp.additionalPresent
                    go!: /VDGODebt/SayContacts
                else:
                    a: Извините, услуга ВДГО по этому номеру ЛС не подключена
                    go!: /ИнициацияЗавершения/CanIHelpYou
                    
                state: AskSupplier
                    a: С января 2024 года по указанию поставщика услуг, начисление выставляется один раз в год за 12 месяцев. Вы можете оплатить всю сумму сразу или равными частями помесячно. В течение года на данную услугу пеня не начисляется. Нужны ли вам контакты поставщиков?

                    state: SupplierNeeded
                        intent: /Согласие
                        go!: /VDGODebt/SayContacts
                    
                    state: SupplierNotNeeded
                        intent: /Несогласие
                        go!: /ИнициацияЗавершения/CanIHelpYou/CanIHelpYouDisagree
                    
            state: VdgoError
                a: Без лицевого счёта и ИИН не могу решить проблему с задолженностью по ВДГО
                go!: /ИнициацияЗавершения/CanIHelpYou
                
        state: NoVDGO
            q: нет *
            q: неправильно *
            intent: /Несогласие
            a: Уточните, пожалуйста, свой запрос
            go!: /ИнициацияЗавершения/CanIHelpYou
        
        state: SpeechUnrecognizedVDGO
            event: speechNotRecognized
            a: Я вас не расслышала.
            go!: /VDGODebt/VDGOBill
            
    state: SayContacts
        script:
            $session.VDGORepeat += 1;
        
        a: Записывайте.
        a: {{$session.textContacts}}.
                
        if: $session.VDGORepeat < 3
            a: Повторить? 
        else:
            go!:/ИнициацияЗавершения/CanIHelpYou
            
        intent: /Согласие || toState = "."
        intent: /Согласие_продиктовать_список_поставщиков || toState = "."
        intent: /Согласие_повторить || toState = "."
        intent: /Повторить || toState = "."
        intent: /Несогласие || toState = "/SupplierContacts/SupplierContacts/CanIHelpYou   "
        intent: /Несогласие_повторить || toState = "/SupplierContacts/SupplierContacts/CanIHelpYou   "
        q: * @duckling.number * || toState = "."
        q: * @Услуга * || toState = ".."
        q: * @УслугаСл * || toState = ".."
        intent: /PhoneBadNumber || toState = "/SupplierContacts/SupplierContacts/MakeRequest"
            
    