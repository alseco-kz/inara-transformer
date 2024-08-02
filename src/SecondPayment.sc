theme: /SecondPayment
    
    state: SecondPayment || modal = true
        intent!: /SecondPayment
        random: 
            a: {{extractPhrase("SecondPayment.sc", "SecondPayment1")}}
            a: {{extractPhrase("SecondPayment.sc", "SecondPayment2")}}
            a: {{extractPhrase("SecondPayment.sc", "SecondPayment3")}}
        go!: /SecondPayment/TransferPoint 
            
    state: ReturnPayment || modal = true
        
        intent!: /ReturnPayment
        random: 
            a: {{extractPhrase("SecondPayment.sc", "ReturnPayment1")}}
            a: {{extractPhrase("SecondPayment.sc", "ReturnPayment2")}}
        go!: /SecondPayment/TransferPoint 
        
    
    state: TransferPoint || modal = true        
        
        state: AnotherQuestion
            q: $no 
            q: $disagree 
            intent: /Несогласие
            intent: /AnotherQuestion
            random:
                a: {{extractPhrase("SecondPayment.sc", "AnotherQuestion1")}}
                a: {{extractPhrase("SecondPayment.sc", "AnotherQuestion2")}}
                #     a: Чем бы я могла Вам помочь?
            go!: /WhatDoYouWant   
            
            
        state: Agreement
            intent: /Согласие
            intent: /ReturnPayment
            intent: /SecondPayment
            script:
                $session.nesoglasie = 0; 
            random: 
                a: {{extractPhrase("SecondPayment.sc", "Agreement1")}}
                a: {{extractPhrase("SecondPayment.sc", "Agreement2")}}
            
            state: Согласие_сегодня
                intent: /Согласие
                intent: /Согласие_сегодня
                #зачем здесь нужны звезды?
                a:  {{extractPhrase("SecondPayment.sc", "Agreement3")}}
                a:  {{extractPhrase("SecondPayment.sc", "Agreement4")}}
                    
                state: Согласие_Обратиться
                    intent: /Согласие
                    a:  {{extractPhrase("SecondPayment.sc", "Agreement5")}}
                    go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
                state: Несогласие_Обратиться
                    intent: /Несогласие
                    intent: /Не_знаю   
                    script:
                        $temp.hour = moment($jsapi.currentTime()).hour();
                        $temp.minute = moment($jsapi.currentTime()).minute();
  
                    if: $temp.hour<9  ||( $temp.hour==9 && $temp.minute <30)
                        a: {{extractPhrase("SecondPayment.sc", "Agreement6")}}
                        go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                    else:
                        
                        # go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю - ПРОВЕРИТЬ, туда ли направляет
                        go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю
                        
                    
                    state: AlsecoAddressRepeat
                        intent: /AlsecoAdressConfirm
                        intent: /Повторить
                        go!: ..

                
            state: Несогласие_не_знаю
                intent: /Несогласие
                intent: /Не_знаю
                intent: /NotToday
                script:
                    $session.nesoglasie = 1
                a:  {{extractPhrase("SecondPayment.sc", "Agreement7")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
        state: Требование_дальнейшей_консультации
            random:
                a: {{extractPhrase("SecondPayment.sc", "Agreement8")}}
                a: {{extractPhrase("SecondPayment.sc", "Agreement9")}}
                
            
            state: Требование_дальнейшей_консультации_Yes
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                random:
                    a: {{extractPhrase("SecondPayment.sc", "Agreement10")}}
                    a: {{extractPhrase("SecondPayment.sc", "Agreement11")}}
                # go!:..
            state: Требование_дальнейшей_консультации_No
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_помочь
                go!: ../../../CanIHelpYou 
            state: Требование_дальнейшей_консультации_Повтор
                intent: /Повторить
                go!: /repeat
    
        state: Whobringsdocs
            intent: /Whobringsdocs
            intent: /собственник_привозит_документы
            intent: /Онлайн
            intent: /Time_to_get_money
            if: $session.nesoglasie ==1
                random:
                    a: {{extractPhrase("SecondPayment.sc", "Agreement12")}}
                    a: {{extractPhrase("SecondPayment.sc", "Agreement13")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
                a: {{extractPhrase("SecondPayment.sc", "Agreement14")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
       
        # state: Sroki_vozvrata
        #     intent: /Sroki
        #     if: $session.nesoglasie == 1 
        #         a: У каждого поставщика услуг свои правила. Поэтому уточните у них
        #         go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
        #     else:
        #         a: Срок возврата уточните  у банка, через который производилась оплата. Он зависит только от них
        #         go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
       
                       
        state: HowFindContacts
            intent: /HowFindContacts
            intent: /WhereToGo
            intent: /WhichPlace
            if: $session.nesoglasie == 1
                a: {{extractPhrase("SecondPayment.sc", "Agreement15")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
            #В какой банк ?Куда идти?
                a: {{extractPhrase("SecondPayment.sc", "Agreement16")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
        state: ComeToAlseco
            intent: /ComeToAlseco
            intent: /Лично
            if: $session.nesoglasie == 1
                a: {{extractPhrase("SecondPayment.sc", "Agreement17")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
                a: {{extractPhrase("SecondPayment.sc", "Agreement18")}}                        # go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю - ПРОВЕРИТЬ, туда ли направляет
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            
                    
                # state: CanIHelpYouAgree
                #     q: $yes
                #     q: $agree
                #     intent: /Согласие
                #     intent: /Согласие_помочь
                #     go!: /WhatDoYouWant
        state: NotToday
            intent: /NotToday
            a: {{extractPhrase("SecondPayment.sc", "Agreement19")}}    
            go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
                

        
        state: ReturnDate
            intent: /ReturnDate
            if: $session.nesoglasie==1
                a: {{extractPhrase("SecondPayment.sc", "Agreement20")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            else:
                a: {{extractPhrase("SecondPayment.sc", "Agreement21")}}
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            
    state: CanIHelpYou || modal = true
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
        state: CanIHelpYouRepeat
                intent: /Повторить
                go!: /repeat
                
                
        
