// счетчик попаданий
function countRepeats(key) {
    var state = key || $.currentState;
    // log('countRepeats, state = ' + state)
    $.session.repeats = $.session.repeats || {};
    $.session.repeats[state] = $.session.repeats[state] ? $.session.repeats[state] + 1 : 1;
    // log('countRepeats, repeats = ' + toPrettyString($.session.repeats))
    return $.session.repeats[state];
}

// счетчик попаданий подряд
function countRepeatsInRow() {
    $.temp.entryState = $.currentState;
    if ($.session.lastEntryState === $.currentState) {
        $.session.repeatsInRow += 1;
    } else {
       $.session.repeatsInRow = 1; // число раз подряд
    }
    return $.session.repeatsInRow;
}