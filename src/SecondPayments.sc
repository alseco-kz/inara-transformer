theme: /SecondPayment
    
    state: SecondPayment
        
        intent!: /SecondPayment
        random: 
            a: Правильно ли я понимаю,  вы ошибочно оплатили счет дважды?
            a: Правильно ли я понимаю,  вы ошибочно оплатили счет два раза?
            a: Вы ошибочно оплатили счет дважды, правильно??
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
                a: Наверное, я неправильно Вас поняла. Можете задать свой вопрос по другому?
                a: А что Вы хотите узнать?
                a: А чем могу я вам помочь? 
            go!: /WhatDoYouWant   
            
            
        state: Agreement
            intent: /Согласие
            random: 
                a: Вы провели оплату сегодня, правильно?
                a: Вы оплатили сегодня, правильно?
            
            state: Согласие_сегодня
                intent: /Согласие
                intent: /Согласие_сегодня
                a:  Обратитесь **сегодня ** в службу поддержки Вашего банка.
                    Банки могут вернуть деньги, Но **только день в день**...
                    Вы **сможете** это сделать?
                    
                state: Согласие_Обратиться
                    intent: /Согласие
                    a:  Отлично!
                    go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                
                state: Несогласие_Обратиться
                    intent: /Несогласие
                    intent: /Не_знаю                    
                    a:  **Сегодня,  до шестнадцати ноль ноль** нужно принести  чек и копию удостоверения к нам в офис по адресу Карасай Батыра, 155
                    go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                    
                    state: AlsecoAddressRepeat
                        intent: /AlsecoAdressConfirm
                        intent: /Повторить
                        go!: ..

                
            state: Несогласие_не_знаю
                intent: /Несогласие
                intent: /Не_знаю
                a:  Ваши деньги  **уже поступили поставщикам**.  они будут учтены как переплата в следующей квитанции.
                    Возврат же могут сделать **только  поставщики**
                go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                
                    
                state: Да_локальное
                    intent: /Согласие
                    intent: /Согласие_помочь
                    a: Скажите Ваш вопрос?
                    
                state: Whobringsdocs
                    intent: /Whobringsdocs
                    intent: /собственник_привозит_документы
                    intent: /Онлайн
                    intent: /Time_to_get_money
                    random:
                        a: У к+аждого поставщика  **свои правила**. Лучше уточн+ите у них
                        a: У к+аждого поставщика  **свои правила**. Актуальную информацию можно узнать только у них
                    go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                    
                state: HowFindContacts
                    intent: /HowFindContacts
                    intent: /WhereToGo
                    a: Вам нужно обратиться с заявлением **к к+аждому** поставщику услуг.
                        Контакты поставщиков Вы можете найти в квитанции
                    go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                
                    
                state: ComeToAlseco
                    intent: /ComeToAlseco
                    a:  К **н+ам н+ет**. Возвраты осуществляют **только поставщики** услуг
                    go!: /SecondPayment/TransferPoint/Agreement/Несогласие_не_знаю/CanIHelpYou
                    
                    
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
                        
                
                
        