require: Functions/AccountPay.js

patterns:
    # $TotalPay = {оплачена квитанция}
    $TotalPay = {(*плат*|*плач*) * (платеж/сумм*/квитанц*)}
    $HaveAnyPayWord = (~платеж/~проплата/~сумма/~оплачивать/~оплатить/~оплата)

theme: /PaymentTotal

    # скрипт - вовзращает дату последеней оплаты по лицевому счету
    state: GetDateLastPay
        intent!: /GetDateLastPay

        # запрашивает информацию с БД по дате последней оплаты 
        # если ЛС нет, то отрабатывает тоже: запрашивает ЛС. 
        # если не определили номер ЛС, то переводим на оператора
        AccountPayDateMessage:
            needCleanEmptyAccount = true
            okState = /PaymentTotal/GetDateLastPay/CanIHelpYou
            errorState = SendToOperator
            noAccountState = SendToOperator
            # errorState = /PaymentTotal/GetDateLastPay/CanIHelpYou 
            # noAccountState = /PaymentTotal/GetDateLastPay/CanIHelpYou
        
        state: CanIHelpYou
            # a: Нужна ли моя помощь дальше?
            script:
                $temp.index = $reactions.random(CommonAnswers.CanIHelpYou.length);
            a: {{CommonAnswers.CanIHelpYou[$temp.index]}}
            
            state: CanIHelpYouAgree
                q: $yes
                q: $agree
                intent: /Согласие
                intent: /Согласие_помочь
                # go!: /WhatDoYouWant
                go!: /WhatDoYouWantNoContext
                
            state: CanIHelpYouDisagree
                q: $no
                q: $disagree
                intent: /Несогласие
                intent: /Несогласие_помочь
                go!: /bye  

        state: GetDateLastPay_NeedSum
            intent: /Платеж_УточнитьСумму
            a: Для решения вашего вопроса перевожу Вас на оператора.
            go!: /CallTheOperator            
    
        state: SendToOperator
            a: Для реш+ения в+ашего вопр+оса перевож+у Вас на опер+атора.
            go!: /CallTheOperator

    state: PaymentQuestion
        intent!: /Платеж
        q!: * $TotalPay *
        q!: * $HaveAnyPayWord *
        a: Давайте посмотрим Ваши платежи, а потом я переведу Вас на оператора
        AccountPayDateMessage:
            needCleanEmptyAccount = true
            okState = SendToOperator
            errorState = SendToOperator
            noAccountState = SendToOperator
        
        state: SendToOperator
            a: Для решения вашего вопроса перевожу Вас на оператора.
            go!: /CallTheOperator
        


        