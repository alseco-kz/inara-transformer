theme: /SecondPayment
    
    state: SecondPayment
        intent!: /SecondPayment
        random: 
            a: Правильно ли я понимаю,  вы ошибочно оплатили счет дважды?
            a: Правильно ли я понимаю,  вы ошибочно оплатили счет два раза?
            a: Вы ошибочно оплатили счет дважды, правильно?
        go!: /SecondPayment/TransferPoint 
            
    state: ReturnPayment
        
        intent!: /ReturnPayment
        random: 
            a: Правильно ли я понимаю,  вы хотите вернуть платеж?
            a: Вы хотите вернуть платеж, правильно?
        go!: /SecondPayment/TransferPoint 
        
    
    state: TransferPoint           
        
        state: AnotherQuestion
            q: $no 
            q: $disagree 
            intent: /Несогласие
            intent: /AnotherQuestion
            random:
                a: Наверное, я неправильно Вас поняла.
                a: Возможно я не верно вас поняла.
                #     a: Чем бы я могла Вам помочь?
            go!: /WhatDoYouWant   
            
            
        state: Agreement
            intent: /Согласие
            intent: /ReturnPayment
            intent: /SecondPayment
            script:
                $session.nesoglasie = 0; 
            random: 
                a: Вы провели оплату сегодня, правильно?
                a: Вы оплатили сегодня, правильно?
            
            state: Согласие_сегодня
                intent: /Согласие
                intent: /Согласие_сегодня
                #зачем здесь нужны звезды?
                a:  Обратитесь **сегодня** в службу поддержки Вашего банка.                     
                    Банки могут вернуть деньги, Но **только день в день**.
                a:  Вы **сможете** это сделать?
                    
                state: Согласие_Обратиться
                    intent: /Согласие
                    a:  Отлично!
                    go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
                state: Несогласие_Обратиться
                    intent: /Несогласие
                    intent: /Не_знаю   
                    script:
                        $temp.hour = moment($jsapi.currentTime()).hour();
                        $temp.minute = moment($jsapi.currentTime()).minute();
  
                    if: $temp.hour<9  ||( $temp.hour==9 && $temp.minute <30)
                        a:  **Сегодня,  до шестнадцати ноль ноль** нужно принести  чек и копию удостоверения личности по адресу Карасай Батыра, 155 в офис Алсеко !
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
                a:  Ваши деньги **уже поступили поставщикам** и  будут учтены как переплата  в следующей квитанции.
                    АЛСЕКО не принимает деньги за услуги. Эти деньги поступают от банка напрямую поставщикам услуг.
                    Возврат могут сделать только поставщики, контакты которых указаны в квитанции.
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
        state: Требование_дальнейшей_консультации
            random:
                a: Вам нужна дальнейшая консультация?
                a: Необходима ли вам дальнейшая консультация по данному вопросу?
                
            
            state: Требование_дальнейшей_консультации_Yes
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                random:
                    a: Задайте Ваш Вопрос
                    a: Я вас слушаю
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
                    a: У к+аждого поставщика  **свои правила**. Лучше уточн+ите у них
                    a: У к+аждого поставщика  **свои правила**. Актуальную информацию можно узнать только у них
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
                a: Это не важно. Главное принести нужные документы.
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
                a: Вам нужно обратиться с заявлением к каждому поставщику услуг. Контакты поставщиков Вы можете найти в квитанции, либо на нашем сайте алсеко точка кей зет.
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
            #В какой банк ?Куда идти?
                a: Идите в банк через который оплатили квитанцию
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
                
        state: ComeToAlseco
            intent: /ComeToAlseco
            intent: /Лично
            if: $session.nesoglasie == 1
                a:  К **н+ам н+ет**. Возвраты осуществляют **только поставщики** услуг
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации
            else:
                a: **Сегодня,  до шестнадцати ноль ноль** нужно принести  чек и копию удостоверения личности по адресу Карасай Батыра, 155 в офис Алсеко !
                        # go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю - ПРОВЕРИТЬ, туда ли направляет
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            
                    
                # state: CanIHelpYouAgree
                #     q: $yes
                #     q: $agree
                #     intent: /Согласие
                #     intent: /Согласие_помочь
                #     go!: /WhatDoYouWant
        state: NotToday
            intent: /NotToday
            a: К тому времени Ваши деньги уже поступят поставщикам и будут учтены как переплата  в следующей квитанции АЛСЕКО не принимает деньги за услуги. Эти деньги поступают от банка напрямую поставщикам услуг.Возврат могут сделать только поставщики, контакты которых указаны в квитанции
    
            go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
                

        
        state: ReturnDate
            intent: /ReturnDate
            if: $session.nesoglasie==1
                a:У к+аждого поставщика  **свои правила**. Лучше уточн+ите у них
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            else:
                a: Срок возврата уточните  у банка, через который производилась оплата. Он зависит только от них
                go!: /SecondPayment/TransferPoint/Требование_дальнейшей_консультации 
            
    state: CanIHelpYou 
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
                
                
        
