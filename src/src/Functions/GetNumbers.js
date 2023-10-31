function compare_pos(a,b){
  if ( a.startPos < b.startPos ){
    return -1;
  }
  if ( a.startPos > b.startPos ){
    return 1;
  }
  return 0;
}
function words_to_number(array){
    var return_string = "";
    
    var arr1 = array;
    arr1.sort(compare_pos);

    arr1.forEach(function(entity, i, entities) {
        var number = "";
        if (entity.pattern == "duckling.number"){
            number = entity.value;
            return_string =  return_string + number + "";
            //log(return_string);
        }
    });
    return return_string;
}

