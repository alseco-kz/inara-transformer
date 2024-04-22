function SetAnwserToUser($context) {
  var answers = [
    // "Что-то пошло не так.",
    "Произошла ошибка. Пожалуйста, повторите запрос позже.", //,
    //    "Все сломалось. Попробуйте еще раз."
  ];
  var randomAnswer = answers[$reactions.random(answers.length)];
  $reactions.answer(randomAnswer);
}
function SendBotChannelMessage(header, message, addBotInfo) {
  var token = $secrets.get("InaraErrorBotToken", "Токен не найден");
  var chat_id = $env.get("InaraErrorBotChatId", "Чат не найден");
  if (addBotInfo) {
    var $request = $jsapi.context().request;
    var $session = $jsapi.context().session;

    var timestamp = moment($jsapi.currentTime());
    message = "ID клиента: " + $request.channelUserId + "\n" + message;
    message = "Время: " + timestamp.format("DD.MMM HH:mm:ss") + "\n" + message;
    // message = "Место: " + place+"\n" + message;
    message = "Канал: " + $request.channelType + "\n\n" + message;
    message = "Бот: " + $request.botId + "\n" + message; //Бот -
  }

  if ($request.channelType != "chatwidget") {
    var url =
      "https://api.telegram.org/bot" +
      token +
      "/sendMessage?chat_id=" +
      chat_id +
      "&text=" +
      header +
      "\n" +
      message; //+"&parse_mode=Markdown";
    var response = $http.query(url, {
      method: "GET",
      //,timeout: 20000        // таймаут выполнения запроса в мс
    });
  }
}

function SendWarningMessage(message) {
  var $request = $jsapi.context().request;
  var $session = $jsapi.context().session;
  // $request.botId
  var msg = ""; //"Bot Warning\nБот (ID канала): "+$request.botId+"\n";
  msg = "Текст:" + message + "\n";
  SendBotChannelMessage("Bot Warning", msg, true);
}

function SendErrorMessage(place, message) {
  // отправить информацию по ошибке в бота
  // https://api.telegram.org/bot<token>/sendMessage?chat_id=<chat_id>&text=Всем привет!
  // var token = $secrets.get("InaraErrorBotToken", "Токен не найден");
  // var chat_id = $env.get("InaraErrorBotChatId", "Чат не найден");
  var $request = $jsapi.context().request;
  var $session = $jsapi.context().session;
  // $request.botId
  var header = "Bot Error\n";
  var msg = "\n"; //"Bot Error\nБот (ID канала): "+$request.botId+"\n";
  msg += "Место: " + place + "\n";
  msg += "Текст ошибки: " + message + "\n";
  msg += "Текст запроса: " + $request.query;

  SendBotChannelMessage(header, msg, true);

  // var url = "https://api.telegram.org/bot"+token+"/sendMessage?chat_id="+chat_id+"&text="+msg;//+"&parse_mode=Markdown";
  // var response =  $http.query(url, {method: "GET"
  // //,timeout: 20000        // таймаут выполнения запроса в мс
  //         })
}

// bind("onScriptError", function ($context) {
//   SetAnwserToUser($context);
//   $analytics.setSessionData(
//     "Ошибка",
//     "onScriptError: " + $context.exception.message
//   );
//   log("onScriptError: " + $context.exception.message);

//   SendErrorMessage("onScriptError", $context.exception.message);
// });

// bind("onDialogError", function ($context) {
//   SetAnwserToUser($context);
//   $analytics.setSessionData(
//     "Ошибка",
//     "onDialogError: " + $context.exception.message
//   );
//   log("onDialogError: " + $context.exception.message);
//   SendErrorMessage("onDialogError", $context.exception.message);
// });

// bind("onAnyError", function ($context) {
//   SetAnwserToUser($context);
//   $analytics.setSessionData(
//     "Ошибка",
//     "onAnyError: " + $context.exception.message
//   );
//   log("onAnyError: " + $context.exception.message);
//   SendErrorMessage("onAnyError", $context.exception.message);
// });
