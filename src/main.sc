require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /

    state: Start
        q!: $regex</start>
        a: Введите фразу на казахском, а я дам ее леммы.

    # state: Hello
    #     intent!: /hello
    #     a: Сәлеметсіз бе!

    # state: Bye
    #     intent!: /bye
    #     a: Дейін!

    state: NoMatch
        # event!: noMatch
        q: *
        a: Сен дедің: {{$request.query}}
        script:
            
            $parseTree.words.forEach(function(val){
                # log(val);
                var markup_val =  $caila.markup(val)
                if (markup_val.words && markup_val.words[0].annotations)
                    {
                    $reactions.answer(val + ", его лемма: "+ markup_val.words[0].annotations.lemma)
                        }
                # log()
                });

    # state: Match
    #     event!: match
    #     a: {{$context.intent.answer}}