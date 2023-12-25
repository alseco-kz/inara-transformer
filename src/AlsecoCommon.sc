theme: /AlsecoCommon
    
    # вопросы, связанные с ПТК (программами алсеко)
    state: AlsecoPTK
        intent!: /AlsecoPTK
        a: Переключаю Вас на оператора. Пожалуйста, подождите
        go!: /CallTheOperator
        
# адрес Алсеко
    state: AlsecoAddress                                                                                                                                                                       
        intent!:/AlsecoGiveAddress
        if: (($parseTree._ивц) && (!$parseTree._алсеко))
            go!: /NoMatch
        else:
            random:
                a: Наш адрес - Карасай батыра 155, угол Виноградова. Возможно, . я или опер+атор сможем **Вам пом+очь** по телеф+ону? 
                a: Карасай батыра, 155, угол Виноградова. Может, мы м+ожем реш+ить **Ваш вопр+ос** по телеф+ону?
            script:
                $dialer.bargeInResponse({
                    bargeIn: "forced",
                    bargeInTrigger: "final",
                    noInterruptTime: 0});
            go!: /WhatDoYouWantNoContext

        state: AlsecoAddressRepeat
            intent: /AlsecoAdressConfirm
            intent: /Повторить
            intent: /DontUnderstand
            go!: ..
            
        state: AlsecoAddressHaveOtherAddress
            intent: /AlsecoAddresOther
            a: у алсеко только один офис.  


# телефоны алсеко
# на этот вопрос не отвечаем
    state: AlsecoPhones
        intent!:/AlsecoGivePhones                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
        go!: /NoMatch


    state: AlsecoMobApp
        intent!: /AlsecoGiveMobApp
        random:
            a: наше приложение Алсеко есть в гугл плей и ап сторе 

    state: AlsecoSite
        intent!:/AlsecoGiveSite
        random:
            a: в интернете вы нас можете найти на сайте алсеко точка кей зет 
 
    state: AlsecoEmail
        intent!:/AlsecoGiveEmail
        random:
            a: наш емейл - -  инфо собачка алсеко точка кей зет.
        if:    countRepeats() < 3
            a: Повторить?
        else: 
            go!:../CanIHelpYou 

        state: AlsecoEmailRepeat
            intent: /Повторить
            intent: /Согласие_продиктовать_список_поставщиков
            intent: /Согласие_повторить
            intent: /Согласие
            intent: /Повторить
            q: $numbersByWords 
            a: инфо собачка алсеко точка кей зет. Диктую слово алсеко по буквам.  - А. - Л. - С, как доллар. - Е. - К, как буква "с" русская. - О. алсеко
            if: countRepeatsInRow() < 3
            # if:    countRepeats() < 3
                a: Повторить?
            else: 
                go!:../../CanIHelpYou   
        
        state: AlsecoEmailFinish
            intent: /Несогласие
            intent: /Несогласие_повторить
            go!:../../CanIHelpYou   
 
    state: AlsecoWorkingDays
        intent!:/AlsecoGiveWorkingDays
        random:
            a: мы работаем в будни с 8 до 16, звонки принимаем до 18 00. Суббота и воскресенье - выходной
        go!:../CanIHelpYou

    state: AlsecoPartnersSaleContacts
        intent!:/AlsecoGivePartnersSaleContacts
        random:
            a: По поводу партнерства с нами вы можете позвонить по телефону 8 727 339 50 01
        # if: countRepeatsInRow(true) < 3
        if:    countRepeats() < 3
            a: Повторить?
        else: 
            go!:../CanIHelpYou            

        state: SaleContactsRepeat:
            intent: /Повторить
            intent: /Согласие_продиктовать_список_поставщиков
            intent: /Согласие_повторить
            intent: /Согласие
            intent: /Повторить
            q: $numbersByWords 
            a: 8 727 339 50 01
            if: countRepeatsInRow() < 3
            # if:    countRepeats() < 3
                a: Повторить?
            else: 
                go!:../../CanIHelpYou  
            # go!: ..        

        state: SaleContactsDecline
            intent: /Несогласие
            intent: /Несогласие_повторить
            go!:../../CanIHelpYou            

    
    state: AlsecoFinance
        intent!:/AlsecoGiveFinance
        # random:
        a: Перевожу **Ваш звон+ок** на опер+атора
        go!: /CallTheOperator

    state: CanIHelpYou ||noContext = false
        # CommonAnswers
        script:
            $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
        a: {{CommonAnswers.CanIHelpYou[$temp.index]}}

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

        # random:
        #     a: Мы находимся по адресу - -  Карасай Батыра, 155
        #     a: Наш +адрес - - Карас+ай Бат+ыра **155**, угол Виногр+адова
