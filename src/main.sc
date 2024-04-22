require: slotfilling/slotFilling.sc
  module = sys.zb-common
  
require: patterns.sc
  module = sys.zb-common
require: common.js
  module = sys.zb-common
require: dateTime/dateTime.sc
  module = sys.zb-common  

require: Functions/GetNumbers.js
require: Functions/AccountsSuppliers.js


#########################################
# логирование произошедших ошибок
require: ErrorBind/ErrorLogger.js
 
#########################################
# ПОДКЛЮЧЕНИЕ ДОПОЛНИТЕЛЬНЫХ СЦЕНАРИЕВ
# сценарий смена собственника
require: ChangeAccountPerson.sc
# сценарий смена количества проживающих
require: ChangeAccountPersonCount.sc
# сценарии по платежам
require: PaymentTotal.sc
# контакты поставщика
require: SupplierContacts.sc
# общие вопросы по алсеко
require: AlsecoCommon.sc
# общие вопросы по Инаре
require: AboutInara.sc
# вопросы по налогам
require: Tax.sc

require: SecondPayment.sc

require: KazLanguage.sc

# Сохранение данных по клиенту 
require: Functions/ClientData.js
#########################################
# Справочник - основные поставщики
require: dicts/MainSuppl.csv
    name = MainSuppl
    var = $MainSuppl

#########################################
# Общие ответы
require: CommonAnswers.yaml
    var = CommonAnswers
    

#########################################
# Функции
require: init.js

require: Functions/CommonFunctions.js
    
patterns:
    # $Yes_for_contacts = (сейчас/*диктуй*/говори*/давай*)
    # $No_for_contacts = (самостоятельно/сам/посмотр* сам/найд* сам)
    # $Offline = (оффлайн/лично/офлайн/*жив*/offline/ofline/*офис*)
    # $Online = (онлайн/*интернет*/online/электрон*)
    $numbers = $regexp<(\d+(-|\/)*)+>
    $numbersByWords = * @duckling.number * 
    $mainSuppl = $entity<MainSuppl> || converter = mainSupplConverter
    $changeOwner = [приобрел* @Недвижимость] *мени* (собственника|хозяина|имя|фамилию)
    

    

theme: /

    state: Start
        q!: $regex</start>
        script:
            $.session.looser_count = 0;
            $context.session.AnswerCnt = 0;
            $.session.repeatsInRow = 0;
            $.session.repeats = {};
            # переключаем Инару на радостный голос "Алены" 
            $dialer.setTtsConfig({emotion: "good"});
        # a: Я Инара, ваш виртуальный помощник. Я могу рассказать, как поменять фамилию или количество человек в квитанции, подсказать дату последней оплаты или подсказать контакты поставщика услуг
        a: Я Инара, ваш виртуальный помощник.
        # Я могу рассказать, как поменять фамилию или количество человек в квитанции, 
        # a: подсказать дату последней оплаты или контакты поставщика услуг
        script:
            $temp.index = $reactions.random(CommonAnswers.WhatDoYouWant.length);
        a: {{CommonAnswers.WhatDoYouWant[$temp.index]}}
        script:
            if ($dialer.getCaller())
                $analytics.setSessionData("Телефон", $dialer.getCaller());
            $dialer.bargeInResponse({
                bargeIn: "phrase",
                bargeInTrigger: "final",
                noInterruptTime: 0});
            FindAccountNumberClear();
        
        state: DialogMakeQuestion
            intent: /НачалоРазговора 
        # // требования к паттернам - только нужные слова, без всяких звездочек и т.п. 
            q: девушка
            q: * мне [надо/нужно] поменять
            q: * [меня] интересует [такой] вопрос
            q: [$oneWord] знаете [$oneWord]
            script:
                $session.DialogMakeQuestion = $session.DialogMakeQuestion || {};
                //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
                if ($session.lastState && !$session.lastState.startsWith("/Start")) {
                    $session.DialogMakeQuestion.repetition = 0;
                } else{
                    $session.DialogMakeQuestion.repetition = $session.DialogMakeQuestion.repetition || 0;
                }
                $session.DialogMakeQuestion.repetition += 1;
            if: $session.DialogMakeQuestion.repetition >= 2
                go!: /WhatDoYouWant
            else:
                script:
                    $temp.index = $reactions.random(CommonAnswers.WhatDoYouWant.length);
                a: {{CommonAnswers.WhatDoYouWant[$temp.index]}}

    state: KnowledgeBase
        intentGroup!: /KnowledgeBase
        script: $faq.pushReplies();

    state: Hello
        intent!: /привет
        script:
            $dialer.setNoInputTimeout(3000);
        go!: /HelloAnswer
        
    state: HelloAnswer
        random:
            a: Здравствуйте, чем я могу вам помочь?
            a: Алло, я Вас слушаю. Чем я могу вам помочь?
        
    
    state: WhatDoYouWant
        script:
            $temp.index = $reactions.random(CommonAnswers.WhatDoYouWant.length);
            $temp.counter = countRepeats();
        if: $temp.counter > 4
            a: Пох+оже, я не мог+у Вас пон+ять. Перевожу звон+ок на опер+атора
            go!: /CallTheOperator
        else:
            a: {{CommonAnswers.WhatDoYouWant[$temp.index]}}

    state: WhatDoYouWantNoContext || noContext = true
        script:
            $temp.index = $reactions.random(CommonAnswers.WhatDoYouWant.length);
        a: {{CommonAnswers.WhatDoYouWant[$temp.index]}}        


    state: OtherTheme
        intent!: /РазноеНаОператора
        intent!:/Квитанция_Дубликат
        intent!:/Квитанция_Доставка
        intent!:/Квитанция_электронка
        intent!:/Квитанция_ошибка
        intent!:/Квитанция_общее
        intent!:/Долги
        intent!:/Договорной
        intent!:/Счетчики_общее
        intent!:/Начисления_общее
        intent!:/Платеж_возврат
        go!: /NoMatch
    
    state: NoMatch || noContext = true
        event!: noMatch
        # a: Я не понял. Вы сказали: {{$request.query}}
        script:
            $session.catchAll = $session.catchAll || {};
        
            //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
            if ($session.lastState && !$session.lastState.startsWith("/CatchAll") ) {
                $session.catchAll.repetition = 0;
            } else{
                $session.catchAll.repetition = $session.catchAll.repetition || 0;
            }
            $session.catchAll.repetition += 1;


        if: $context.session.AnswerCnt == 1
            script:
                $temp.index = $reactions.random(CommonAnswers.NoMatch.answers.length);
            a: {{CommonAnswers.NoMatch.answers[$temp.index]}}
            # random:
            #     a: Извините, я Вас не поняла. Повторите пожалуйста 
            #     a: Я Вас не поняла. Сформулируйте по-другому 
            #     a: Скажите еще раз 
            #     a: Мне плохо слышно, повторите
        else:
            a: Для решения Вашего вопроса перевожу Вас на оператора. Пожалуйста, подождите
            go!: /CallTheOperator
    
    state: SwitchToOperator
        q!: перевод на оператора
        q!: $switchToOperator
        intent!: /CallTheOperator
        if: $context.session.AnswerCnt == 1
            a: Чтобы я переключила Вас на нужного оператора, озвучьте свой вопрос
        else:
            a: Переключаю Вас на оператора. Пожалуйста, подождите
            go!: /CallTheOperator
            
    
        

    # перевод на оператора.
    # В А Ж Н О: слова перед переводом говорит стейт, который вызывает этот переход
    state: CallTheOperator
        # a: Перевожу Вас на оператора
        script:
            # Александр Цепелев:
            # Привет. Кто-то делал перевод звонка на оператора с подставлением номера абонента? Как вы в поле FROM передавали этот номер?
            
            # Anatoly Belov:
            # у нас работает так:
            
            # var switchReply = {type:"switch"};
            # switchReply.phoneNumber = "ТУТВНУТРЕННИЙНОМЕР";
            # var callerIdHeader = "\""+$dialer.getCaller()+"\""+" <sip:"+$dialer.getCaller()+"@ТУТВНУТРIP>";
            # switchReply.headers = { "P-Asserted-Identity":  callerIdHeader};
            # $response.replies = $response.replies || [];
            # $response.replies.push(switchReply);
            
            # если звонок передается внутри АТС, т все ок )            
            var switchReply = {type:"switch"};
            # switchReply.phoneNumber = "5015"; // номер, на который переключаем
            switchReply.phoneNumber = "2222"; // номер, на который переключаем
            //switchReply.phoneNumber = "5020"; // номер, на который переключаем
            # switchReply.phoneNumber = "5000"; // номер, на который переключаем
            
            var callerIdHeader = "\""+ $dialer.getCaller() +"\""+" <sip:"+$dialer.getCaller()+"@10.40.89.112>"; // последнеее - внутренний IP
            # var callerIdHeader = "\""+ $dialer.getCaller() +"\""+" <sip:"+$dialer.getCaller()+"@92.46.54.211>"; // последнеее - внутренний IP 
            //
            switchReply.headers = { "P-Asserted-Identity":  callerIdHeader, testheader: "header"};
            
            // при true, абонент будет возвращен к диалогу с ботом после разговора с оператором, а также, если оператор недоступен.
            switchReply.continueCall = false; 

            // при true, разговор продолжает записываться, в том числе с оператором и при повторном возвращении абонента в диалог с ботом. Запись звонка будет доступна в логах диалогов.
            switchReply.continueRecording = false; 
            
            $response.replies = $response.replies || [];
            $response.replies.push(switchReply);
            

    state: CallTheOperatorTransferEvent
        event: transfer
        script:
            var status = $dialer.getTransferStatus();
            log('transfer_status = ' + toPrettyString(status));
        if: $dialer.getTransferStatus().status === 'FAIL'
            a: К сожалению, на данный момент все операторы заняты. Могу ли я Вам еще чем-то помочь? 
        elseif: !$dialer.getTransferStatus().hangup 
            a: Вы вернулись в бота после оператора. 
        #     a: Спасибо, что связались с нами. Оцените, пожалуйста, качество обслуживания.    
        state: CanIHelpYouAgree
            q: $yes
            q: $agree
            intent: /Согласие
            go!: /WhatDoYouWant
            
        state: CanIHelpYouDisagree
            q: $no
            q: $disagree
            intent: /Несогласие
            go!: /bye                

    state: repeat || noContext = true
        q!:  ( повтор* / что / еще раз* / ещё раз*)
        intent!: /Повторить
        script:
            //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
            if ($session.lastState && !$session.lastState.startsWith("/repeat")) {
                $session.repeatRepetition = 0;
            } else{
                $session.repeatRepetition = $session.repeatRepetition || 0;
            }
            $session.repeatRepetition += 1;
            
        if: $session.repeatRepetition >= 3
            a: Кажется, проблемы со связью.
            random:
                a: Соединяю вас с оператором.
                a: Перевожу вас на оператора.
                a: Сейчас я переведу вас на оператора.
            go!: /CallTheOperator
        else:
            if: $session._last_reply
                a: {{$session._last_reply}}
            else:
                go!: {{$session.contextPath}}
        
        
    # go!: {{ $context.session._lastState }} 

    state: bye
        q!: $bye
        intent!: /Прощание
        if: $context.session.AnswerCnt == 1
            script:
                $temp.index = $reactions.random(CommonAnswers.WhatDoYouWant.length);
            a: {{CommonAnswers.WhatDoYouWant[$temp.index]}}
        else:        
            a: Благодарим за обращение!
            random: 
                a: До свидания!
                a: Надеюсь, я смогла вам помочь. Удачи!
            script:
                $dialer.hangUp();
            
    state: greeting
        intent!: /greeting
        random: 
            a: Пожалуйста || htmlEnabled = false, html = "Пожалуйста"
            a: Это моя работа || htmlEnabled = false, html = "Это моя работа"
            a: Я старалась || htmlEnabled = false, html = "Я старалась"
            
    state: looser
        q!: * $looser *
        q!: * $obsceneWord *
        q!: * $stupid  * 
        script:
            $analytics.setMessageLabel("Отрицательная")
            # здесь хочется Чем я могу Вам помочь? Иначе провисание диалога
        if: $session.looser_count ==0
            script: $session.looser_count=+1
            go!: /WhatDoYouWant
        else:
            random:
                a: Не ругайтесь пожалуйста. Соединяю вас с оператором.
                a: Спасибо.Мне важно ваше мнение. Перевожу вас на оператора.
                a: Давайте не будем переживать. Перевожу вас на оператора.
            go!: /CallTheOperator
            



        

    state: HangUp
        event!: hangup
        event!: botHangup
        script: 
            SaveClientLastData();
            FindAccountNumberClear()

    state: WhereAreYou || noContext = true
        q!: где ты [сейчас]
        a: {{$context.contextPath}}
        #a: {{$context.session._lastState}}            

    state: ClearAccount
        q!: сбрось лицев* 
        script: FindAccountNumberClear();
        #a: Ок

    state: speechNotRecognizedGlobal
        event!: speechNotRecognized
        script:
            $session.speechNotRecognized = $session.speechNotRecognized || {};
            //Начинаем считать попадания в кэчол с нуля, когда предыдущий стейт не кэчол.
            if ($session.lastState && !$session.lastState.startsWith("/speechNotRecognizedGlobal")) {
                $session.speechNotRecognized.repetition = 0;
            } else{
                $session.speechNotRecognized.repetition = $session.speechNotRecognized.repetition || 0;
            }
            $session.speechNotRecognized.repetition += 1;
            
        if: $session.speechNotRecognized.repetition >= 3
            a: Кажется, проблемы со связью.
            script:
                $dialer.hangUp();
        else:
            random: 
                a: Извините, я не расслышала. Повторите, пожалуйста.
                a: Не совсем поняла. Можете повторить, пожалуйста?
                a: Повторите, пожалуйста. Вас не слышно.

    state: sessionDataSoftLimitExceeded
        # // обрабатываем событие о достижении soft лимита
        event!: sessionDataSoftLimitExceeded
        script:
            SendWarningMessage('Достигнут лимит sessionDataSoftLimitExceeded')

            
            
    state: BotTooSlow
        event!: timeLimit
        script:
            SendWarningMessage('Сработал лимит timeLimit - по обработке сообщения ботом')
        a: Не смогла найти ответ. Переключаю вас на оператора 
        go!: /CallTheOperator

theme: /ИнициацияЗавершения
    
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

