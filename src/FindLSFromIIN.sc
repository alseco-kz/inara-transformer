require: /Functions/AccountNumberInput.js
require: /Blocks/AccountIIN/account_iin.sc
require: /Functions/Language.js
require: /Functions/AccountNumberInput.js

theme: /FindLSFromIIN
    
    state: FindLSFromIIN
        intent!: /FindAccountNumber
        intent!: /Juice
        a: Мы можем найти ваш лицевой счет по ИИН владельца недвижимости || ignoreBargeIn = true
        a: если не проводилась оплата по данному лицевому сч+ёту. Пожалуйста, назовите ваш ИИН.
        script:
            $session.repeatHouseSeek = 0;
            $session.repeatRoomSeek = 0;
            $dialer.bargeInResponse({
                bargeIn: "phrase",
                bargeInTrigger: "interim",
                noInterruptTime: 0
            });
            
            $session.fromFindLSFromIIN = true;
        go: /AccountIIN/AccountIIN
        
    state: AccountFound || modal = true
        
        state: HouseNoMatch
            event: noMatch
            a: Я вас не расслышала. Назовите пожалуйста номер дома
            go!: /FindLSFromIIN/AccountFound
                
        state: HouseSeek
            q: * $numbers *
            q: * @zb.number *
            script:
                $session.Account.House = Math.floor($parseTree.value);

                for(var i = 0; i < $session.Account._numbers.length; i++) {
                    $session.Account._number = $session.Account._numbers[i].accountId;
                    
                   
                    $session.Account.Address = FindAccountAddress();
                    
                    $session.Account.HouseEquals = ($session.Account.House == keepOnlyDigits(toPrettyString($session.Account.Address.data.houseName)));
                
                    if ($session.Account.HouseEquals) {
                        break;
                    }
                }
                
                $session.repeatHouseSeek += 1;
            
            if: $session.Account.HouseEquals
                go!: /FindLSFromIIN/AccountFound/HouseSeek/StartRoomSeek
            elseif: $session.repeatHouseSeek <= 1
                a: Номер дома не найден, давайте попробуем снова
                a: Назовите пожалуйста номер дома
                go!: /FindLSFromIIN/AccountFound
            else:
                a: Номер дома не найден, но
                go!: /FindLSFromIIN/LsInstruction
                
            state: RoomNoMatch
                event: noMatch
                a: Я вас не расслышала. Назовите пожалуйста номер дома
                script:
                    $session.repeatHouseSeek = 0;
                go!: /FindLSFromIIN/AccountFound
                
            
            state: StartRoomSeek
                a: Назовите пожалуйста номер квартиры
            
                state: RoomSeek
                    q: * $numbers *
                    q: * @zb.number *
                
                    script:
                        $session.Account.RoomNumber = Math.floor($parseTree.value);

                        for(var i = 0; i < $session.Account._numbers.length; i++) {
                            $session.Account._number = $session.Account._numbers[i].accountId;
                    
                   
                            $session.Account.Address = FindAccountAddress();
                            
                            $session.StreetName = toPrettyString($session.Account.Address.data.streetName);
                            $session.HouseName = toPrettyString($session.Account.Address.data.houseName);
                            $session.FlatName = toPrettyString($session.Account.Address.data.flatName);
                    
                            $session.Account.StreetEquals = ($session.Account.House == keepOnlyDigits($session.StreetName));
                            $session.Account.HouseEquals = ($session.Account.House == keepOnlyDigits($session.HouseName));
                            $session.Account.RoomEquals = ($session.Account.RoomNumber == keepOnlyDigits($session.FlatName));
                
                            if ($session.Account.HouseEquals && $session.Account.RoomEquals) {
                                break;
                            }
                        }   
                
                        $session.repeatRoomSeek += 1;
            
                    if: $session.Account.RoomEquals
                        a: Подскажите, это ваш адрес? {{$session.StreetName}} {{$session.HouseName}} {{$session.FlatName}}
                    elseif: $session.repeatRoomSeek <= 1
                        a: Номер квартиры не найден, давайте попробуем снова
                        go!: /FindLSFromIIN/AccountFound/HouseSeek/StartRoomSeek
                    else:
                        a: Номер квартиры не найден, но
                        go!: /FindLSFromIIN/LsInstruction
                
                    state: AddressAgreement
                        intent: /Согласие
                        a: Номер лицевого счёта по данному адресу {{AccountTalkNumber($session.Account._number)}}
                        if: $session.addressRepetition >= 1
                            go!: /ИнициацияЗавершения/CanIHelpYou
                        else:
                            a: Повторить?
                        
                    
                        state: AddressAgreementRepeat
                            q!:  ( повтор* / что / еще раз* / ещё раз*)
                            intent: /Повторить
                            intent: /Согласие
                            script:
                                $session.addressRepetition += 1;
                            go!: /FindLSFromIIN/AccountFound/HouseSeek/StartRoomSeek/RoomSeek/AddressAgreement
                                
                        state: AddressAgreementNotRepeat
                            intent: /Несогласие
                            go!: /ИнициацияЗавершения/CanIHelpYou        
                        
                    
                    state: AddressNotAgreement
                        intent: /Несогласие
                        go!: /FindLSFromIIN/LsInstruction
            
                        
    
        state: AccountNotFound
            go!: /FindLSFromIIN/LsInstruction
                
            
        
    state: LsInstruction
        a: Вы можете узнать лицевой счет на портале Смарт Жэ Кэ Ха или на ватсап по номеру телефона 8 771 339 50 15, также это можно сделать в офисе Алсеко по адресу Байзакова 221
        a: Нужна ли Вам дополнительная информация по данному вопросу?
        
        state: YesInformation
            q: $yes
            q: $agree
            intent: /Согласие
            a: Вы бы хотели узнать лицевой счет онлайн или в офисе?
            
            state: Online
                intent: /Онлайн
                a: Хотите через портал Смарт жэ кэ ха. Или ватсап?
                
                state: SmartZKH
                    q: жкх
                    intent: /СмартЖКХ
                    a: В браузере необходимо найти сайт Смарт жэ кэ ха. Вам потребуется ключ э цэ пэ для создания личного кабинета, а также завести объект недвижимости по которому вы хотите узнать лицевой сч+ёт.
                    a: Повторить информацию?
                    
                    state: SmartZKH
                        q: жкх
                        intent: /СмартЖКХ
                        go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/SmartZKH
                        
                    state: WhatsUp
                        intent: /WhatsUp
                        go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/WhatsUp
                    
                    state: Repeat
                        intent: /Согласие
                        a: В браузере необходимо найти сайт Смарт жэ кэ ха. Вам потребуется ключ э цэ пэ для создания личного кабинета, а также завести объект недвижимости по которому вы хотите узнать лицевой сч+ёт.
                        go!: /FindLSFromIIN/CanIHelpYou
                        
                    state: DontRepeat
                        intent: /Несогласие
                        go!: /FindLSFromIIN/CanIHelpYou
                        
                    
                state: WhatsUp
                    intent: /WhatsUp
                    a: Вы можете написать нам на ватсап по номеру 8 771 339 50 15, прикрепив все необходимые документы, то есть документ на право владения недвижимости, это справка с ег+ов и договор купли-продажи, также необходимы копия удостоверения личности собственника и ваше удостоверение личности.
                    a: Повторить информацию?
                    
                    state: SmartZKH
                        q: жкх
                        intent: /СмартЖКХ
                        go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/SmartZKH
                        
                    state: WhatsUp
                        intent: /WhatsUp
                        go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/WhatsUp
                    
                    state: Repeat
                        intent: /Согласие
                        a: Вы можете написать нам на ватсап по номеру 8 771 339 50 15, прикрепив все необходимые документы, то есть документ на право владения недвижимости, это справка с ег+ов и договор купли-продажи, также необходимы копия удостоверения личности собственника и ваше удостоверение личности.
                        go!: /FindLSFromIIN/CanIHelpYou
                        
                    state: DontRepeat
                        intent: /Несогласие
                        go!: /FindLSFromIIN/CanIHelpYou
                        
                    
                        
                    
                        
                
            state: InOffice
                intent: /Лично
                a: Вы можете подойти к нам в офис Алсеко по адресу Байзакова 221 со всеми необходимыми документами, то есть с документом на право владения недвижимости, это справка с ег+ов и договор купли-продажи, также необходимы копия удостоверения личности собственника  и ваше удостоверение личности.
                go!: /FindLSFromIIN/CanIHelpYou
                
                
                
            
        state: NoInformation
            q: $no
            q: $disagree
            intent: /Несогласие
            go!: /FindLSFromIIN/CanIHelpYou
        
        
    state: CanIHelpYou
        a: Могу Вам еще чем-нибудь помочь?
        
        state: CanIHelpYouAgree
            intent: /Согласие
            intent: /Согласие_помочь
            go!: /WhatDoYouWant
            
        state: CanIHelpYouDisagree
            intent: /Несогласие
            intent: /Несогласие_помочь
            go!: /bye
            
        state: CanIHelpYouAgreeRent
            intent: /Арендую
            a: Если вы арендуете, то нужен документ на право владения недвижимости от владельца
            go!: /FindLSFromIIN/CanIHelpYou
            
        state: Online
            intent: /Онлайн
            go!: /FindLSFromIIN/LsInstruction/YesInformation/Online
                        
        state: Office
            intent: /Лично
            go!: /FindLSFromIIN/LsInstruction/YesInformation/InOffice
                        
        state: SmartZKH
            q: жкх
            intent: /СмартЖКХ
            go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/SmartZKH
                        
        state: WhatsUp
            intent: /WhatsUp
            go!: /FindLSFromIIN/LsInstruction/YesInformation/Online/WhatsUp
            
        state: Repeat
            intent: /Повторить
            go!: /FindLSFromIIN/LsInstruction