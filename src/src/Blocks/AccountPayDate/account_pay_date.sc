theme: /Blocks
    
    # параметры 
    # - needCleanEmptyAccount - запрашивать ЛС повторно, если он ранее не был распознан
    # okState - переход, если информация была предоставлена
    # errorState - если произошла ошибка
    # noAccountState - если ЛС не определен
    
    state: AccountPayDateMessage
        # a: здесь мы получаем информацию по дате последней оплаты по ЛС 
        # a: ЛС запрашиваем здесь же или в другом месте? 
        script: // если задан параметр очищать номер ЛС, то делаем очистку
            if ($request.data.args)
            {
                $session.AccountPayOkState = $request.data.args.okState;
                $session.AccountPayErrorState = $request.data.args.errorState;
                $session.AccountPayNoAccounState = $request.data.args.noAccountState;
            }
            if ($request.data.args.needCleanEmptyAccount){
                if ($session.Account && $session.Account.Number < 0){
                    FindAccountNumberClear();
                    $request.data.args.needCleanEmptyAccount = false;
                }
            }
            
            // есть номер ЛС 
        if: ($session.Account && $session.Account.Number > 0)
            go!: AccountPayGetDateLastPayInfo
        else:
            # go!:/AccountNumInput/AccountInput
            BlockAccountNumber:
                # needCleanEmptyAccount = false
                okState = AccountPayGetDateLastPayInfo
                errorState = ErrorAccount
                noAccountState = NoAccount

        state: NoAccount
            a: К сожалению, без лицевого счёта я не могу дать дату последней оплаты
            go!: {{$session.AccountPayNoAccounState}}

        state: ErrorAccount
            a: К сожалению, мне не удалось получить информацию по оплате. 
            go!: {{$session.AccountPayErrorState}}
            

        state: AccountPayGetDateLastPayInfo
            # получаем инфо по оплате, выводим ее
            if: GetAccountPayShortInfo()
                a: {{GetPaymentAnswer()}} 
                if: $session.AccountPayOkState
                    go!: {{$session.AccountPayOkState}}        
            else: 
                a: К сожалению, мне не удалось получить информацию по оплате. 
                go!: {{$session.AccountPayErrorState}}        


