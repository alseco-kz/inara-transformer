theme: /ChangeAccountPerson
    
    state: ChangeAccountPerson
        intent!: /ChangeAccountPerson
        # a: {{toPrettyString($parseTree)}}
        script:
            $temp.IsOnline = ($parseTree._Сущность_Онлайн)? true: false;
                // $temp.IsOnline = true;
        if: $temp.IsOnline
            go!: Online 
        
        a: Сменить реквизиты можно в офисе или онлайн. Вы хотите подать заявку онлайн?

        state: AnotherQuestion
            intent: /AnotherQuestion
            go!: /WhatDoYouWant
            
            
        state: Offline
            intent: /Лично
            intent: /НеОнлайн
            q: $no 
            q: $disagree 
            q: [я] не (умею/могу/знаю) *
            intent: /CantDoThis
            intent: /Несогласие
            # event: speechNotRecognized
            a: Вы можете обратиться в абонентский отдел, к одному, из основных поставщиков услуг, указанных в верхней части счёта на оплату.

            # script:
            # # встраиваем перебивание в длинный ответ 
            #     $dialer.bargeInResponse({
            #         bargeIn: "forced",
            #         bargeInTrigger: "interim",
            #         noInterruptTime: 2});
            go!: /ChangeAccountPerson/ChangeAccountPerson/Offline/Suppliers_List_Info
            
            state: Suppliers_List_Info
                a: Хотите узнать, к каким поставщикам можно обратиться?
                # script:
                #     if ($session.Account && $session.Account.Number < 0) FindAccountNumberClear();
                # state: AlsecoAddressConfirm
                #     # q: * сто пятьдесят * 
                #     intent: /AlsecoAdressConfirm
                #     a: Вы можете обратиться к нам по адресу Карасай батыра сто пятьдесят пять, угол Байзакова
                #     go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson
    
                state: No_Suppliers_List
                    q: $no
                    q: $disagree
                    intent: /Несогласие_продиктовать_список_поставщиков
                    intent: /Несогласие
                    event: noMatch
                    go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson
                                
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
                        # a: сейчас дам вам еще информацию по счёту {{$session.Account.Number}}
                        # script: 
                        #      $reactions.answer(GetAccountNumAnswer($session.Account.Number));
                    # else:
                    #     BlockAccountNumber:
                    #         okState = SupplierContactsByAccount
                    #         errorState = SupplierContactsFull
                    #         noAccountState = SupplierContactsFull
                    # elseif: ($session.Account && $session.Account.Number < 0)
                    #     # a: что ж с тобой делать? нет у тебя лицевого счёта ... 
                    #     go!: SupplierContactsFull
                    else: 
                        # здесь идет определение, что ЛС в рамках дилагога еще не запрашивался - передаем управление туда
                        a: Чтобы я дала контакты нужных Вам поставщиков, нужен Ваш лицевой счёт
                        BlockAccountNumber:
                            okState = SupplierContactsByAccount
                            errorState = SupplierContactsFull
                            noAccountState = SupplierContactsFull
                        # go!:/AccountNumInput/AccountInput                    
                    

                    state: SupplierContactsFull
                        a:   Вы можете обратиться  к одному из основных поставщиков коммунальных услуг на выбор - {{GetMainSupplNames($MainSuppl)}}.
                        go!: ../Contacts
                                    
                    state: SupplierContactsByAccount
                        # где-то здесь надо получить список поставщиков из БД и сформировать строку 
                        if: GetAccountMainSuppls()
                            a:   Вы можете обратиться  к одному из основных поставщиков коммунальных услуг на выбор -  {{GetAccountMainSupplNames($MainSuppl)}}.
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
                            go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson
                                    
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
                            # a:   Записывайте городские номера. Код города - 727. --- АлматыЭнергоСбыт - телефон 356, 99, 99. Алматинские тепловые сети, 341, 0, 777.  АлматыСу,  3, 777, 444. Алматыгаз,  244, 55,  33. Тартып - 393, 08, 03. Повторить номера?
                            
                            state: No_Repeat
                                event: noMatch
                                q: $no
                                q: $disagree
                                intent: /Несогласие_повторить
                                intent: /Несогласие 
                                go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson
                               
                            state: Yes_Repeat
                                q: $yes
                                q: $agree
                                intent: /Согласие_повторить
                                intent: /Согласие
                                go!: ../../Yes_Contacts
        state: Online
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

            go!: /ChangeAccountPerson/ChangeAccountPerson/CanIHelpYou
            
                
        state: DocumentsToChangeAccountPerson
            a:  Перечислить необходимые документы?
            
            state: No_List_Doc
                q: $no
                q: $disagree
                intent: /Несогласие_перечислить
                intent: /Несогласие
                # a:  Интент "Инициация завершения диалога"
                go!: /ChangeAccountPerson/ChangeAccountPerson/CanIHelpYou                              
                
            state: Yes_List_Doc
                q: $yes
                q: $agree
                intent: /Согласие_перечислить
                intent: /Согласие
                a:  Необходимые документы: удостоверение личности собственника и правоустанавливающие документы на объект недвижимости. Хотите узнать какие документы на собственность подходят?
            
                state: No_Property_Documents
                    q: $no
                    q: $disagree
                    intent: /Несогласие
                    intent: /Несогласие_перечислить
                    # a:  Интент "Инициация завершения диалога"
                    go!: /ChangeAccountPerson/ChangeAccountPerson/CanIHelpYou
                                    
                state: Yes_Property_Documents
                    q: $yes
                    q: $agree
                    intent: /Cогласие_перечислить
                    intent: /Согласие
                    a:  Подходят --копии договора купли-продажи, -дарения, -справка о наличии недвижимого имущества, или  зарегистрированных правах на недвижимое имущество с портала е гов
                    # a:  Интент "Инициация завершения диалога"
                    go!: /ChangeAccountPerson/ChangeAccountPerson/CanIHelpYou
                    
        state: WhoBringsDocuments
            intent: /собственник_привозит_документы
            a: Документы может принести владелец жилья. Либо заявку может подать доверенное лицо - - тогда еще понадобится доверенность от собственника жилья. - - 
            go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson
            
        # state: DocsToBring
        #     intent: /перечисление_документов
        #     go!: /ChangeAccountPerson/ChangeAccountPerson/DocumentsToChangeAccountPerson/Yes_List_Doc


        state: CanIHelpYou 
            # CommonAnswers
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            # a: Нужна ли моя помощь дальше?
            
            state: CanIHelpYouRepeat
                intent: /Повторить
                go!: /repeat

            state: CanIHelpYouOnline
                intent: /Онлайн
                intent: /SiteRepeat
                go!: ../../Online
            
            state: CanIHelpYouOffline
                intent: /Лично
                intent: /НеОнлайн
                go!: ../../Offline
    
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
                
    state: ChangeAccountPerson_Question
        intent!: /ChangeAccountPerson_MayBe
        random:
            a: Вы хот+ите измен+ить фам+илию в квитанции? 
            
        state: ChangeAccountPerson_QuestionYes
            q: $yes
            q: $agree
            intent: /Согласие
            intent: /Согласие_адрес_определен_верно

            go!:/ChangeAccountPerson/ChangeAccountPerson

        state: ChangeAccountPerson_QuestionNo
            intent: /Несогласие
            intent: /AnotherQuestion
            event: noMatch
            go!: /WhatDoYouWant
                                