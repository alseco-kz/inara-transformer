require: slotfilling/slotFilling.sc
  module = sys.zb-common
  
require: patterns.sc
  module = sys.zb-common
  
require: ChangeAccountPerson.sc
require: Functions/GetNumbers.js
require: AccountInput.sc 

require: dicts/MainSuppl.csv
    name = MainSuppl
    var = $MainSuppl

patterns:
    $Yes_for_contacts = (сейчас/*диктуй*/говори*/давай*)
    $No_for_contacts = (самостоятельно/сам/посмотр* сам/найд* сам)
    $Offline = (оффлайн/лично/офлайн/*жив*/offline/ofline)
    $Online = (онлайн/*интернет*/online/электрон*)
    $numbers = $regexp<(\d+(-|\/)*)+>

init:
    bind("preProcess", function($context) {
        $context.session._lastState = $context.currentState;
        //$context.session._lastState = $context.contextPath ;
    });

theme: /

    state: Start
        q!: $regex</start>
        a: Здравствуйте! Я Инара, ваш виртуальный помощник. 
        a: Я могу рассказать, как поменять фамилию или количество человек в квитанции, дать общую информацию по лицевому счёту или подсказать контакты поставщика услуг
        random:
            a: Что вы хотите узнать?
            a: По какому вопросу вы обращаетесь?
            a: Задайте Ваш вопрос
            a: Скажите свой вопрос
        script:
            $dialer.bargeInResponse({
                bargeIn: "forced",
                bargeInTrigger: "interim",
                noInterruptTime: 0});
        # заглушки
        # event: noMatch || onlyThisState = false, toState = "/NoMatch" 
        # intent: /CallTheOperator || onlyThisState = false, toState = "/NoMatch" 
        # intent: /ChangeAccountPerson || onlyThisState = false, toState = "/ChangeAccountPerson/ChangeAccountPerson" 
        # intent: /ChangeAccountPersonCount || onlyThisState = false, toState = "/ChangeAccountPersonCount" 

    state: Hello
        intent!: /привет
        a: Привет привет

    state: NoMatch || noContext = true
        event!: noMatch
        # a: Я не понял. Вы сказали: {{$request.query}}
        a: Не поняла Вас. Перевожу на оператора

    state: CallTheOperator
        a: Хорошо. Перевожу на оператора
        
    state: ChangeAccountPersonCount
        a: Хорошо. Давайте сменим количество проживающих

    state: repeat || noContext = true
        q!:  * ( повтор* / что / еще раз* / ещё раз*) *
        go!: {{$session.contextPath}}
        # go!: {{ $context.session._lastState }} 
        
    state: bye
        q!: $bye
        intent!: /пока
        a: Благодарим за обращение!
        random: 
            a: До свидания! 
            a: Надеюсь, я смогла вам помочь. Удачи! 
        script:
            $dialer.hangUp();
            
    state: greeting
        intent: /greeting
        random: 
            a: Пожалуйста || htmlEnabled = false, html = "Пожалуйста"
            a: Не за что || htmlEnabled = false, html = "Не за что"
            a: Я старалась || htmlEnabled = false, html = "Я старалась"
            
    state: looser
        q!: * $looser *
        q!: * $obsceneWord  *
        q!: * $stupid  * 
        random: 
            a: Спасибо. Мне крайне важно ваше мнение || htmlEnabled = false, html = "Спасибо. Мне крайне важно ваше мнение"
            a: Вы очень любезны сегодня || htmlEnabled = false, html = "Вы очень любезны сегодня"
            a: Это комплимент или оскорбление? || htmlEnabled = false, html = "Это комплимент или оскорбление?"
            # здесь хочется Чем я могу Вам помочь? Иначе провисание диалога

    state: HangUp
        event!: hangup
        event!: botHangup
        script: FindAccountNumberClear()

    state: WhereAreYou || noContext = true
        q!: где ты [сейчас]
        a: {{$context.contextPath}}
        #a: {{$context.session._lastState}}            

    state: ClearAccount
        q!: сбрось лицев* 
        script: FindAccountNumberClear();
        #a: Ок

    state: CatchSpeech
        event!: speechNotRecognized
        random: 
            a: Извините, я не расслышала. Повторите, пожалуйста.
            a: Не совсем поняла. Можете повторить, пожалуйста?
            a: Повторите, пожалуйста. Вас плохо слышно.

theme: /ИнициацияЗавершения
    
    state: CanIHelpYou 
        a: Нужна ли моя помощь дальше?
        
        state: CanIHelpYouAgree
            q: $yes
            q: $agree
            
            
        state: CanIHelpYouDisagree
            q: $no
            q: $disagree
            go!: /bye
