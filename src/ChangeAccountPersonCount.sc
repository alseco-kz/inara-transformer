theme: /ChangeAccountPersonCount
    
    state: ChangeAccountPersonStart
        intent!: /ChangeAccountPersonCount
        script:
            $temp.HasTBOservice = false
                 if ($parseTree._Услуга){
                    $temp.Service = $parseTree._Услуга;
                    if (typeof($temp.Service)=="string"){
                        var  Names = $temp.Service;
                        Names = Names.replaceAll( "\"","\'");
                        Names = Names.replaceAll( "\'","\"");
                        $temp.Service = JSON.parse(Names);
                    }
                    $temp.HasTBOservice = $temp.Service.SERV_ID[0] == 400
                }
        # a: {{toPrettyString($parseTree._Услуга)}}
        if: $temp.HasTBOservice
            go!: ../ChangeAccountPersonTBOoMain
        else:
            go!:../ChangeAccountPersonCountQuestion
            
    
    
    
    state: ChangeAccountPersonCountQuestion
        a: Вы можете сменить количество зарегистрированных проживающих в квитанции по основным услугам или в графе вывоз ТэБэО. Вы хотите сменить количество проживающих по основным услугам?
       
        state: TBO
            intent: /TBO
            q: $no 
            q: $disagree 
            intent: /Несогласие
            go!: ../../ChangeAccountPersonTBOoMain
        state: ChangeAccountPersonCount
            intent:  /ChangeAccountPersonCount
            intent: /ServiceInSmart
            q: $yes
            q: $agree
            intent: /Согласие    
            script:
                $temp.HasTBOservice = false
                     if ($parseTree._Услуга){
                        $temp.Service = $parseTree._Услуга;
                        if (typeof($temp.Service)=="string"){
                            var  Names = $temp.Service;
                            Names = Names.replaceAll( "\"","\'");
                            Names = Names.replaceAll( "\'","\"");
                            $temp.Service = JSON.parse(Names);
                        }
                        $temp.HasTBOservice = $temp.Service.SERV_ID[0] == 400
                    }
        # a: {{toPrettyString($parseTree._Услуга)}}
            if: $temp.HasTBOservice
                go!: ../../ChangeAccountPersonTBOoMain
            else:
                go!: ../../ChangeAccountPersonCount
    
    state: ChangeAccountPersonTBOoMain
        random: 
            a: Вам необходимо обратиться в Тартып в офис или онлайн
            a: Для решения вашего вопроса Вам необходимо обратиться в Тартып в офис или онлайн
        a: Вы бы хотели посетить офис?
        state: Online
            intent: /Онлайн
            intent: /HowCanIDoThis
            q: $no 
            q: $disagree 
            intent: /Несогласие
            intent: /CantDoThis  
            
            a: Вы можете направить заявление с подтверждающими документами на адрес электронной почты инфо собачка тартып точка кей зет.
            go!: ../CanIHelpYouTBO    
        state:Offline
            intent: /Лично
            intent: /НеОнлайн
            q: $yes
            q: $agree
            intent: /Согласие     
            a:  Вам нужен абонентский отдел ТОО Тартып по адресу микрорайон Коктем 2, дом номер 2
            go!: ../CanIHelpYouTBO   
        state: CanIHelpYouTBO
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            
            state: CanIHelpYouRepeat
                intent: /Повторить
                go!: /repeat            
            
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
                intent: /greeting
                go!: /bye  
        
        
        
        
        
        state: DocumentsToChangeAccountPerson
            intent: /NeedSomeDocument 
            a: Необходимо уточнить в "Тартып" по номеру 393, 08, 03 или по адресу электронной почты инфо собачка кей зет 
            go!: ../../../ChangeAccountPersonCount/ChangeAccountPersonTBOoMain/CanIHelpYouTBO
        

    
    
    state: ChangeAccountPersonCountToChange
        q!: * (прописано|поменялось) [количество]* @duckling.number * [~человек] * 
        a: В+ы хот+ите помен+ять кол+ичество челов+ек **в квит+анции**. Верно?
        state: ChangeAccountPersonCountToChangeConfirm
            q: $yes
            q: $agree
            intent: /Согласие
            go!: /ChangeAccountPersonCount/ChangeAccountPersonCount
            
        state: ChangeAccountPersonCountToChangeDecline    
            q: $no 
            q: $disagree 
            intent: /Несогласие
            intent: /AnotherQuestion
            random:
                a: Наверное, я неправильно Вас поняла. Можете задать свой вопрос по другому?
                a: А что Вы хотите узнать?
                a: А чем могу я вам помочь? 
            go:/WhatDoYouWant
        
    state: ChangeAccountPersonCount
        
        a: Изменить количество проживающих можно в офисе или онлайн. Вы хотите подать заявку онлайн?
        
        state: Offline
            #intent: /Offline
            intent: /Лично
            intent: /НеОнлайн
            q: $no 
            q: $disagree 
            intent: /Несогласие
            intent: /CantDoThis
            # event: speechNotRecognized
            a: Вы можете обратиться в абонентский отдел, к одному, из основных поставщиков услуг, указанных в верхней части счёта на оплату. Хотите узнать, к каким поставщикам можно обратиться?
            # script:
            # # встраиваем перебивание в длинный ответ 
            #     $dialer.bargeInResponse({
            #         bargeIn: "forced",
            #         bargeInTrigger: "interim",
            #         noInterruptTime: 2});

            # state: AlsecoAddressConfirm
            #     intent: /AlsecoAdressConfirm
            #     a: Вы можете обратиться к нам по адресу Карасай батыра сто пятьдесят пять, угол Байзакова
            #     go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangePersonCount

            state: No_Suppliers_List
                q: $no
                q: $disagree
                intent: /Несогласие_продиктовать_список_поставщиков
                intent: /Несогласие
                event: noMatch
                go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangePersonCount
                    
            state: Yes_Suppliers_List
                # пользователь сказал, что хочет узнать контакты поставщиков 
                #  уточняем, есть ли ЛС. Если нет, то даем контакты всех
                # если говорит номер ЛС, то даем только тех, что есть в квитанции
                q: $yes
                q: $agree
                intent: /Согласие_продиктовать_список_поставщиков
                intent: /Согласие
                # смотрим, был ли лицевой счет выявлен в ходе диалога
                if: ($session.Account && $session.Account.Number > 0)
                    # Есть номер лицевого счета, будем давать информацию по нему по контактам поставщиков
                    go!: SupplierContactsByAccount
                else: 
                    # здесь идет определение, что ЛС в рамках дилагога еще не запрашивался - передаем управление туда
                    a: Чтобы я дала контакты нужных Вам поставщиков, нужен Ваш лицевой счёт
                    BlockAccountNumber:
                        okState = SupplierContactsByAccount
                        errorState = SupplierContactsFull
                        noAccountState = SupplierContactsFull
                    

                state: SupplierContactsFull
                    a:   Вы можете обратиться  к одному из основных поставщиков коммунальных услуг на выбор -  {{GetMainSupplNames($MainSuppl)}}. Если вы уменьшаете количество проживающих, то еще необходимо обратиться в Тартып.
                    go!: ../Contacts
                                    
                state: SupplierContactsByAccount
                    # где-то здесь надо получить список поставщиков из БД и сформировать строку
                        # где-то здесь надо получить список поставщиков из БД и сформировать строку 
                    if: GetAccountMainSuppls()
                        a:   Вы можете обратиться  к одному из основных поставщиков коммунальных услуг на выбор -  {{GetAccountMainSupplNames($MainSuppl)}}.
                        if: isAccountHasSuppl('tartyp')
                            a: Если вы уменьшаете количество проживающих, то еще необходимо обратиться в Тартып.
                    else:
                        go!: ../SupplierContactsFull
                    # go!: /PersonChange/PersonChange/Offline/Yes_Suppliers_List/Contacts
                    go!: ../Contacts
                        
                state: Contacts
                    a:   Контакты можно узнать в мобильном приложении алсеко или я могу продиктовать Вам телефоны сейчас?
                        
                    state: No_Contacts
                        q: $no
                        q: $disagree
                        intent: /Несогласие_продиктовать_список_поставщиков
                        intent: /Несогласие
                        go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangePersonCount
                                    
                    state: Yes_Contacts
                        q: $yes
                        q: $agree
                        intent: /Согласие_продиктовать_список_поставщиков
                        intent: /Согласие
                        event: noMatch
                        if: GetAccountMainSuppls()
                            a:   Записывайте городские номера. Код города - 727. --- {{GetAccountMainSupplNamesContracts($MainSuppl)}}. Повторить номера?
                        else:
                            a:   Записывайте городские номера. Код города - 727. --- {{GetMainSupplNamesContracts($MainSuppl)}}. Повторить номера?

                        state: No_Repeat
                            q: $no
                            q: $disagree
                            intent: /Несогласие_повторить
                            intent: /Несогласие
                            event: noMaatch
                            go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangePersonCount
                                
                        state: Yes_Repeat
                            q: $yes
                            q: $agree
                            intent: /Согласие_повторить
                            intent: /Согласие
                            go!: ../../Yes_Contacts
                    

        state: Online
            #intent: /Online
            intent: /Онлайн
            intent: /HowCanIDoThis
            q: $yes
            q: $agree
            intent: /Согласие
            a: Это можно сделать на сайте смарт точка алсеко точка кей зет.
            a: Зайдите в личный кабинет через э це пэ собственника жилья. Выберите раздел Мои Заявки. Там создайте новую заявку, укажите Алсеко как поставщика услуг и выберите заявку. Дальше следуйте инструкции
            # script:
            # # встраиваем перебивание в длинный ответ 
            #     $dialer.bargeInResponse({
            #         bargeIn: "forced",
            #         bargeInTrigger: "interim",
            #         noInterruptTime: 2});
            go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/CanIHelpYou
            
        state: DocumentsToChangePersonCount
            a:  Перечислить необходимые документы?
            
            state: No_List_Doc
                q: $no
                q: $disagree
                intent: /Несогласие_перечислить
                intent: /Несогласие
                go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/CanIHelpYou
                
            state: Yes_List_Doc
                q: $yes
                q: $agree
                intent: /Согласие_перечислить
                intent: /Согласие
                a:  Необходимые документы: удостоверение личности собственника и сведения о зарегистрированных лицах с портала е гов
                go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/CanIHelpYou

        state: WhoBringsDocuments
            intent: /собственник_привозит_документы
            a: Документы может принести владелец жилья. Либо заявку может подать доверенное лицо - - тогда еще понадобится доверенность от собственника жилья. - - 
            go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangePersonCount
            
        # state: DocsToBring
        #     intent: /перечисление_документов
        #     go!: /ChangeAccountPersonCount/ChangeAccountPersonCount/DocumentsToChangeAccountPersonCount/Yes_List_Doc
        
        state: CanIHelpYou 
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            
            state: CanIHelpYouRepeat
                intent: /Повторить
                go!: /repeat            
            
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
                intent: /greeting
                go!: /bye  
            
            state: RepeatSite
                intent: /SiteRepeat
                go!:/ChangeAccountPersonCount/ChangeAccountPersonCount/Online